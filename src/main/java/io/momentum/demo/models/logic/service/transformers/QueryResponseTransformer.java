package io.momentum.demo.models.logic.service.transformers;


import com.google.api.server.spi.config.Transformer;

import io.momentum.demo.models.logic.service.models.QueryResponse;
import io.momentum.demo.models.logic.service.models.SerializedQueryResponse;


/**
 * Created by sam on 1/12/16.
 */
public final class QueryResponseTransformer implements Transformer<QueryResponse, SerializedQueryResponse> {
  @Override
  public QueryResponse transformFrom(SerializedQueryResponse serializedQueryResult) {
    throw new RuntimeException("method `transformFrom` of `QueryResponseTransformer` is not implemented");
  }

  @Override
  public SerializedQueryResponse transformTo(QueryResponse query) {
    return query.execute();
  }
}
