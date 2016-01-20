package io.momentum.demo.models.logic.runtime.render;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.cache.BaseTagCacheKey;
import com.mitchellbosecke.pebble.extension.Extension;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import io.momentum.demo.models.logic.memcache.MemcacheLogic;
import io.momentum.demo.models.logic.runtime.state.AppEngineRuntimeState;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


/**
 * Created by sam on 1/13/16.
 */
public final class TemplateService {
  /* internals */
  private final static String charset = "UTF-8";
  private final static String postfix = ".html";
  private final static String prefix = "/WEB-INF/resources/templates/";
  protected static PebbleEngine engine = null;
  protected static TemplateLoader loader = null;
  protected static ExecutorService executor = null;
  protected static Logger logging = Logger.getLogger(TemplateService.class.getName());
  private static boolean initialized = false;

  /* caches */
  private static final MemcacheLogic memcache;
  private static final Cache<Object, PebbleTemplate> bytecodeCache;
  private static final Cache<BaseTagCacheKey, Object> tagCache;

  /* loaders */
  private static final MemcacheTemplateObjectLoader<BaseTagCacheKey, Object> tagLoader;
  private static final MemcacheTemplateObjectLoader<Object, PebbleTemplate> bytecodeLoader;

  /* cache exceptions */
  private static final class TemplateNotFound extends Exception {
    private final Object key;
    public TemplateNotFound(Object key) {
      this.key = key;
    }

    public Object getKey() {
      return key;
    }
  }

  private static final class MemcacheTemplateObjectLoader<K, V> extends CacheLoader<K, V> {
    @Override
    public V load(K key) throws TemplateNotFound {
      V value = (V)memcache.sync.get(key);
      if (value != null) return value;
      throw new TemplateNotFound(key);
    }
  }

  static {
    initialize();

    memcache = new MemcacheLogic();  // memcache can work independently of other logic

    // build loaders first
    bytecodeLoader = new MemcacheTemplateObjectLoader<>();
    tagLoader = new MemcacheTemplateObjectLoader<>();

    bytecodeCache = CacheBuilder.newBuilder()
                                .concurrencyLevel(1)
                                .maximumSize(100)
                                .build(bytecodeLoader);

    tagCache = CacheBuilder.newBuilder()
                           .concurrencyLevel(1)
                           .maximumSize(50)
                           .build(tagLoader);
  }

  public static void initialize() {
    if (!initialized) {
      logging.fine("Setting up template runtime...");
      pebble();
      initialized = true;
    }
  }

  public static PebbleEngine pebble() {
    if (engine == null) {
      // executor first
      executor = Executors.newSingleThreadExecutor();

      // then file loader
      loader = new TemplateLoader();

      // then pebble engine
      boolean debug = new AppEngineRuntimeState().getDebug();
      PebbleEngine.Builder engineBuilder = new PebbleEngine.Builder()
                                               .loader(loader)
                                               .executorService(executor)
                                               .tagCache(tagCache)
                                               .templateCache(bytecodeCache)
                                               .cacheActive(!debug);

      // build base extensions list
      ArrayList<Extension> baseExtensions = new ArrayList<>();
      baseExtensions.add(engineBuilder.getEscaperExtension());

      engine = engineBuilder.build(baseExtensions);
    }
    return engine;
  }
}
