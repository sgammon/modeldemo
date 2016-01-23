package io.momentum.demo.models.logic.service.base;



import com.google.api.client.util.DateTime;
import com.google.api.server.spi.ConfiguredObjectMapper;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiSerializationConfig;
import com.google.api.server.spi.response.ResultWriter;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.appengine.api.datastore.Blob;

import com.google.appengine.repackaged.org.codehaus.jackson.JsonGenerationException;
import com.google.appengine.repackaged.org.codehaus.jackson.JsonGenerator;
import com.google.appengine.repackaged.org.codehaus.jackson.JsonProcessingException;
import com.google.appengine.repackaged.org.codehaus.jackson.Version;
import com.google.appengine.repackaged.org.codehaus.jackson.map.JsonMappingException;
import com.google.appengine.repackaged.org.codehaus.jackson.map.JsonSerializer;
import com.google.appengine.repackaged.org.codehaus.jackson.map.ObjectWriter;
import com.google.appengine.repackaged.org.codehaus.jackson.map.SerializerProvider;
import com.google.appengine.repackaged.org.codehaus.jackson.map.module.SimpleModule;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.impl.ref.LiveRef;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;


/**
 * Created by sam on 1/22/16.
 */
public class K9ResponseResultWriter implements ResultWriter {
  private static final Set<SimpleModule> WRITER_MODULES;
  private final HttpServletResponse servletResponse;
  private final ObjectWriter objectWriter;

  public K9ResponseResultWriter(HttpServletResponse servletResponse, ApiSerializationConfig serializationConfig) {
    this.servletResponse = servletResponse;
    LinkedHashSet modules = new LinkedHashSet();
    modules.addAll(WRITER_MODULES);
    this.objectWriter = ConfiguredObjectMapper.builder()
                                              .apiSerializationConfig(serializationConfig)
                                              .addRegisteredModules(modules)
                                              .build()
                                              .writer();
  }

  private static boolean isCollection(Object value) {
    return value != null && (value instanceof Collection || value.getClass().isArray());
  }

  public Object wrapCollection(Object value) {
    if(isCollection(value)) {
      HashMap<String, Object> wrapped = new HashMap<>();
      wrapped.put("items", value);
      return wrapped;
    } else {
      return value;
    }
  }

  public void write(Object response) throws IOException {
    if(response == null) {
      this.write(204, null, null);
    } else {
      this.write(200, null, writeValueAsString(this.objectWriter, wrapCollection(response)));
    }
  }

  public void writeError(ServiceException e) throws IOException {
    HashMap<String, Object> errors = new HashMap<>();
    errors.put("error_message", e.getMessage());
    this.write(e.getStatusCode(), e.getHeaders(), writeValueAsString(this.objectWriter, errors));
  }

  private void write(int status, Map<String, String> headers, String content) throws IOException {
    this.servletResponse.setStatus(status);
    if(headers != null) {
      Iterator var4 = headers.entrySet().iterator();

      while(var4.hasNext()) {
        Map.Entry entry = (Map.Entry)var4.next();
        this.servletResponse.addHeader((String)entry.getKey(), (String)entry.getValue());
      }
    }

    if(content != null) {
      this.servletResponse.setContentType("application/json; charset=UTF-8");
      this.servletResponse.setContentLength(content.getBytes("UTF-8").length);
      this.servletResponse.getWriter().write(content);
    }

  }

