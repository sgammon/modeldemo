package io.momentum.demo.models.logic;


import io.momentum.demo.models.logic.google.GoogleLogic;
import io.momentum.demo.models.logic.memcache.MemcacheLogic;
import io.momentum.demo.models.logic.pubsub.PubsubLogic;
import io.momentum.demo.models.logic.runtime.state.AppEnginePlatformState;
import io.momentum.demo.models.logic.runtime.state.AppEngineRuntimeState;


/**
 * Created by sam on 1/13/16.
 */
public final class PlatformBridge {
  private static PlatformBridge __singleton = null;

  /* appengine services */
  public final MemcacheLogic memcache;
  public final PubsubLogic pubsub;

  /* vendor services */
  public final GoogleLogic google;

  protected PlatformBridge() {
    memcache = new MemcacheLogic();
    memcache.setBridge(this);

    pubsub = new PubsubLogic();
    pubsub.setBridge(this);

    google = new GoogleLogic();
    google.setBridge(this);
  }

  public static PlatformBridge acquire() {
    if (__singleton == null)
      __singleton = new PlatformBridge();
    return __singleton;
  }

  public AppEngineRuntimeState getRuntimeState() {
    return new AppEngineRuntimeState();
  }

  public AppEnginePlatformState getPlatformState() {
    return new AppEnginePlatformState();
  }
}
