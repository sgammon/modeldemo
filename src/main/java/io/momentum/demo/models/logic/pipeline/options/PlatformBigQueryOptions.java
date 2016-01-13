package io.momentum.demo.models.logic.pipeline.options;


import com.google.cloud.dataflow.sdk.options.BigQueryOptions;
import com.google.cloud.dataflow.sdk.options.Description;


/**
 * Created by sam on 9/9/15.
 */
public interface PlatformBigQueryOptions extends BigQueryOptions {
  @Description("BigQuery dataset name")
  String getBigQueryDataset();
  void setBigQueryDataset(String dataset);
}
