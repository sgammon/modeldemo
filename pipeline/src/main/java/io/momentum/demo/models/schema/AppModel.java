package io.momentum.demo.models.schema;

import com.google.api.server.spi.config.ApiTransformer;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.appengine.api.datastore.*;
import com.google.cloud.dataflow.sdk.coders.AvroCoder;
import com.google.cloud.dataflow.sdk.coders.DefaultCoder;
import com.googlecode.objectify.*;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;

import com.googlecode.objectify.annotation.Index;
import io.protostuff.Tag;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.avro.Schema;

import io.momentum.demo.models.logic.runtime.datastore.DatastoreService;
import io.momentum.demo.models.logic.service.models.SerializedModel;
import io.momentum.demo.models.logic.service.transformers.ModelTransformer;
import io.momentum.demo.models.pipeline.coder.ModelCoder;

import java.util.*;
import java.util.logging.Logger;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import static com.fasterxml.jackson.annotation.JsonInclude.Include;


/**
 * Created by sam on 1/12/16.
 */
@DefaultCoder(ModelCoder.class)
@ApiTransformer(ModelTransformer.class)
@JsonInclude(value = Include.ALWAYS)
@JsonTypeInfo(use = Id.MINIMAL_CLASS, include = As.PROPERTY, property = "kind")
public abstract class AppModel {
  private static final Logger logging = Logger.getLogger(AppModel.class.getSimpleName());

  private enum FieldType {
    STRING,
    NUMBER,
    FLOAT,
    BOOLEAN,
    TIMESTAMP,
    RECORD
  }

  private static class FieldSchema {
    private final FieldType type;
    private final boolean nullable;
    private final boolean repeated;

    private FieldSchema(FieldType type,
                        boolean nullable,
                        boolean repeated) {
      this.type = type;
      this.nullable = nullable;
      this.repeated = repeated;
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
  }

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
  public Map<String, Object> flatten(boolean removeNulls) {
    Map<String, Object> obj = datastore().save().toEntity(this).getProperties();
    if (removeNulls) {
      Map<String, Object> finalObjs = new HashMap<>();
      for (Map.Entry<String, Object> entry : obj.entrySet()) {
        if (entry.getValue() != null) {
          finalObjs.put(entry.getKey(), entry.getValue());
        }
      }
      return finalObjs;
    }
    return obj;
  }

  public SerializedModel serialize() {
    return new SerializedModel(this);
  }

  public SerializedModel serialize(boolean removeNulls) {
    return new SerializedModel(this, removeNulls);
  }

  public static <M extends AppModel> M deserialize(SerializedModel serialized) {
    Key targetKey = Key.create(serialized.key.encoded);
    com.google.appengine.api.datastore.Entity lowlevelEntity = new com.google.appengine.api.datastore.Entity(targetKey.getRaw());

    if (serialized.data.size() > 0) {
      for (Map.Entry<String, Object> entry : serialized.data.entrySet()) {
        lowlevelEntity.setProperty(entry.getKey(), entry.getValue());
      }
    }

    try {
      return datastore().load().fromEntity(lowlevelEntity);
    } catch (ClassCastException e) {
      logging.severe("Unable to cast model during deserialization: " + e.getLocalizedMessage());
      throw new RuntimeException("Unable to cast model during deserialization: " + e.getLocalizedMessage());
    }
  }

  public TableRow export() {
    throw new RuntimeException("table rows cannot yet be autogenerated");
  }

  public static FieldSchema resolveTypeForTableRow(Schema schema) {
    switch (schema.getType()) {
      case STRING: return FieldSchema.notNullable(FieldType.STRING);
      case INT: return FieldSchema.notNullable(FieldType.NUMBER);
      case FLOAT: return FieldSchema.notNullable(FieldType.FLOAT);
      case BOOLEAN: return FieldSchema.notNullable(FieldType.BOOLEAN);
      case ARRAY:
        // extract inner type
        Schema elementSchema = schema.getElementType();
        FieldSchema subschema = resolveTypeForTableRow(elementSchema);
        return FieldSchema.repeated(subschema.type);

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
    TableFieldSchema keySchema = new TableFieldSchema();
    keySchema.setName("key");
    keySchema.setType("STRING");
    keySchema.setMode("REQUIRED");
    keySchema.setDescription("Datastore key for original record.");

    TableFieldSchema kindSchema = new TableFieldSchema();
    kindSchema.setName("kind");
    kindSchema.setType("STRING");
    kindSchema.setMode("REQUIRED");
    kindSchema.setDescription("Datastore kind for original record.");

    fields.add(keySchema);
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
