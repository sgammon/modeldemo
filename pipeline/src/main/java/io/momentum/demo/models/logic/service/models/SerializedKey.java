package io.momentum.demo.models.logic.service.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.googlecode.objectify.Key;

import java.io.Serializable;


/**
 * Created by sam on 1/12/16.
 */
public final class SerializedKey extends SerializedDatastoreObject implements Serializable {
  /** -- properties -- **/
  public final @JsonProperty("encoded") String encoded;

  /** -- constructors -- **/
  @JsonCreator
  public SerializedKey(@JsonProperty("encoded") String encoded) {
    this.encoded = encoded;
  }

  /** -- factories -- **/
  public Key toKey() {
    if (encoded != null) return Key.create(encoded);
    return null;
  }

  public static SerializedKey fromKey(Key key) {
    return new SerializedKey(key.toWebSafeString());
  }

  /** -- getters / setters -- **/
  @JsonIgnore
  public String getKind() {
    return Key.create(encoded).getKind();
  }

  public String getEncoded() {
    return encoded;
  }
}
