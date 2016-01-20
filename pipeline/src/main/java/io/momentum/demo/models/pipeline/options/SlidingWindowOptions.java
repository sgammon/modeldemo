package io.momentum.demo.models.pipeline.options;


import com.google.cloud.dataflow.sdk.options.DataflowPipelineOptions;
import com.google.cloud.dataflow.sdk.options.Default;
import com.google.cloud.dataflow.sdk.options.Description;


/**
 * Created by sam on 9/21/15.
 */
public interface SlidingWindowOptions extends DataflowPipelineOptions {
  @Description("Duration of sliding window")
  @Default.Integer(5)
  Integer getSlidingWindowDuration();
  void setSlidingWindowDuration(Integer value);

  @Description("Interval of sliding window")
  @Default.Integer(1)
  Integer getSlidingWindowInterval();
  void setSlidingWindowInterval(Integer value);
}
