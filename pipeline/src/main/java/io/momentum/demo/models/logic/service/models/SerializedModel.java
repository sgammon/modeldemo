package io.momentum.demo.models.logic.service.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.googlecode.objectify.Key;

import io.momentum.demo.models.schema.AppModel;

import java.io.Serializable;


/**
 * Created by sam on 1/12/16.
 */
public final class SerializedModel extends SerializedDatastoreObject implements Serializable {
  /** -- properties -- **/
  public final @JsonProperty("kind") String kind;
  public final @JsonProperty("key") SerializedKey key;
  public final @JsonProperty("data") AppModel data;

  /** -- constructors -- **/
  @JsonCreator
  public SerializedModel(@JsonProperty("kind") String kind,
                         @JsonProperty("key") SerializedKey key,
                         @JsonProperty("data") AppModel data) {
    this.kind = kind;
    this.key = key;
    this.data = data;
  }

  public SerializedModel(AppModel model) {
    this(model, false);
  }

  public SerializedModel(AppModel model, boolean removeNulls) {
    this.data = model;

    SerializedKey subject;
    String kind;
    try {
      Key target = Key.create(model);
      subject = SerializedKey.fromKey(target);
      kind = target.getKind();
    } catch (RuntimeException e) {
      subject = null;
      kind = Key.create(model.getClass(), "1").getKind();
    }
    this.kind = kind;
    this.key = subject;
  }

  /** -- getters / setters -- **/
  public SerializedKey getKey() {
    return key;
  }

  public AppModel getData() {
    return data;
  }
}
