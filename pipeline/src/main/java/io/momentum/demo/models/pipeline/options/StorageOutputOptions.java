package io.momentum.demo.models.pipeline.options;


import com.google.cloud.dataflow.sdk.options.*;


/**
 * Interface defining Pipeline options related to outputting to GCS.
 */
public interface StorageOutputOptions extends PlatformStorageOptions, DataflowPipelineOptions {
  /**
   * Returns the job name as the default BigQuery table name.
   */
  class ShardTemplateFactory implements DefaultValueFactory<String> {
    public final static String batchDefault = "-SSSSS-of-NNNNN";  // job name, shard number, total number
    public final static String streamingDefault = "-SSSSS";  // job name, date, shard number

    @Override
    public String create(PipelineOptions options) {
      // use streaming default for streaming pipelines, etc
      if (options instanceof DataflowPipelineOptions &&
              options.as(DataflowPipelineOptions.class)
                     .isStreaming())
        return streamingDefault;
      return batchDefault;
    }
  }

  @Description("Output archive prefix")
  String getStorageOutputPrefix();
  void setStorageOutputPrefix(String value);

  @Description("Output shard template")
  @Default.InstanceFactory(ShardTemplateFactory.class)
  String getOutputShardTemplate();
  void setOutputShardTemplate(String value);

  @Description("Output archive file")
  String getStorageOutputFile();
  void setStorageOutputFile(String value);
}
