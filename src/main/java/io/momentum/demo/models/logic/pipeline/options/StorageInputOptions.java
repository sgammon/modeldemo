package io.momentum.demo.models.logic.pipeline.options;


import com.google.cloud.dataflow.sdk.options.Description;


/**
 * Interface defining pipeline options related to reading input from GCS.
 */
public interface StorageInputOptions extends PlatformStorageOptions {
  @Description("GCS Input file to process in batch mode")
  String getStorageInputFile();
  void setStorageInputFile(String value);
}
