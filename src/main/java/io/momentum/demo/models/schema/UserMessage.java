package io.momentum.demo.models.schema;


import com.google.cloud.dataflow.sdk.coders.AvroCoder;
import com.google.cloud.dataflow.sdk.coders.DefaultCoder;
import com.googlecode.objectify.annotation.*;
import io.protostuff.Tag;
import org.apache.avro.reflect.Nullable;


/**
 * Created by sam on 1/12/16.
 */
@Entity
@Cache
@DefaultCoder(AvroCoder.class)
public final class UserMessage extends AppModel {
  /** -- properties -- **/
  @Tag(value = 10, alias = "i")
  public @Id Long id;

  // user's full name
  @Tag(value = 20, alias = "n")
  public @Index String name;

  // message left by user
  @Tag(value = 30, alias = "m")
  public @Index String message;

  // email address, if logged in
  @Tag(value = 40, alias = "e")
  public @Index @Nullable String email;

  /** -- constructors -- **/
  public UserMessage() {}

  public UserMessage(String name,
                     String message) {
    this.name = name;
    this.message = message;
    this.email = null;
  }

  public UserMessage(String name,
                     String message,
                     String email) {
    this.name = name;
    this.message = message;
    this.email = email;
  }

  /** -- getters & setters -- **/
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
