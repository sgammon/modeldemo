package io.momentum.demo.models.logic.service.transformers;


import com.google.api.server.spi.config.Transformer;
import com.googlecode.objectify.impl.ref.LiveRef;


/**
 * Created by sam on 1/22/16.
 */
public final class LiveRefSerializer implements Transformer<LiveRef, String> {
  @Override
  public LiveRef transformFrom(String s) {
    throw new IllegalAccessError("not implemented");
  }

  @Override
  public String transformTo(LiveRef ref) {
    if (ref != null && ref.getKey() != null)
      return ref.getKey().toWebSafeString();
    return null;
  }
}