  private static SimpleModule getWriteObjectifyKeyAsEncodedModule() {
    JsonSerializer keySerializer = new JsonSerializer<Key>() {
      @Override
      public void serialize(Key o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
          throws IOException, JsonProcessingException {
        // write key as encoded or otherwise it's null
        if (o == null) jsonGenerator.writeNull();
        else jsonGenerator.writeString(o.toWebSafeString());
      }
    };

    SimpleModule writeObjectifyKeyAsStringModule = new SimpleModule("writeObjectifyKeyAsStringModule", new Version(1, 0, 0, null));
    writeObjectifyKeyAsStringModule.addSerializer(Key.class, keySerializer);
    return writeObjectifyKeyAsStringModule;
  }

  private static SimpleModule getWriteObjectifyRefAsEncodedModule() {
    JsonSerializer refSerializer = new JsonSerializer<Ref>() {
      @Override
      public void serialize(Ref o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
          throws IOException, JsonProcessingException {
        if (o == null || o.getKey() == null) jsonGenerator.writeNull();
        else jsonGenerator.writeString(o.getKey().toWebSafeString());
      }
    };

    SimpleModule writeObjectifyRefAsStringModule = new SimpleModule("writeObjectifyRefAsStringModule", new Version(1, 0, 0, null));
    writeObjectifyRefAsStringModule.addSerializer(Ref.class, refSerializer);
    writeObjectifyRefAsStringModule.addSerializer(LiveRef.class, refSerializer);
    return writeObjectifyRefAsStringModule;
  }

  private static SimpleModule getWriteLongAsStringModule() {
    JsonSerializer longSerializer = new JsonSerializer<Long>() {
      public void serialize(Long value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                JsonProcessingException {
        jgen.writeString(value.toString());
      }
    };
    SimpleModule writeLongAsStringModule = new SimpleModule("writeLongAsStringModule", new Version(1, 0, 0, null));
    writeLongAsStringModule.addSerializer(Long.TYPE, longSerializer);
    writeLongAsStringModule.addSerializer(Long.class, longSerializer);
    return writeLongAsStringModule;
  }

  private static SimpleModule getWriteDateAndTimeAsStringModule() {
    JsonSerializer dateAndTimeSerializer = new JsonSerializer<DateAndTime>() {
      public void serialize(DateAndTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeString(value.toRfc3339String());
      }
    };
    SimpleModule writeDateAsStringModule = new SimpleModule("writeDateAsStringModule", new Version(1, 0, 0, (String)null));
    writeDateAsStringModule.addSerializer(DateAndTime.class, dateAndTimeSerializer);
    return writeDateAsStringModule;
  }

  private static SimpleModule getWriteSimpleDateAsStringModule() {
    JsonSerializer simpleDateSerializer = new JsonSerializer<SimpleDate>() {
      @Override
      public void serialize(SimpleDate value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeString(String.format("%04d-%02d-%02d", new Object[]{Integer.valueOf(value.getYear()), Integer.valueOf(value.getMonth()), Integer.valueOf(value.getDay())}));
      }
    };
    SimpleModule writeSimpleDateAsModule = new SimpleModule("writeSimpleDateAsModule", new Version(1, 0, 0, (String)null));
    writeSimpleDateAsModule.addSerializer(SimpleDate.class, simpleDateSerializer);
    return writeSimpleDateAsModule;
  }

  private static SimpleModule getWriteDateAsStringModule() {
    JsonSerializer dateSerializer = new JsonSerializer<Date>() {
      public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeString((new DateTime(value)).toStringRfc3339());
      }
    };
    SimpleModule writeDateAsStringModule = new SimpleModule("writeDateAsStringModule", new Version(1, 0, 0, (String)null));
    writeDateAsStringModule.addSerializer(Date.class, dateSerializer);
    return writeDateAsStringModule;
  }

  private static SimpleModule getWriteBlobAsBase64Module() {
    JsonSerializer dateSerializer = new JsonSerializer<Blob>() {
      public void serialize(Blob value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        byte[] bytes = value.getBytes();
        jgen.writeBinary(bytes, 0, bytes.length);
      }
    };
    SimpleModule writeBlobAsBase64Module = new SimpleModule("writeBlobAsBase64Module", new Version(1, 0, 0, (String)null));
    writeBlobAsBase64Module.addSerializer(Blob.class, dateSerializer);
    return writeBlobAsBase64Module;
  }

  private static String writeValueAsString(ObjectWriter objectMapper, Object value) throws IOException {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonGenerationException var3) {
      throw new IOException(var3);
    } catch (JsonMappingException var4) {
      throw new IOException(var4);
    }
  }

  static {
    LinkedHashSet<SimpleModule> modules = new LinkedHashSet<>();

    // standard modules first
    modules.add(getWriteLongAsStringModule());
    modules.add(getWriteDateAsStringModule());
    modules.add(getWriteDateAndTimeAsStringModule());
    modules.add(getWriteSimpleDateAsStringModule());
    modules.add(getWriteBlobAsBase64Module());

    // then K9 custom modules
    modules.add(getWriteObjectifyKeyAsEncodedModule());
    modules.add(getWriteObjectifyRefAsEncodedModule());
    WRITER_MODULES = Collections.unmodifiableSet(modules);
  }
}
