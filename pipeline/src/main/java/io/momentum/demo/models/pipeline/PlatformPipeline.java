package io.momentum.demo.models.pipeline;


import com.google.api.services.bigquery.model.TableReference;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.coders.CoderRegistry;
import com.google.cloud.dataflow.sdk.options.DataflowPipelineOptions;
import com.google.cloud.dataflow.sdk.options.GcpOptions;
import com.google.cloud.dataflow.sdk.options.PipelineOptions;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.runners.BlockingDataflowPipelineRunner;
import com.google.cloud.dataflow.sdk.runners.DataflowPipelineRunner;
import com.google.cloud.dataflow.sdk.runners.DirectPipelineRunner;

import io.momentum.demo.models.pipeline.coder.ModelCoder;
import io.momentum.demo.models.pipeline.options.*;
import io.momentum.demo.models.schema.AppModel;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;


/**
 * Created by sam on 1/12/16.
 */
public abstract class PlatformPipeline extends BasePipeline {
  /**
   * -- tools --
   **/
  public static String topicForInput(PubsubInputOptions options) {
    // @TODO: cross-project pubsub
    return "projects/" + options.as(GcpOptions.class).getProject() + "/topics/" + options.getPubsubInputTopic();
  }

  public static String topicForOutput(PubsubOutputOptions options) {
    // @TODO: cross-project pubsub
    return "projects/" + options.as(GcpOptions.class).getProject() + "/topics/" + options.getPubsubOutputTopic();
  }

  public static TableReference tableRefForInput(BigQueryInputOptions options) {
    return new TableReference().setProjectId(options.getProject())
                               .setDatasetId(options.getBigQueryDataset())
                               .setTableId(options.getBigQueryInputTable());
  }

  public static TableReference tableRefForOutput(BigQueryOutputOptions options) {
    if (options.getBigQueryOutputTablePrefix() != null) {
      return new TableReference().setProjectId(options.getProject())
                                 .setDatasetId(options.getBigQueryDataset())
                                 .setTableId(
                                     BigQueryOutputOptions.BigQueryOutputTableFactory.dayScopedTableName(options));
    }

    // explicit or unset output table name
    return new TableReference().setProjectId(options.getProject())
                               .setDatasetId(options.getBigQueryDataset())
                               .setTableId(options.getBigQueryOutputTable());
  }

  public static String shardTemplateForOutput(StorageOutputOptions options) {
    return options.getOutputShardTemplate();
  }

  public static String outputPrefixForStorage(StorageOutputOptions options) {
    return options.getStorageOutputPrefix()
                  .replace("J", options.getJobName())
                  .replace("D", dayScopeName());
  }

  private static String dayScopeName() {
    return dayScopeName(new Date());
  }

  private static String dayScopeName(Date i) {
    return daystampFormat.format(i);
  }

  public static void main(String[] args)
      throws IOException,
             ClassNotFoundException,
             InstantiationException,
             IllegalAccessException {
    try {
      // prepare harness
      PlatformPipeline pipe = resolve(args[0]).newInstance();
      Class<? extends PipelineOptions> optionsType = pipe.options();

      PipelineOptions options = pipe.prepare(args, optionsType);
      PlatformPipelineHarness harness = new PlatformPipelineHarness(options);
      harness.setup();

      // execute
      Pipeline pipeline = Pipeline.create(options);
      pipe.collapse(pipeline);
      pipeline.run();
    } catch (ClassNotFoundException e) {
      logging.severe("Failed to find required class for operation: " + e.getLocalizedMessage());
      throw e;

    } catch (InstantiationException e) {
      logging.severe("Failed to instantiate pipeline for operation: " + e.getLocalizedMessage());
      throw e;

    }
  }

  private static Class<? extends PlatformPipeline> resolve(String name)
      throws ClassNotFoundException {
    return (Class<? extends PlatformPipeline>) Class.forName(name);
  }

  /**
   * -- interface --
   **/
  protected abstract Class<? extends PipelineOptions> options();

  /**
   * -- entrypoint --
   **/
  @Override
  protected PipelineOptions prepare(String[] args,
                                    Class<? extends PipelineOptions> optionsType) throws IOException {
    // inflate arguments
    logging.info("Constructing pipeline...");
    String[] args_clean = Arrays.copyOfRange(args, 1, args.length);
    PipelineOptions options = PipelineOptionsFactory.fromArgs(args_clean)
                                                    .withValidation()
                                                    .as(optionsType);

    // options cannot be null
    if (options == null) throw new RuntimeException("Found null pipeline options.");

    if (options instanceof PlatformPipelineOptions &&
            options.as(PlatformPipelineOptions.class)
                   .getLocal()) {

      // setup local runner
      logging.info("Running locally with pipeline runner `DirectPipelineRunner`...");
      options.setRunner(DirectPipelineRunner.class);
    } else {

      // is this a streaming job?
      if (options instanceof DataflowPipelineOptions &&
              options.as(DataflowPipelineOptions.class)
                     .isStreaming()) {
        logging.info("Running in production with Dataflow service (pipeline is in streaming mode)...");
        options.setRunner(DataflowPipelineRunner.class);
      } else {

        if (options instanceof PlatformPipelineOptions &&
                options.as(PlatformPipelineOptions.class).getBlocking()) {
          logging.info("Running in production with blocking Dataflow service...");
          options.setRunner(BlockingDataflowPipelineRunner.class);
        } else {
          // we're using dataflow service
          logging.info("Running in production with Dataflow service...");
          options.setRunner(DataflowPipelineRunner.class);
        }
      }
    }
    return options;
  }
}
