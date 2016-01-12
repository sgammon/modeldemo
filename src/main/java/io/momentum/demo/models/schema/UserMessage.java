package io.momentum.demo.models.schema;


import com.googlecode.objectify.annotation.*;


/**
 * Created by sam on 1/12/16.
 */
@Entity
@Cache
public class UserMessage {
  /** -- properties -- **/
  public @Id Long id;
  public @Index String name;
  public @Index String message;

  /** -- constructors -- **/
  public UserMessage() {}

  public UserMessage(String name,
                     String message) {
    this.name = name;
    this.message = message;
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
