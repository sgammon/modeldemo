package io.momentum.demo.models.logic.service.models;


import com.google.api.server.spi.config.ApiTransformer;
import com.googlecode.objectify.Key;

import io.momentum.demo.models.logic.service.transformers.KeyInflator;


/**
 * Created by sam on 1/12/16.
 */
@ApiTransformer(KeyInflator.class)
public final class EncodedKey {
  private final Key inner;

  private EncodedKey(Key inner) {
    this.inner = inner;
  }

  public String toWebSafeString() {
    return inner.toWebSafeString();
  }

  public static EncodedKey create(String encoded) {
    return new EncodedKey(Key.create(encoded));
  }

  public static EncodedKey create(Key key) {
    return new EncodedKey(key);
  }

  public Key getKey() {
    return inner;
  }
}
