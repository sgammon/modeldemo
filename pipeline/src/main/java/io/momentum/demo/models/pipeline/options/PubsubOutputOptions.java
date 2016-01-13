package io.momentum.demo.models.pipeline.options;


import com.google.cloud.dataflow.sdk.options.Description;


/**
 * Interface defining options for pipelines that output via pubsub.
 */
public interface PubsubOutputOptions extends PlatformPubsubOptions {
  @Description("PubSub output topic to send results to")
  String getPubsubOutputTopic();
  void setPubsubOutputTopic(String value);
}
