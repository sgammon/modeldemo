package io.momentum.demo.models.pipeline.coder;


import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.dataflow.sdk.coders.Coder;
import com.google.cloud.dataflow.sdk.coders.CoderException;

import io.momentum.demo.models.logic.service.models.SerializedModel;
import io.momentum.demo.models.schema.AppModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by sam on 1/14/16.
 */
interface ModelSerializer {
  String id();

  <M extends AppModel> void encode(SerializedModel value,
                                   OutputStream outStream,
                                   Coder.Context context,
                                   TypeReference<TypedSerializedModel<M>> reference) throws IOException,
                                                                                            CoderException;

  <M extends AppModel> TypedSerializedModel<M> decode(InputStream inStream,
                                                      Coder.Context context,
                                                      TypeReference<TypedSerializedModel<M>> reference) throws IOException,
                                                                                                               CoderException;
}
