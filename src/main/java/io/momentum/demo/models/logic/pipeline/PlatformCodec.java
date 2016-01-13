package io.momentum.demo.models.logic.pipeline;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


/**
 * Created by sam on 1/12/16.
 */
public final class PlatformCodec {
  public static ObjectMapper jsonWriter = new ObjectMapper();
  public static ObjectMapper jsonReader = new ObjectMapper();

  static {
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
