package io.momentum.demo.models.logic.service.models;


import java.util.List;


/**
 * Created by sam on 1/12/16.
 */
public final class SerializedQueryResponse {
  public final int count;
  public final List<SerializedDatastoreObject> items;

  public SerializedQueryResponse(List<SerializedDatastoreObject> items) {
    this.count = items.size();
    this.items = items;
  }
}
