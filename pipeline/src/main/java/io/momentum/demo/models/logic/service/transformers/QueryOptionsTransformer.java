package io.momentum.demo.models.logic.service.transformers;


import com.google.api.server.spi.config.Transformer;

import io.momentum.demo.models.logic.service.models.QueryOptions;


/**
 * Created by sam on 1/12/16.
 */
public final class QueryOptionsTransformer implements Transformer<QueryOptions, String> {
  @Override
  public QueryOptions transformFrom(String s) {
    return null;
  }

  @Override
  public String transformTo(QueryOptions queryOptions) {
    return null;
  }
}
