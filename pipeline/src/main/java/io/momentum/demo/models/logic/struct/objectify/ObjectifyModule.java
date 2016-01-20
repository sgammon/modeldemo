package io.momentum.demo.models.logic.struct.objectify;


import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;


/**
 * Created by sam on 1/20/16.
 */
public final class ObjectifyModule extends SimpleModule {
  @SuppressWarnings({"rawtypes", "unchecked"})
  public ObjectifyModule() {
    super("ObjectifyModule", new Version(1, 0, 0, null));
    addSerializer(Ref.class, new ObjectifyRefSerializer());
    addSerializer(Key.class, new ObjectifyKeySerializer());
  }
}
