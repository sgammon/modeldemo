package io.momentum.demo.models.pipeline.coder;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonTypeResolver;

import io.momentum.demo.models.logic.service.models.SerializedKey;
import io.momentum.demo.models.pipeline.coder.resolver.SerializedModelPropertyResolver;
import io.momentum.demo.models.schema.AppModel;


/**
 * Created by sam on 1/14/16.
 */
@JsonTypeResolver(SerializedModelPropertyResolver.class)
public final class TypedSerializedModel<M extends AppModel> {
  private final @JsonProperty("kind") String kind;
  private final @JsonProperty("key") SerializedKey key;
  private @JsonProperty("data") M data;

  @JsonCreator
  public TypedSerializedModel(@JsonProperty("kind") String kind,
                              @JsonProperty("key") SerializedKey key,
                              @JsonProperty("data") JsonNode data) {
    this.kind = kind;
    this.key = key;
    this.data = AppModel.deserialize(kind, data, new TypeReference<M>() { }, this);
  }

  /** -- getters -- **/
  public String getKind() {
    return kind;
  }

  public SerializedKey getKey() {
    return key;
  }

  public M getData() {
    return data;
  }
}
