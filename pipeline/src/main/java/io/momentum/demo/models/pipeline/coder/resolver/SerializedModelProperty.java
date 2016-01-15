package io.momentum.demo.models.pipeline.coder.resolver;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.googlecode.objectify.Objectify;

import io.momentum.demo.models.logic.runtime.datastore.DatastoreService;
import io.momentum.demo.models.schema.AppModel;

import java.io.IOException;


/**
 * Created by sam on 1/14/16.
 */
public final class SerializedModelProperty extends AsPropertyTypeDeserializer {
  private static final String kindProperty = "kind";

  public SerializedModelProperty(final JavaType bt,
                                 final TypeIdResolver resolver,
                                 final String propertyName,
                                 final boolean visible,
                                 final Class<?> defaultImpl) {
    super(bt, resolver, propertyName, visible, defaultImpl);
  }

  public SerializedModelProperty(final AsPropertyTypeDeserializer src,
                                 final BeanProperty property) {
    super(src, property);
  }

  private static Objectify datastore() {
    return DatastoreService.ofy();
  }

  @Override
  public TypeDeserializer forProperty(BeanProperty prop) {
    return (prop == _property) ? this : new SerializedModelProperty(this, prop);
  }

  @Override
  public Object deserializeTypedFromObject(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.readValueAsTree();
    Class<?> subType = findSubType(node, p, ctxt);
    JavaType type = SimpleType.construct(subType);

    JsonParser jsonParser = new TreeTraversingParser(node, p.getCodec());
    if (jsonParser.getCurrentToken() == null)
      jsonParser.nextToken();

    JsonDeserializer<Object> deser = ctxt.findContextualValueDeserializer(type, _property);
    return deser.deserialize(jsonParser, ctxt);
  }

  protected Class<?> findSubType(JsonNode node, JsonParser p, DeserializationContext ctxt) {
    datastore();
    String type = node.get(kindProperty).asText();
    Class<? extends AppModel> subModel = DatastoreService.resolve(type);
    return subModel.getClass();
  }
}
