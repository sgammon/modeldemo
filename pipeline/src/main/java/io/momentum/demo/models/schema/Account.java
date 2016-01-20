package io.momentum.demo.models.schema;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.server.spi.auth.common.User;
import com.google.api.services.bigquery.model.TableRow;
import com.google.cloud.dataflow.sdk.coders.DefaultCoder;
import com.googlecode.objectify.annotation.*;
import io.protostuff.Tag;

import io.momentum.demo.models.pipeline.coder.ModelCoder;

import java.io.Serializable;


/**
 * Created by sam on 1/20/16.
 */
@Entity
@Cache
@DefaultCoder(ModelCoder.class)
public final class Account extends AppModel {
  /** -- properties -- **/
  @Tag(value = 10, alias = "i")
  public @Id @JsonProperty("id") String id;

  @Tag(value = 11, alias = "n")
  public @Index PersonName name;

  @Tag(value = 12, alias = "e")
  public @Index @JsonProperty("email") String email;

  /** -- constructors -- **/
  public Account() {}

  public Account(User user,
                 String first,
                 String last) {
    this.id = user.getId();
    this.email = user.getEmail();
    this.name = new PersonName(first, last);
  }

  @JsonCreator
  public Account(@JsonProperty("id") String id,
                 @JsonProperty("name") PersonName name,
                 @JsonProperty("email") String email) {
    this.id = id;
    this.email = email;
    this.name = name;
  }

  /** -- getters & setters -- **/

  /** -- table rows -- **/
  @Override
  public TableRow export() {
    TableRow row = new TableRow();
    return row;
  }

  /** -- subclasses -- **/
  public static class PersonName implements Serializable {
    public @JsonProperty("first") String first;
    public @JsonProperty("last") String last;

    public PersonName() {}

    public PersonName(String first,
                      String last) {
      this.first = first;
      this.last = last;
    }

    public String getFirst() {
      return first;
    }

    public void setFirst(String first) {
      this.first = first;
    }

    public String getLast() {
      return last;
    }

    public void setLast(String last) {
      this.last = last;
    }
  }
}
