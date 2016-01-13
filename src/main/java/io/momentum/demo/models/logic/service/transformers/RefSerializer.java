package io.momentum.demo.models.logic.service.transformers;


import com.google.api.server.spi.config.Transformer;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;


/**
 * Created by sam on 1/12/16.
 */
public final class RefSerializer implements Transformer<Ref, String> {
  @Override
  public String transformTo(Ref ref) {
    return ref != null ? ref.key().toWebSafeString() : null;
  }

  @Override
  public Ref transformFrom(String encoded) {
    try {
      Key target = Key.create(encoded);
      return Ref.create(target);
    } catch (RuntimeException e) {
      return null;
    }
  }
}
