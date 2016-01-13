package io.momentum.demo.models.pipeline.options;


import com.google.cloud.dataflow.sdk.options.Default;
import com.google.cloud.dataflow.sdk.options.Description;
import com.google.cloud.dataflow.sdk.options.PipelineOptions;


/**
 * Created by sam on 9/21/15.
 */
public interface FixedWindowOptions extends PipelineOptions {
  @Description("Duration of fixed window window")
  @Default.Integer(5)
  Integer getFixedWindowDuration();
  void setFixedWindowDuration(Integer value);
}
