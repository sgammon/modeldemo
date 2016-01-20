package io.momentum.demo.models.logic.service.transformers;


import com.google.api.server.spi.config.Transformer;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;


/**
 * Created by sam on 1/12/16.
 */
public final class RefInflator implements Transformer<String, Ref> {
  @Override
  public Ref transformTo(String encoded) {
    try {
      Key target = Key.create(encoded);
      return Ref.create(target);
    } catch (RuntimeException e) {
      return null;
    }
  }

  @Override
  public String transformFrom(Ref target) {
    return target != null ? target.key().toWebSafeString() : null;
  }
}
