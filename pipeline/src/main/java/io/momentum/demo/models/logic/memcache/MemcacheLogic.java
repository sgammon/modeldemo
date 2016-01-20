package io.momentum.demo.models.logic.memcache;


import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import io.momentum.demo.models.logic.PlatformLogic;


/**
 * Created by sam on 1/13/16.
 */
public final class MemcacheLogic extends PlatformLogic {
  public final MemcacheService sync = MemcacheServiceFactory.getMemcacheService();
  public final AsyncMemcacheService async = MemcacheServiceFactory.getAsyncMemcacheService();
}
