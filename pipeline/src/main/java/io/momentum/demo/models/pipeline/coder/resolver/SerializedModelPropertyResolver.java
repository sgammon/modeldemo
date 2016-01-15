package io.momentum.demo.models.pipeline.coder.resolver;


import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

import java.util.Collection;


/**
 * Created by sam on 1/14/16.
 */
public final class SerializedModelPropertyResolver extends StdTypeResolverBuilder {
  @Override
  public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, JavaType baseType,
                                                Collection<NamedType> subtypes) {
    return new SerializedModelProperty(baseType, null, _typeProperty, _typeIdVisible, _defaultImpl);
  }
}
