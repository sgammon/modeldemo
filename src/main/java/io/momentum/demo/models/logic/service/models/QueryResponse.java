package io.momentum.demo.models.logic.service.models;


import com.google.api.server.spi.config.ApiTransformer;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.QueryExecute;

import io.momentum.demo.models.logic.service.transformers.QueryResponseTransformer;
import io.momentum.demo.models.schema.AppModel;

import java.util.ArrayList;
import java.util.logging.Logger;


/**
 * Created by sam on 1/12/16.
 */
@ApiTransformer(QueryResponseTransformer.class)
public final class QueryResponse {
  /** -- internals -- **/
  private static final Logger logging = Logger.getLogger(QueryResponse.class.getSimpleName());

  /** -- properties -- **/
  public final QueryOptions options;
  public final QueryExecute executor;

  public QueryResponse(QueryOptions options, QueryExecute executor) {
    this.executor = executor;
    this.options = options != null ? options : new QueryOptions();
  }

  /** -- public interface -- **/
  public SerializedQueryResponse execute() {
    ArrayList<SerializedDatastoreObject> objects = new ArrayList<>(options.getLimit() != null ? options.getLimit() : 20);

    if (options.getKeysOnly()) {
      // decode and serialize query keys
      Key inner;
      for (Object result : this.executor) {
        try {
          inner = (Key)result;
          objects.add(SerializedKey.fromKey(inner));

        } catch (ClassCastException e) {
          throw new RuntimeException(e);
        }
      }

    } else {
      // iterate over models, serializing as we go
      AppModel inner;

      // decode models
      for (Object result : this.executor) {
        try {
          inner = (AppModel)result;
          objects.add(inner.serialize());

        } catch (ClassCastException e) {
          logging.severe("`ClassCastException` during query execution, skipping record: " + e.getLocalizedMessage());
        }
      }
    }
    return new SerializedQueryResponse(objects);
  }
}
