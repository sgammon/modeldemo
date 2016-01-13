package io.momentum.demo.models.pipeline.options;


import com.google.cloud.dataflow.sdk.options.Description;
import com.google.cloud.dataflow.sdk.options.GcsOptions;
import com.google.cloud.dataflow.sdk.options.PipelineOptions;


/**
 * Created by sam on 9/9/15.
 */
public interface PlatformStorageOptions extends PipelineOptions, GcsOptions {
  @Description("Application storage bucket")
  String getStorageBucket();
  void setStorageBucket(String value);
}
