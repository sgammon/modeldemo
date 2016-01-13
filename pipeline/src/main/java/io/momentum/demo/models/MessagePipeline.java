package io.momentum.demo.models;


import com.google.api.services.bigquery.model.TableRow;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.coders.AvroCoder;
import com.google.cloud.dataflow.sdk.io.BigQueryIO;
import com.google.cloud.dataflow.sdk.io.PubsubIO;
import com.google.cloud.dataflow.sdk.options.*;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.values.PCollection;
import com.googlecode.objectify.Key;

import io.momentum.demo.models.pipeline.BasePipeline;
import io.momentum.demo.models.pipeline.PlatformPipeline;
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

  public interface MessagesPipelineOptions extends PubsubInputOptions,
                                                   PubsubOutputOptions,
                                                   BigQueryInputOptions,
                                                   BigQueryOutputOptions,
                                                   FixedWindowOptions,
                                                   DataflowPipelineOptions,
                                                   PlatformPipelineOptions {}

  @Override
  protected Class<? extends PipelineOptions> options() {
    return MessagesPipelineOptions.class;
  }

  @Override
  protected Pipeline collapse(Pipeline pipeline) {
    PCollection<String> messagePayloads = window(pipeline.getOptions().as(FixedWindowOptions.class),
                                          read(pipeline.getOptions().as(MessagesPipelineOptions.class), pipeline));

    // inflate messages
    PCollection<UserMessage> messages = messagePayloads.apply(ParDo.of(new InflateMessage()).named("Inflate Messages"));

    if (!pipeline.getOptions().as(MessagesPipelineOptions.class).getDryRun()) {
      write(pipeline.getOptions().as(MessagesPipelineOptions.class), messages);
    }
    return null;
  }

  /** -- functions -- **/
  public static class InflateMessage extends DoFn<String, UserMessage> {
    @Override
    public void processElement(ProcessContext processContext) throws Exception {
      String payload = processContext.element();
      UserMessage message = BasePipeline.codec.getReader().readValue(payload, UserMessage.class);
      processContext.output(message);
    }
  }

  public static class FormatMessage extends DoFn<UserMessage, TableRow> {
    @Override
    public void processElement(ProcessContext processContext) throws Exception {
      UserMessage payload = processContext.element();
      TableRow row = payload.export();
      Key objectKey = Key.create(payload);
      row.set("key", objectKey.toWebSafeString());
      row.set("kind", objectKey.getKind());
      processContext.output(row);
    }
  }

  /** -- pipeline steps -- **/
  private static PCollection<String> read(MessagesPipelineOptions options,
                                          Pipeline pipeline) {
    if (options.isStreaming()) {
      return pipeline.apply(PubsubIO.Read.topic(topicForInput(options)).named("Message Stream"));
    } else {
      // no-op
      throw new RuntimeException("Non-streaming `MessagesPipeline` is not yet implemented.");
    }
  }

  private static void write(MessagesPipelineOptions options,
                            PCollection<UserMessage> pipeline) {
    pipeline.apply(ParDo.of(new FormatMessage()).named("Format for BigQuery"))
            .apply(BigQueryIO.Write.to(tableRefForOutput(options.as(BigQueryOutputOptions.class)))
                                   .withSchema(AppModel.schema(UserMessage.class))
                                   .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                                   .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                                   .named("Write to BigQuery"));
  }

  private static void publish(MessagesPipelineOptions options,
                              PCollection<UserMessage> pipeline) {
    pipeline.apply(PubsubIO.Write.topic(topicForOutput(options.as(PubsubOutputOptions.class)))
                                 .withCoder(AvroCoder.of(UserMessage.class))
                                 .named("Re-publish Messages"));
  }
}
