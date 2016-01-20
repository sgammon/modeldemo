package io.momentum.demo.models.logic.struct.objectify;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.googlecode.objectify.Key;

import java.io.IOException;


/**
 * Created by sam on 1/20/16.
 */
public final class ObjectifyKeySerializer<T> extends JsonSerializer<Key<T>> {
  @Override
  public void serialize(Key<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeString(value.getString());
  }
}
