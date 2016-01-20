package io.momentum.demo.models.logic.service.transformers;


import com.google.api.server.spi.config.Transformer;

import io.momentum.demo.models.logic.service.models.SerializedModel;
import io.momentum.demo.models.schema.AppModel;


/**
 * Created by sam on 1/12/16.
 */
public final class ModelTransformer implements Transformer<AppModel, SerializedModel> {
  @Override
  public SerializedModel transformTo(AppModel model) {
    if (model == null) return null;
    return model.serialize();
  }

  @Override
  public AppModel transformFrom(SerializedModel model) {
    if (model == null) return null;
    return model.getData();
  }
}
