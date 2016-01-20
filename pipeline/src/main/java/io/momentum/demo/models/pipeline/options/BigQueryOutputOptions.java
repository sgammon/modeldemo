package io.momentum.demo.models.pipeline.options;


import com.google.cloud.dataflow.sdk.options.*;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Interface specifying pipeline options related to BigQuery output.
 */
public interface BigQueryOutputOptions extends PlatformBigQueryOptions {
  /**
   * Returns the job name as the default BigQuery table name.
   */
  class BigQueryOutputTableFactory implements DefaultValueFactory<String> {
    @Override
    public String create(PipelineOptions options) {
      // calculate day-scoped-name
      String tableName = dayScopedTableName(options.as(BigQueryOutputOptions.class));

      if (tableName == null)  // system opted not to specify any kind of output table
        return options.as(DataflowPipelineOptions.class)
                                         .getJobName()
                                         .replace('-', '_');
      return tableName;
    }

    public static String dayScopedTableName(BigQueryOutputOptions options) {
      if (options.getBigQueryOutputTablePrefix() != null) {
        // calculate our day stamp
        Date d = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
        String tableDaystamp = f.format(d);

        return options.getBigQueryOutputTablePrefix() + tableDaystamp;
      }
      return null;
    }
  }

  @Description("BigQuery output table name")
  @Default.InstanceFactory(BigQueryOutputTableFactory.class)
  String getBigQueryOutputTable();
  void setBigQueryOutputTable(String table);

  @Description("BigQuery output table prefix")
  String getBigQueryOutputTablePrefix();
  void setBigQueryOutputTablePrefix(String prefix);
}
