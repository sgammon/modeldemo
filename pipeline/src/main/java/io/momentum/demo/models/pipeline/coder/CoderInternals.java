package io.momentum.demo.models.pipeline.coder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.dataflow.sdk.coders.Coder;
import com.googlecode.objectify.ObjectifyService;

import io.momentum.demo.models.logic.runtime.datastore.DatastoreService;
import io.momentum.demo.models.logic.service.models.SerializedModel;
import io.momentum.demo.models.pipeline.PlatformCodec;
import io.momentum.demo.models.schema.AppModel;

import java.io.*;


/**
 * Created by sam on 1/14/16.
 */
final class CoderInternals {
  private static final CoderMode coder = CoderMode.JSON;
  private static final PlatformCodec codec = new PlatformCodec();

  private static Closeable datastore() {
    // warm datastore for decoding/encoding models
    Closeable context = ObjectifyService.begin();
    DatastoreService.ofy();
    return context;
  }

  /** -- implementations -- **/
  private enum CoderMode implements ModelSerializer {
    JSON {
      @Override
      public String id() {
        return "json";
      }

      @Override
      public <M extends AppModel> void encode(SerializedModel value, OutputStream outStream, Coder.Context context,
                                              TypeReference<TypedSerializedModel<M>> reference) throws IOException {
        String encoded = codec.getWriter()
                              .writeValueAsString(value);
        codec.getWriter().writeValue(outStream, value);
      }

      @Override
      public <M extends AppModel> TypedSerializedModel<M> decode(InputStream inStream, Coder.Context context,
                                                                 TypeReference<TypedSerializedModel<M>> reference) throws IOException {
        TypedSerializedModel<M> m = codec.getReader()
                                 .disable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                                 .readValue(inStream, reference);
        return m;
      }
    },

    JAVA {
      @Override
      public String id() {
        return "java";
      }

      @Override
      public <M extends AppModel> void encode(SerializedModel value, OutputStream outStream, Coder.Context context,
                                              TypeReference<TypedSerializedModel<M>> reference) throws IOException {
        throw new RuntimeException("java serialized model coder not implemented");
      }

      @Override
      public <M extends AppModel> TypedSerializedModel<M> decode(InputStream inStream, Coder.Context context,
                                                                 TypeReference<TypedSerializedModel<M>> reference) throws IOException {
        throw new RuntimeException("java serialized model coder not implemented");
      }
    },

    MPACK {
      @Override
      public String id() {
        return "mpack";
      }

      @Override
      public <M extends AppModel> void encode(SerializedModel value, OutputStream outStream, Coder.Context context,
                                              TypeReference<TypedSerializedModel<M>> reference) throws IOException {
        throw new RuntimeException("mpack serialized model coder not implemented");
      }

      @Override
      public <M extends AppModel> TypedSerializedModel<M> decode(InputStream inStream, Coder.Context context,
                                                                 TypeReference<TypedSerializedModel<M>> reference) throws IOException {
        throw new RuntimeException("mpack serialized model coder not implemented");
      }
    }
  }

  /** -- public interface -- **/
  public static String encodingId() {
    return coder.id();
  }

  public static <M extends AppModel> void encode(M value,
                                                 OutputStream outStream,
                                                 Coder.Context context,
                                                 TypeReference<TypedSerializedModel<M>> reference) throws IOException {
    try (Closeable ofy = datastore()) {
      coder.encode(value.serialize(), outStream, context, reference);
    }
  }

  public static <M extends AppModel> M decode(InputStream inStream,
                                              Coder.Context context,
                                              TypeReference<TypedSerializedModel<M>> reference) throws IOException {
    try (Closeable ofy = datastore()) {
      TypedSerializedModel<M> s = coder.decode(inStream, context, reference);
      return s.getData();
    }
  }
}
