package io.momentum.demo.models.logic.service.models;


import com.googlecode.objectify.Key;


/**
 * Created by sam on 1/12/16.
 */
public final class SerializedKey extends SerializedDatastoreObject {
  /** -- properties -- **/
  public final String kind;
  public final String encoded;

  /** -- constructors -- **/
  private SerializedKey(String kind, String encoded) {
    this.kind = kind;
    this.encoded = encoded;
  }

  /** -- factories -- **/
  public Key toKey() {
    if (encoded != null) return Key.create(encoded);
    return null;
  }

  public static SerializedKey fromKey(Key key) {
    return new SerializedKey(key.getKind(), key.toWebSafeString());
  }

  /** -- getters / setters -- **/
  public String getKind() {
    return kind;
  }

  public String getEncoded() {
    return encoded;
  }
}
