package io.momentum.demo.models.pipeline;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.momentum.demo.models.logic.struct.objectify.ObjectifyModule;


/**
 * Created by sam on 1/12/16.
 */
public final class PlatformCodec {
  public ObjectMapper jsonEngine = new ObjectMapper();

  public PlatformCodec() {
    // add modules
    jsonEngine.registerModule(new ObjectifyModule());


    // apply default settings
    jsonEngine.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    jsonEngine.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  public ObjectMapper getWriter() {
    return jsonEngine;
  }

  public ObjectMapper getReader() {
    return jsonEngine;
  }
}
