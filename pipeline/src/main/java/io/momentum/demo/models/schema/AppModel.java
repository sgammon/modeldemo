package io.momentum.demo.models.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.server.spi.config.ApiTransformer;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.appengine.api.datastore.PropertyContainer;
import com.google.cloud.dataflow.sdk.coders.AvroCoder;
import com.google.cloud.dataflow.sdk.coders.DefaultCoder;
import com.googlecode.objectify.*;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;

import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.KeyMetadata;
import io.protostuff.Tag;

import com.googlecode.objectify.annotation.Index;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.avro.Schema;
import org.apache.commons.beanutils.BeanUtils;

import io.momentum.demo.models.logic.runtime.datastore.DatastoreService;
import io.momentum.demo.models.logic.service.models.FlatModel;
import io.momentum.demo.models.logic.service.models.SerializedModel;
import io.momentum.demo.models.logic.service.transformers.ModelTransformer;
import io.momentum.demo.models.pipeline.PlatformCodec;
import io.momentum.demo.models.pipeline.coder.ModelCoder;
import io.momentum.demo.models.pipeline.coder.TypedSerializedModel;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;


/**
 * Created by sam on 1/12/16.
 */
@DefaultCoder(ModelCoder.class)
@JsonInclude(value = Include.ALWAYS)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public abstract class AppModel implements Serializable {
  private static final PlatformCodec codec = new PlatformCodec();
  private static final Logger logging = Logger.getLogger(AppModel.class.getSimpleName());

  private enum FieldType {
    STRING,
    INTEGER,
    FLOAT,
    BOOLEAN,
    TIMESTAMP,
    RECORD
  }

  private static class FieldSchema {
    private final FieldType type;
    private final boolean nullable;
    private final boolean repeated;
    private final List<FieldSchema> fields;

    private FieldSchema(FieldType type,
                        boolean nullable,
                        boolean repeated) {
      this.type = type;
      this.nullable = nullable;
      this.repeated = repeated;
      this.fields = null;  // a non-record type doesn't have fields
    }

    private FieldSchema(boolean nullable,
                        boolean repeated,
                        List<FieldSchema> fields) {
      this.type = FieldType.RECORD;
      this.nullable = nullable;
      this.repeated = repeated;
      this.fields = fields;
    }

    private static FieldSchema nullable(FieldType type) {
      return new FieldSchema(type, true, false);
    }

    private static FieldSchema repeated(FieldType type) {
      return new FieldSchema(type, true, true);
    }

    private static FieldSchema notNullable(FieldType type) {
      return new FieldSchema(type, false, false);
    }

    private static FieldSchema record(List<FieldSchema> fields) {
      return record(fields, false);
    }

    private static FieldSchema record(List<FieldSchema> fields, boolean repeated) {
      return new FieldSchema(false, repeated, fields);
    }

    @Override
    public String toString() {
      return type.name();
    }
  }

  /** -- internals -- **/
  private @Ignore transient boolean _isExporting = false;
  private void setExporting(boolean exportMode) {
    this._isExporting = exportMode;
  }
  protected static Objectify datastore() {
    return DatastoreService.ofy();
  }
  protected static Closeable objectify() {
    return ObjectifyService.begin();
  }

  /** -- properties -- **/
  // timestamp for creation
  @Tag(value = 1, alias = "c")
  public @Index @JsonProperty("created") Date created;

  // timestamp for modification
  @Tag(value = 2, alias = "m")
  public @Index @JsonProperty("modified") Date modified;

  public @OnSave void updateTimestamps() {
    if (!_isExporting) {
      Date ts = new Date();
      this.modified = ts;
      if (this.created == null) this.created = ts;
    }
  }

  /** -- getters/setters -- **/
  public Date getCreated() {
    return created;
  }

  public Date getModified() {
    return modified;
  }

  /** -- schema & codec -- **/
  public String kind() {
    return Key.create(this.getClass(), "1").getKind();
  }

  public Map<String, Object> flatten() {
    this.setExporting(true);
    com.google.appengine.api.datastore.Entity entity = datastore().save().toEntity(this);
    this.setExporting(false);

    return entity.getProperties();
  }

  public SerializedModel serialize() {
    return new SerializedModel(this);
  }

  @SuppressWarnings("unchecked")
  public static <M extends AppModel> M deserialize(String kind, JsonNode node, TypeReference<M> ref, TypedSerializedModel<M> serialized) {
    // inflate key, attach, and deserialize
    try (Closeable dsSession = objectify()) {
      Class<M> modelClass = (Class<M>)DatastoreService.resolve(kind);
      if (modelClass == null) throw new RuntimeException("unable to resolve model class: '" + kind + "'");

      // inflate data according to kind schema
      M modelObj = codec.getReader()
                        .treeToValue(node, modelClass);

      if (serialized.getKey() != null) {
        // object has a key: inflate the key
        Key<M> targetKey = Key.create(serialized.getKey()
                                             .getEncoded());
        EntityMetadata<M> metadata = datastore().factory().getMetadata(targetKey);
        KeyMetadata<M> keyMetadata = metadata.getKeyMetadata();

        // set ID
        Class<?> idType = keyMetadata.getIdFieldType();

        try {
          if (idType.isAssignableFrom(String.class)) {
            // it's a string keyname
            BeanUtils.setProperty(modelObj, keyMetadata.getIdFieldName(), targetKey.getName());
          } else {
            // it's either a native or boxed `long`
            BeanUtils.setProperty(modelObj, keyMetadata.getIdFieldName(), targetKey.getId());
          }
        } catch (IllegalAccessException e) {
          logging.warning("Received `IllegalAccessException` when trying to carry over key ID or name: " + e.getLocalizedMessage());
        } catch (InvocationTargetException e) {
          logging.warning("Received `InvocationTargetException` when trying to carry over key ID or name: " + e.getLocalizedMessage());
        }
      }
      return modelObj;
    } catch (JsonProcessingException e) {
      logging.severe("Encountered JsonProcessingException when deserializing model of class '" + kind + "': " + e.getLocalizedMessage());
      throw new RuntimeException(e);
    } catch (ClassCastException e) {
      logging.severe("Encountered ClassCastException when deserializing model of class '" + kind + "': " + e.getLocalizedMessage());
      throw new RuntimeException(e);
    } catch (IOException e) {
      logging.severe("Encountered IOException when deserializing model of class '" + kind + "': " + e.getLocalizedMessage());
      throw new RuntimeException(e);
    }
  }

  public TableRow export() {
    throw new RuntimeException("table rows cannot yet be autogenerated");
  }

  public static FieldSchema resolveTypeForTableRow(Schema schema) {
    switch (schema.getType()) {
      case STRING: return FieldSchema.notNullable(FieldType.STRING);
      case INT: return FieldSchema.notNullable(FieldType.INTEGER);
      case LONG: return FieldSchema.notNullable(FieldType.INTEGER);
      case FLOAT: return FieldSchema.notNullable(FieldType.FLOAT);
      case BOOLEAN: return FieldSchema.notNullable(FieldType.BOOLEAN);
      case ARRAY:
        // extract inner type
        Schema elementSchema = schema.getElementType();
        FieldSchema subschema = resolveTypeForTableRow(elementSchema);
        return FieldSchema.repeated(subschema.type);

      case RECORD:
        if (schema.getNamespace().equals("com.googlecode.objectify") &&
           (schema.getName().equals("Ref") || schema.getName().equals("Key"))) {
          // it's a `Ref` or a `Key` object
          return FieldSchema.notNullable(FieldType.STRING);

        } else {
          // it's a record with its own fields
          List<Schema.Field> fields = schema.getFields();
          List<FieldSchema> fieldSchemas = new ArrayList<>(fields.size());
          for (Schema.Field field : fields) {
            // add a subfield for each
            FieldSchema fieldSubschema = resolveTypeForTableRow(field.schema());
            if (fieldSubschema != null) {
              fieldSchemas.add(fieldSubschema);
            } else {
              throw new RuntimeException("unable to resolve type for table row: sub-field '" + field.name() +
                                             "' of record '" + schema.getName() + "'.");
            }
          }
          return FieldSchema.record(fieldSchemas);
        }

      case UNION:
        // it's likely nullable
        List<Schema> unioned = schema.getTypes();
        boolean foundNull = false;
        Schema innerNullableType = null;

        for (Schema innerType : unioned) {
          if (innerType.getType() == Schema.Type.NULL) {
            foundNull = true;
            continue;
          }
          innerNullableType = innerType;
          break;
        }

        if (innerNullableType == null || !foundNull) {
          throw new RuntimeException("Failed to calculate pair for UNION type");
        }
        FieldSchema unionSchema = resolveTypeForTableRow(innerNullableType);
        return FieldSchema.nullable(unionSchema.type);

    }
    throw new RuntimeException("unsupported data type for automatic table schema");
  }

  public static <T extends AppModel> TableSchema schema(Class<T> model) {
    TableSchema target = new TableSchema();
    List<TableFieldSchema> fields = new ArrayList<>();

    // add key and kind first
    TableFieldSchema kindSchema = new TableFieldSchema();
    kindSchema.setName("kind");
    kindSchema.setType("STRING");
    kindSchema.setMode("REQUIRED");
    kindSchema.setDescription("Datastore kind for original record.");
    fields.add(kindSchema);

    // sniff schema using avro
    AvroCoder<T> coder = AvroCoder.of(model);
    Schema modelSchema = coder.getSchema();
    for (Schema.Field field : modelSchema.getFields()) {
      // add to schema
      TableFieldSchema fieldSchema = new TableFieldSchema();
      fieldSchema.setName(field.name());
      if (field.doc() != null) fieldSchema.setDescription(field.doc());

      // special case: timestamps
      if (field.name().equals("created") ||
          field.name().equals("modified")) {
        fieldSchema.setType("TIMESTAMP");
        fieldSchema.setMode("REQUIRED");

        if (field.name().equals("created")) {
          fieldSchema.setDescription("Timestamp for when this record was created.");
        } else {
          fieldSchema.setDescription("Timestamp for when this record was last modified.");
        }
      } else {
        FieldSchema resolved = resolveTypeForTableRow(field.schema());
        fieldSchema.setType(resolved.toString());

        if (resolved.repeated) {
          fieldSchema.setMode("REPEATED");
        } else if (resolved.nullable) {
          fieldSchema.setMode("NULLABLE");
        } else {
          fieldSchema.setMode("REQUIRED");
        }
      }
      fields.add(fieldSchema);
    }
    target.setFields(fields);
    return target;
  }
}
