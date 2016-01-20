package io.momentum.demo.models.logic.struct.objectify;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.googlecode.objectify.Ref;

import java.io.IOException;


/**
 * Created by sam on 1/20/16.
 */
public class ObjectifyRefSerializer<T> extends JsonSerializer<Ref<T>> {
  @Override
  public void serialize(Ref<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    T t = value.getValue();
    if (t != null) {
      gen.writeObject(t);
    } else {
      gen.writeString(value.getKey().getString());
    }
  }
}
