package io.momentum.demo.models.logic.runtime.datastore;


import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

import io.momentum.demo.models.schema.UserMessage;


/**
 * Created by sam on 1/12/16.
 */
public final class DatastoreService {
  private static boolean initialized = false;

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

  public static void initialize() {
    if (!initialized) {
      factory().register(UserMessage.class);

      initialized = true;
    }
  }
}
