package io.momentum.demo.models.pipeline;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


/**
 * Created by sam on 1/12/16.
 */
public final class PlatformCodec {
  public ObjectMapper jsonWriter = new ObjectMapper();
  public ObjectMapper jsonReader = new ObjectMapper();

  public PlatformCodec() {
    // apply default settings
    jsonWriter.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    jsonReader.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  public ObjectMapper getWriter() {
    return jsonWriter;
  }

  public ObjectMapper getReader() {
    return jsonReader;
  }
}
