package io.momentum.demo.models.pipeline.options;


import com.google.cloud.dataflow.sdk.options.Default;
import com.google.cloud.dataflow.sdk.options.Description;
import com.google.cloud.dataflow.sdk.options.PipelineOptions;


/**
 * Created by sam on 10/20/15.
 */
public interface PlatformPipelineOptions extends PipelineOptions {
  @Description("Local mode")
  @Default.Boolean(false)
  Boolean getLocal();
  void setLocal(Boolean value);

  @Description("Blocking mode")
  @Default.Boolean(false)
  Boolean getBlocking();
  void setBlocking(Boolean value);

  @Description("Dry Run")
  @Default.Boolean(false)
  Boolean getDryRun();
  void setDryRun(Boolean value);

  @Description("Enable Windowing")
  @Default.Boolean(true)
  Boolean getEnableWindowing();
  void setEnableWindowing(Boolean value);

  @Description("Output Table Prefix")
  String getOutputTablePrefix();
  void setOutputTablePrefix(String value);
}
