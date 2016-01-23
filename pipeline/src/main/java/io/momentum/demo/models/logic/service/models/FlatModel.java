package io.momentum.demo.models.logic.service.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Map;


/**
 * Created by sam on 1/22/16.
 */
public final class FlatModel extends SerializedDatastoreObject implements Serializable {
  /** -- properties -- **/
  public final @JsonProperty("kind") String kind;
  public final @JsonProperty("key") SerializedKey key;
  public final @JsonProperty("data") Map<String, Object> data;

  /** -- constructors -- **/
  @JsonCreator
  public FlatModel(@JsonProperty("kind") String kind,
                   @JsonProperty("key") SerializedKey key,
                   @JsonProperty("data") Map<String, Object> data) {
    this.kind = kind;
    this.key = key;
    this.data = data;
  }

  /** -- getters / setters -- **/
  public String getKind() {
    return kind;
  }

  public SerializedKey getKey() {
    return key;
  }

  public Map<String, Object> getData() {
    return data;
  }
}
