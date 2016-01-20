package io.momentum.demo.models.pipeline.options;


import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.dataflow.sdk.options.Description;


/**
 * Specifies options related to pipelines that accept input from
 * BigQuery tables in batch form.
 */
public interface BigQueryInputOptions extends PlatformBigQueryOptions {
  @Description("BigQuery input table name")
  String getBigQueryInputTable();
  void setBigQueryInputTable(String table);

  @Description("BigQuery input table schema")
  TableSchema getBigQueryInputSchema();
  void setBigQueryInputSchema(TableSchema schema);
}
