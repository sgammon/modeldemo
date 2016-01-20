package io.momentum.demo.models;


import com.google.api.services.bigquery.model.TableRow;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.io.BigQueryIO;
import com.google.cloud.dataflow.sdk.io.PubsubIO;
import com.google.cloud.dataflow.sdk.io.TextIO;
import com.google.cloud.dataflow.sdk.options.*;
import com.google.cloud.dataflow.sdk.transforms.*;
import com.google.cloud.dataflow.sdk.values.KV;
import com.google.cloud.dataflow.sdk.values.PCollection;
import com.googlecode.objectify.Key;

import io.momentum.demo.models.pipeline.PlatformPipeline;
import io.momentum.demo.models.pipeline.coder.ModelCoder;
import io.momentum.demo.models.pipeline.options.*;
import io.momentum.demo.models.schema.AppModel;
import io.momentum.demo.models.schema.UserMessage;


/**
 * Created by sam on 1/12/16.
 */
public final class MessagePipeline extends PlatformPipeline {
  static {
    PipelineOptionsFactory.register(MessagesPipelineOptions.class);
  }

  public interface MessagesPipelineOptions extends StreamingOptions,
                                                   PubsubInputOptions,
                                                   PubsubOutputOptions,
                                                   StorageInputOptions,
                                                   StorageOutputOptions,
                                                   BigQueryInputOptions,
                                                   BigQueryOutputOptions,
                                                   FixedWindowOptions,
                                                   DataflowPipelineOptions,
                                                   PlatformPipelineOptions {
    /**
     * Set to true if we should calculate and output word stats.
     */
    @Default.Boolean(true)
    @Description("Set to true to calculate word stats.")
    boolean isEnableWordstats();
    void setEnableWordstats(boolean value);
  }

  @Override
  protected Class<? extends PipelineOptions> options() {
    return MessagesPipelineOptions.class;
  }

  @Override
  protected Pipeline collapse(Pipeline pipeline) {
    // add model coder stuff
    //pipeline.getCoderRegistry().registerCoder(AppModel.class, ModelCoder.class);
    //pipeline.getCoderRegistry().registerCoder(UserMessage.class, ModelCoder.class);

    // read and inflate user messages
    PCollection<UserMessage> messages;
    MessagesPipelineOptions options = pipeline.getOptions().as(MessagesPipelineOptions.class);

    // do the thing
    if (options.isStreaming() ||
        options.getEnableWindowing()) {
      messages = window(options.as(FixedWindowOptions.class),
                   read(options, pipeline));
    } else {
      messages = read(options, pipeline);
    }

    // write user messages to pubsub and bigquery
    if (!options.getDryRun()) {
      publish(options, messages);
        write(options, messages);
    }

    // calculate wordstats if so instructed
    if (options.isEnableWordstats()) {
      PCollection<String> wordStats = messages.apply("Extract Words", ParDo.of(new ExtractMessageWords()))
                                              .apply("Count Words", Count.<String>perElement())
                                              .apply("Format for Word Report", ParDo.of(new FormatWordForStat()));

      if (options.isStreaming()) {
        // need to output this to pubsub, in windows
        throw new RuntimeException("wordstats not yet supported in streaming mode");
      } else {
        // output stats to file
        if (options.getStorageBucket() == null) throw new RuntimeException("you forgot your storage bucket ya doofus");

        if (options.getStorageOutputFile() != null && !options.getDryRun()) {
          // output via storage file
          wordStats.apply(TextIO.Write.to(options.getStorageOutputFile())
                                      .named("Write to Storage"));

        } else if (options.getStorageOutputPrefix() != null && !options.getDryRun()) {
          // output via storage prefix
          wordStats.apply(TextIO.Write.withShardNameTemplate(options.getStorageOutputPrefix())
                                      .withSuffix("csv")
                                      .named("Write to Storage"));
        }
      }
    }
    return pipeline;
  }

  /** -- functions -- **/
  public static class ExtractMessageWords extends DoFn<UserMessage, String> {
    private Aggregator<Integer, Integer> globalWordCounter;
    private Aggregator<Integer, Integer> globalMessageCounter;

