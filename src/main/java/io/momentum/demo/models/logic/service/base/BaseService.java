package io.momentum.demo.models.logic.service.base;


import com.google.api.server.spi.ServiceException;
import com.google.appengine.api.datastore.Cursor;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.cmd.Query;

import io.momentum.demo.models.logic.data.DatastoreService;
import io.momentum.demo.models.logic.service.exceptions.ServiceError;
import io.momentum.demo.models.logic.service.models.QueryOptions;
import io.momentum.demo.models.logic.service.models.QueryResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;


/**
 * Created by sam on 1/12/16.
 */
public abstract class BaseService {
  protected static Logger logging = Logger.getAnonymousLogger();
  protected static Objectify datastore() { return DatastoreService.ofy(); }

  protected abstract ServiceError[] errorTypes();

  protected QueryResponse prepare(Query q, QueryOptions options) {
    if (options == null) return new QueryResponse(new QueryOptions(), q);
    if (options.getLimit() != null) q.limit(options.getLimit());
    if (options.getOffset() != null) q.offset(options.getOffset());
    if (options.getProject() != null) q.project((String[])options.getProject().toArray());
    if (options.getCursor() != null) q.startAt(Cursor.fromWebSafeString(options.getCursor()));

    // dispatch according to keys-only-ness
    if (options.getKeysOnly() != null) {
      if (options.getKeysOnly()) {
        return new QueryResponse(options, q.keys());
      } else {
        return new QueryResponse(options, q);
      }
    }
    return new QueryResponse(options, q);
  }

  protected <T extends ServiceError> ServiceException fail(T errorType) {
    Class<? extends ServiceException> excType = errorType.exception();
    try {
      ServiceException exc = excType.getConstructor(ServiceError.class).newInstance(errorType);
      return exc;

    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException f) {
      throw new RuntimeException(f);
    } catch (NoSuchMethodException n) {
      throw new RuntimeException(n);
    } catch (InvocationTargetException t) {
      throw new RuntimeException(t);
    }
  }
}
