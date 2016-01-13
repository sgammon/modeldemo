package io.momentum.demo.models.pipeline.coder;


import com.google.cloud.dataflow.sdk.coders.AvroCoder;
import com.google.cloud.dataflow.sdk.coders.Coder;
import com.google.cloud.dataflow.sdk.coders.StandardCoder;

import io.momentum.demo.models.logic.service.models.SerializedModel;
import io.momentum.demo.models.schema.AppModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;


/**
 * Created by sam on 1/13/16.
 */
public final class ModelCoder<M extends AppModel> extends StandardCoder<M> {
  private final Class<M> modelType;
  private final AvroCoder<SerializedModel> avroCoder = AvroCoder.of(SerializedModel.class);

  private ModelCoder(Class<M> modelType) {
    this.modelType = modelType;
  }

  public static <M extends AppModel> ModelCoder<M> of(Class<M> klass) {
    return new ModelCoder<>(klass);
  }

  @Override
  public List<? extends Coder<?>> getCoderArguments() {
    return avroCoder.getCoderArguments();
  }

  public byte[] encode(M modelObj) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    encode(modelObj, outputStream, Context.OUTER);
    return outputStream.toByteArray();
  }

  @Override
  public void encode(M modelObj, OutputStream outputStream, Context context) throws IOException {
    avroCoder.encode(modelObj.serialize(true), outputStream, context);
  }

  @Override
  public M decode(InputStream inputStream, Context context) throws IOException {
    SerializedModel deserialized = avroCoder.decode(inputStream, context);
    return AppModel.deserialize(deserialized);
  }

  @Override
  public void verifyDeterministic() throws NonDeterministicException {
    avroCoder.verifyDeterministic();
  }
}