    public ExtractMessageWords() {
      globalWordCounter = createAggregator("globalWordCount", new Sum.SumIntegerFn());
      globalMessageCounter = createAggregator("globalMessageCount", new Sum.SumIntegerFn());
    }

    @Override
    public void processElement(ProcessContext c) throws Exception {
      final UserMessage payload = c.element();

      globalMessageCounter.addValue(1);
      logging.info("Processing message from user '" + payload.name + "': \"" + payload.message + "\".");

      // process message words
      if (payload.message.length() > 1) {
        for (String word : payload.message.split("[^a-zA-Z']+")) {
          if (!word.isEmpty()) {
            globalWordCounter.addValue(1);
            c.output(word.toLowerCase());
          }
        }
      }
    }
  }

  public static class FormatWordForStat extends DoFn<KV<String, Long>, String> {
    @Override
    public void processElement(ProcessContext c) throws Exception {
      KV<String, Long> el = c.element();
      c.output(el.getKey() + ": " + el.getValue());
    }
  }

  public static class FormatMessageForRow extends DoFn<UserMessage, TableRow> {
    @Override
    public void processElement(ProcessContext processContext) throws Exception {
      UserMessage payload = processContext.element();
      TableRow row = payload.export();
      row.set("id", payload.getId());
      row.set("kind", UserMessage.class.getSimpleName());
      processContext.output(row);
    }
  }

  public static class InflateMessageFromRow extends DoFn<TableRow, UserMessage> {
    @Override
    public void processElement(ProcessContext c) throws Exception {
      TableRow row = c.element();
      String name = (String)row.get("name");
      String message = (String)row.get("message");
      String email = (String)row.get("email");
      UserMessage messageObject = new UserMessage(name, message, email);

      // carry over key ID
      String encodedKey = (String)row.get("key");
      if (encodedKey != null) {
        Key inflatedKey = Key.create(encodedKey);
        messageObject.setId(inflatedKey.getId());
      }
      c.output(messageObject);
    }
  }

  /** -- pipeline steps -- **/
  private static PCollection<UserMessage> read(MessagesPipelineOptions options,
                                               Pipeline pipeline) {
    if (options.isStreaming()) {
      return pipeline.apply(PubsubIO.Read.topic(topicForInput(options))
                                         .named("Message Stream")
                                         .withCoder(ModelCoder.of(UserMessage.class)));
    } else {
      if (options.getStorageInputFile() != null) {
        // we're pulling from storage
        return pipeline.apply(TextIO.Read.from(options.getStorageInputFile())
                                         .named("Read from Storage")
                                         .withCoder(ModelCoder.of(UserMessage.class)));

      } else if (options.getBigQueryInputTable() != null) {
        // we're pulling from bigquery
        return pipeline.apply(BigQueryIO.Read.from(tableRefForInput(options))
                                             .named("Read from Table"))
                       .apply(ParDo.of(new InflateMessageFromRow()).named("Inflate Messages"));

      } else {
        throw new RuntimeException("unknown or unsupported source data sink; tried bigquery, storage and pubsub");
      }
    }
  }

  private static void write(MessagesPipelineOptions options,
                            PCollection<UserMessage> pipeline) {
    pipeline.apply(ParDo.of(new FormatMessageForRow()).named("Format for BigQuery"))
            .apply(BigQueryIO.Write.to(tableRefForOutput(options.as(BigQueryOutputOptions.class)))
                                   .withSchema(AppModel.schema(UserMessage.class))
                                   .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                                   .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                                   .named("Write to BigQuery"));
  }

  private static void publish(MessagesPipelineOptions options,
                              PCollection<UserMessage> pipeline) {
    if (options.getPubsubOutputTopic() != null) {
      pipeline.apply(PubsubIO.Write.topic(topicForOutput(options.as(PubsubOutputOptions.class)))
                                   .withCoder(ModelCoder.of(UserMessage.class))
                                   .named("Re-publish Messages"));
    }
  }
}
