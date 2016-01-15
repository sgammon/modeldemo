package io.momentum.demo.models.pipeline.coder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.cloud.dataflow.sdk.coders.*;
import com.google.cloud.dataflow.sdk.util.CloudObject;

import io.momentum.demo.models.schema.AppModel;

import java.io.*;


/**
 * Created by sam on 1/13/16.
 */
public final class ModelCoder<M extends AppModel> extends AtomicCoder<M> {
  public static <T extends AppModel> ModelCoder<T> of(Class<T> clazz) {
    return new ModelCoder<>(clazz);
  }

  @JsonCreator
  @SuppressWarnings("unchecked")
  public static ModelCoder<?> of(@JsonProperty("kind") String classType) throws ClassNotFoundException {
    Class<?> clazz = Class.forName(classType);
    return of((Class<? extends AppModel>) clazz);
  }

  private String kind;

  public ModelCoder(Class<M> type) {
    this.kind = type.getSimpleName();
  }

  @Override
  public void encode(M value, OutputStream outStream, Context context) throws IOException, CoderException {
    CoderInternals.encode(value, outStream, context, new TypeReference<TypedSerializedModel<M>>() { });
  }

  @Override
  public M decode(InputStream inStream, Context context) throws IOException, CoderException {
    return CoderInternals.decode(inStream, context, new TypeReference<TypedSerializedModel<M>>() { });
  }

  @Override
  public String getEncodingId() {
    return CoderInternals.encodingId();
  }

  @Override
  public CloudObject asCloudObject() {
    CloudObject co = super.asCloudObject();
    co.set("kind", kind);
    return co;
  }
}
