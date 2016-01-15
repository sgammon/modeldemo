package io.momentum.demo.models.logic.service.transformers;


import com.google.api.server.spi.config.Transformer;
import com.googlecode.objectify.Key;

import io.momentum.demo.models.logic.service.models.EncodedKey;


/**
 * Created by sam on 1/12/16.
 */
public final class KeyInflator implements Transformer<EncodedKey, String> {
  @Override
  public EncodedKey transformFrom(String encoded) {
    try {
      return EncodedKey.create(encoded);
    } catch (RuntimeException e) {
      return null;
    }
  }

  @Override
  public String transformTo(EncodedKey key) {
    return key != null ? key.toWebSafeString() : null;
  }
}
