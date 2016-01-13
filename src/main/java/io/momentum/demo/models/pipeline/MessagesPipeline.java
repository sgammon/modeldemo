package io.momentum.demo.models.pipeline;


import com.google.api.services.bigquery.model.TableRow;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.io.BigQueryIO;
import com.google.cloud.dataflow.sdk.io.PubsubIO;
import com.google.cloud.dataflow.sdk.options.Default;
import com.google.cloud.dataflow.sdk.options.Description;
import com.google.cloud.dataflow.sdk.options.PipelineOptions;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.values.PCollection;
import com.googlecode.objectify.Key;

import io.momentum.demo.models.logic.pipeline.BasePipeline;
import io.momentum.demo.models.logic.pipeline.PlatformPipeline;
import io.momentum.demo.models.logic.pipeline.options.*;
import io.momentum.demo.models.schema.UserMessage;

import java.util.Map;


/**
 * Created by sam on 1/12/16.
 */
public final class MessagesPipeline extends PlatformPipeline {
  static {
    PipelineOptionsFactory.register(MessagesPipelineOptions.class);
  }

  public interface MessagesPipelineOptions extends PubsubInputOptions,
                                                   BigQueryInputOptions,
                                                   BigQueryOutputOptions,
                                                   FixedWindowOptions {
    @Description("Dry Run")
    @Default.Boolean(false)
    Boolean getDryRun();
    void setDryRun(Boolean value);

    @Description("Enable Windowing")
    @Default.Boolean(true)
    Boolean getEnableWindowing();
    void setEnableWindowing(Boolean value);
  }

  @Override
  protected Class<? extends PipelineOptions> options() {
    return MessagesPipelineOptions.class;
  }

  @Override
  protected Pipeline collapse(Pipeline pipeline) {
    PCollection<UserMessage> messages = window(pipeline.getOptions().as(FixedWindowOptions.class),
                                               read(pipeline.getOptions().as(MessagesPipelineOptions.class), pipeline));

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
      TableRow row = new TableRow();
      row.set("key", Key.create(payload).toWebSafeString());
      row.set("name", payload.getName());
      row.set("message", payload.getMessage());
      row.set("modified", payload.getModified().getTime());
      row.set("created", payload.getCreated().getTime());
      processContext.output(row);
    }
  }

  public static class SerializeMessage extends DoFn<UserMessage, String> {
    @Override
    public void processElement(ProcessContext processContext) throws Exception {
      UserMessage subject = processContext.element();
      String payload = BasePipeline.codec.getWriter().writeValueAsString(subject);
      processContext.output(payload);
    }
  }

  /** -- pipeline steps -- **/
  private static PCollection<UserMessage> read(MessagesPipelineOptions options,
                                               Pipeline pipeline) {
    if (options.isStreaming()) {
      return pipeline.apply(PubsubIO.Read.topic(topicForInput(options)).named("Message Stream"))
                     .apply(ParDo.of(new InflateMessage()).named("Inflate Messages"));
    } else {
      // no-op
      throw new RuntimeException("Non-streaming `MessagesPipeline` is not yet implemented.");
    }
  }

  private static void write(MessagesPipelineOptions options,
                            PCollection<UserMessage> pipeline) {
    pipeline.apply(ParDo.of(new FormatMessage()).named("Format for BigQuery"))
            .apply(BigQueryIO.Write.to(tableRefForOutput(options.as(BigQueryOutputOptions.class)))
                                   .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                                   .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                                   .named("Write to BigQuery"));
  }

  private static void publish(MessagesPipelineOptions options,
                              PCollection<UserMessage> pipeline) {
    pipeline.apply(ParDo.of(new SerializeMessage()).named("Serialize Messages"))
            .apply(PubsubIO.Write.topic(topicForOutput(options.as(PubsubOutputOptions.class))).named("Re-publish Messages"));
  }
}
