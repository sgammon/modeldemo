package io.momentum.demo.models.pipeline.coder;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

import io.momentum.demo.models.logic.runtime.datastore.DatastoreService;
import io.momentum.demo.models.schema.AppModel;


/**
 * Created by sam on 1/14/16.
 */
final class ModelTypeResolver extends TypeIdResolverBase {
  private JavaType basetype;

  private static Objectify datastore() {
    return DatastoreService.ofy();
  }

  @Override
  public void init(JavaType javaType) {
    this.basetype = javaType;
  }

  @Override
  public JsonTypeInfo.Id getMechanism() {
    return JsonTypeInfo.Id.CUSTOM;
  }

  @Override
  public String idFromValue(Object o) {
    try {
      AppModel target = ((AppModel) o);
      return target.kind();
    } catch (ClassCastException e) {
      throw new RuntimeException("Cannot cast non-model using ModelTypeResolver: " + e.getLocalizedMessage());
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public String idFromValueAndType(Object o, Class<?> aClass) {
    try {
      Class<? extends AppModel> target = ((Class<? extends AppModel>) o);
      return Key.create(target, "1").getKind();
    } catch (ClassCastException e) {
      throw new RuntimeException("Cannot cast non-model using ModelTypeResolver: " + e.getLocalizedMessage());
    }
  }

  @Override
  public JavaType typeFromId(DatabindContext context, String id) {
    datastore();
    Class<? extends AppModel> model = DatastoreService.resolve(id);
    return this.basetype;
  }
}
