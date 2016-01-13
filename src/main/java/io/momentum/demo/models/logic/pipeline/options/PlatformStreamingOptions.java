package io.momentum.demo.models.logic.pipeline.options;


import com.google.cloud.dataflow.sdk.options.Default;
import com.google.cloud.dataflow.sdk.options.Description;
import com.google.cloud.dataflow.sdk.options.StreamingOptions;


/**
 * Defines options for pipelines that operate in a streaming fasion.
 */
public interface PlatformStreamingOptions extends StreamingOptions {
  @Description("Whether to keep jobs running on the Dataflow service after local process exit")
  @Default.Boolean(false)
  boolean getKeepJobsRunning();
  void setKeepJobsRunning(boolean keepJobsRunning);

  @Description("Number of workers to use when executing the injector pipeline")
  @Default.Integer(1)
  int getInjectorNumWorkers();
  void setInjectorNumWorkers(int numWorkers);
}
