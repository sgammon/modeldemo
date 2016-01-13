package io.momentum.demo.models.logic.pipeline;


import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.model.Dataset;
import com.google.api.services.bigquery.model.DatasetReference;
import com.google.api.services.bigquery.model.Table;
import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.model.Topic;
import com.google.api.services.storage.Storage;
import com.google.cloud.dataflow.sdk.options.*;
import com.google.cloud.dataflow.sdk.util.Transport;
import javax.servlet.http.HttpServletResponse;

import io.momentum.demo.models.logic.pipeline.options.*;

import java.io.IOException;
import java.util.logging.Logger;


/**
 * Created by sam on 1/12/16.
 */
public final class PlatformPipelineHarness {
  private static final Logger logging = Logger.getAnonymousLogger();

  // -- private members -- //
  private final PipelineOptions options;
  private Bigquery bigquery = null;
  private Pubsub pubsub = null;
  private Storage storage = null;

  public PlatformPipelineHarness(PipelineOptions options) {
    this.options = options;
  }

  public void setup() throws IOException {
    setupPubsub();
    setupBigquery();
    setupCloudStorage();
  }

  private void setupPubsub()
      throws IOException {
    if (options instanceof DataflowPipelineOptions) {
      logging.info("Setting up PubSub...");
      if (pubsub == null)
        pubsub = Transport.newPubsubClient(options.as(DataflowPipelineOptions.class))
                          .setApplicationName(options.as(DataflowPipelineOptions.class).getProject() + "."
                                                  + options.as(ApplicationNameOptions.class)
                                                           .getAppName())
                          .build();

      // input topic must exist ahead of time
      if (options instanceof PubsubInputOptions &&
              options.as(PubsubInputOptions.class)
                     .getPubsubInputTopic() != null) {
        if (options.as(DataflowPipelineOptions.class).isStreaming()) {
          logging.info("Setting up PubSub inputs...");
          String targetTopic = PlatformPipeline.topicForInput(options.as(PubsubInputOptions.class));

          if (executeNullIfNotFound(pubsub.projects()
                                          .topics()
                                          .get(targetTopic)) == null) {
            throw new RuntimeException("PubSub input topic '" +
                                           targetTopic + "' was not found to exist.");
          }
        }
      }
    } else {
      logging.warning("Skipping input pubsub (not running on Dataflow Service)...");
    }

    // output topic can be created on-the-fly
    if (options instanceof PubsubOutputOptions &&
            options.as(PubsubOutputOptions.class)
                   .getPubsubOutputTopic() != null) {
      logging.info("Setting up PubSub outputs...");
      if (executeNullIfNotFound(pubsub.projects()
                                      .topics()
                                      .get(PlatformPipeline.topicForOutput(
                                          options.as(PubsubOutputOptions.class)))) == null) {
        // create it - output topic wasn't found
        pubsub.projects()
              .topics()
              .create(PlatformPipeline.topicForOutput(options.as(PubsubOutputOptions.class)), new Topic())
              .execute();
      }
    }
  }

  private void setupBigquery()
      throws IOException {
    logging.info("Setting up BigQuery...");
    if (bigquery == null)
      bigquery = Transport.newBigQueryClient(options.as(BigQueryOptions.class))
                          .setApplicationName(options.as(DataflowPipelineOptions.class).getProject())
                          .build();

    if (options instanceof BigQueryInputOptions ||
            options instanceof BigQueryOutputOptions &&
                options.as(PlatformBigQueryOptions.class)
                       .getBigQueryDataset() != null) {
      logging.info("Setting up BigQuery dataset...");
      Bigquery.Datasets datasetsService = bigquery.datasets();

      // find dataset first
      if (executeNullIfNotFound(datasetsService.get(options.as(DataflowPipelineOptions.class).getProject(),
                                                    options.as(PlatformBigQueryOptions.class)
                                                           .getBigQueryDataset())) == null) {
        // need to create target/subject dataset
        Dataset newDataset = new Dataset().setDatasetReference(new DatasetReference()
                                                                   .setProjectId(options.as(DataflowPipelineOptions.class).getProject())
                                                                   .setDatasetId(
                                                                       options.as(PlatformBigQueryOptions.class)
                                                                              .getBigQueryDataset()));

        // execute the insert request
        datasetsService.insert(options.as(DataflowPipelineOptions.class).getProject(), newDataset)
                       .execute();
      }

      /* continue by resolving input and output tables */
      Bigquery.Tables tableService = bigquery.tables();
      Table inputDataref = null;
      Table outputDataref = null;

      // input table
      if (options instanceof BigQueryInputOptions &&
              options.as(BigQueryInputOptions.class)
                     .getBigQueryInputTable() != null) {
        logging.info("Setting up BigQuery inputs...");
        inputDataref = executeNullIfNotFound(tableService.get(options.as(DataflowPipelineOptions.class).getProject(),
                                                              options.as(PlatformBigQueryOptions.class)
                                                                     .getBigQueryDataset(),
                                                              options.as(BigQueryInputOptions.class)
                                                                     .getBigQueryInputTable()));

        if (inputDataref == null) {
          // table needs to exist ahead of time
          throw new RuntimeException("BigQuery input table '" +
                                         options.as(BigQueryInputOptions.class)
                                                .getBigQueryInputTable() +
                                         "' was not found to exist in dataset '" +
                                         options.as(PlatformBigQueryOptions.class)
                                                .getBigQueryDataset() + "'");
        } else {
          if (options instanceof BigQueryOutputOptions &&
                  options.as(BigQueryOutputOptions.class)
                         .getBigQueryOutputTable() != null) {

            String outputTable = options.as(BigQueryOutputOptions.class).getBigQueryOutputTable();
            String inputTable = options.as(BigQueryInputOptions.class).getBigQueryInputTable();

            // check if input/output tables are the same, which is not supported
            if (outputTable.equals(inputTable))
              throw new RuntimeException("BigQuery input table and output table may not be equal (got '" +
                                             outputTable + "' for both).");
          }
        }
      }

      // output table
      if (options instanceof BigQueryOutputOptions &&
              options.as(BigQueryOutputOptions.class)
                     .getBigQueryOutputTable() != null) {
        // create output dataset if needed
        logging.info("Setting up BigQuery outputs...");
        executeNullIfNotFound(tableService.get(options.as(DataflowPipelineOptions.class).getProject(),
                                               options.as(PlatformBigQueryOptions.class)
                                                      .getBigQueryDataset(),
                                               options.as(BigQueryOutputOptions.class)
                                                      .getBigQueryOutputTable()));
      }
    }
  }

  private void setupCloudStorage() {
    logging.info("Setting up storage...");
    if (storage != null)
      storage = Transport.newStorageClient(options.as(GcsOptions.class))
                         .setApplicationName(options.as(DataflowPipelineOptions.class).getProject())
                         .build();
  }

  private static <T> T executeNullIfNotFound(AbstractGoogleClientRequest<T> request)
      throws IOException {
    try {
      return request.execute();
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == HttpServletResponse.SC_NOT_FOUND) {
        return null;
      } else {
        throw e;
      }
    }
  }
}
