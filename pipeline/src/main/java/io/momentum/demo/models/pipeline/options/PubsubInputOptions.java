package io.momentum.demo.models.pipeline.options;


import com.google.cloud.dataflow.sdk.options.Description;
import com.google.cloud.dataflow.sdk.options.GcpOptions;


/**
 * Interface defining options related to input taken via pubsub.
 */
public interface PubsubInputOptions extends PlatformPubsubOptions, GcpOptions {
  @Description("PubSub input topic to listen on")
  String getPubsubInputTopic();
  void setPubsubInputTopic(String value);
}
