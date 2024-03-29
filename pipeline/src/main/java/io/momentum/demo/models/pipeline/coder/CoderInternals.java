package io.momentum.demo.models.pipeline.coder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.dataflow.sdk.coders.Coder;

import io.momentum.demo.models.logic.service.models.SerializedModel;
import io.momentum.demo.models.pipeline.PlatformCodec;
import io.momentum.demo.models.schema.AppModel;

import java.io.*;
import java.nio.charset.StandardCharsets;


/**
 * Created by sam on 1/14/16.
 */
final class CoderInternals {
  private static final CoderMode coder = CoderMode.JSON;
  private static final PlatformCodec codec = new PlatformCodec();

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
        outStream.write(encoded.getBytes());
      }

      @Override
      public <M extends AppModel> TypedSerializedModel<M> decode(InputStream inStream, Coder.Context context,
                                                                 TypeReference<TypedSerializedModel<M>> reference) throws IOException {
        // build readers
        InputStreamReader isr = new InputStreamReader(inStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(isr);

        // build a string builder
        StringBuilder output = new StringBuilder();
        String aux = "";

        // read the output into the string buffer
        while ((aux = reader.readLine()) != null) {
          output.append(aux);
        }

        String jsonPayload = output.toString();
        TypedSerializedModel<M> m = codec.getReader()
                                           .readValue(jsonPayload, reference);
        return m;  // @NOTE: we are intentionally not closing the stream - if we did, it would except as we don't own it
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
    coder.encode(value.serialize(), outStream, context, reference);
  }

  public static <M extends AppModel> M decode(InputStream inStream,
                                              Coder.Context context,
                                              TypeReference<TypedSerializedModel<M>> reference) throws IOException {
    TypedSerializedModel<M> s = coder.decode(inStream, context, reference);
    return s.getData();
  }
}
