package io.momentum.demo.models.logic.service.models;

import com.googlecode.objectify.Key;

import io.momentum.demo.models.schema.AppModel;

import java.util.Map;


/**
 * Created by sam on 1/12/16.
 */
public final class SerializedModel extends SerializedDatastoreObject {
  /** -- properties -- **/
  public final SerializedKey key;
  public final Map<String, Object> data;

  /** -- constructors -- **/
  public SerializedModel(AppModel model) {
    this.data = model.flatten();

    SerializedKey subject;
    try {
      subject = SerializedKey.fromKey(Key.create(model));
    } catch (RuntimeException e) {
      subject = null;
    }
    this.key = subject;
  }

  /** -- getters / setters -- **/
  public SerializedKey getKey() {
    return key;
  }

  public Map<String, Object> getData() {
    return data;
  }
}
