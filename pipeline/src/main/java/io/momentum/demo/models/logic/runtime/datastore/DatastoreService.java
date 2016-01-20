package io.momentum.demo.models.logic.runtime.datastore;


import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

import io.momentum.demo.models.schema.AppModel;
import io.momentum.demo.models.schema.UserMessage;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by sam on 1/12/16.
 */
public final class DatastoreService {
  private static boolean initialized = false;
  private static ConcurrentHashMap<String, Class<? extends AppModel>> modelMap = new ConcurrentHashMap<>();

  static {
    initialize();
  }

  public static Objectify ofy() {
    initialize();
    return ObjectifyService.ofy();
  }

  public static ObjectifyFactory factory() {
    return ObjectifyService.factory();
  }

  public static void register(Class<? extends AppModel> modelClass) {
    if (modelMap.containsKey(modelClass.getSimpleName()))
      throw new RuntimeException("cannot register model with duplicate name `" + modelClass.getSimpleName() + "`.");
    modelMap.put(modelClass.getSimpleName(), modelClass);
    factory().register(modelClass);
  }

  public static Class<? extends AppModel> resolve(String kind) {
    return modelMap.get(kind);
  }

  public static void initialize() {
    if (!initialized) {
      register(UserMessage.class);

      initialized = true;
    }
  }
}
