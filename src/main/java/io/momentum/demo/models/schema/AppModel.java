package io.momentum.demo.models.schema;

import com.google.api.server.spi.config.ApiTransformer;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.*;

import io.protostuff.Tag;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.momentum.demo.models.logic.data.DatastoreService;
import io.momentum.demo.models.logic.service.models.SerializedModel;
import io.momentum.demo.models.logic.service.transformers.ModelTransformer;

import java.util.Date;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import static com.fasterxml.jackson.annotation.JsonInclude.Include;


/**
 * Created by sam on 1/12/16.
 */
@ApiTransformer(ModelTransformer.class)
@JsonInclude(value = Include.ALWAYS)
@JsonTypeInfo(use = Id.MINIMAL_CLASS, include = As.PROPERTY, property = "kind")
public abstract class AppModel {
  /** -- internals -- **/
  protected static Objectify datastore() {
    return DatastoreService.ofy();
  }

  /** -- properties -- **/
  // timestamp for creation
  @Tag(value = 1, alias = "c")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  public @Index Date created;

  // timestamp for modification
  @Tag(value = 2, alias = "m")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  public @Index Date modified;

  /** -- lifecycle -- **/
  public @OnSave void updateTimestamps() {
    Date ts = new Date();
    this.modified = ts;
    if (this.created == null) this.created = ts;
  }

  /** -- getters/setters -- **/
  public Date getCreated() {
    return created;
  }

  public Date getModified() {
    return modified;
  }

  /** -- schema & codec -- **/
  public Map<String, Object> flatten() {
    return datastore().save().toEntity(this).getProperties();
  }

  public SerializedModel serialize() {
    return new SerializedModel(this);
  }

  public static AppModel deserialize(SerializedModel model) {
    throw new RuntimeException("Deserializing models is not yet supported.");
  }
}
