package io.momentum.demo.models.logic.service.transformers;


import com.google.api.server.spi.config.Transformer;
import com.googlecode.objectify.Key;

import io.momentum.demo.models.logic.service.models.SerializedKey;


/**
 * Created by sam on 1/12/16.
 */
public final class KeySerializer implements Transformer<Key, SerializedKey> {
  @Override
  public SerializedKey transformTo(Key key) {
    return key != null ? SerializedKey.fromKey(key) : null;
  }

  @Override
  public Key transformFrom(SerializedKey target) {
    if (target == null) return null;
    try {
      return Key.create(target.encoded);
    } catch (RuntimeException e) {
      return null;
    }
  }
}
