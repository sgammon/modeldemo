package io.momentum.demo.models.schema;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.api.services.bigquery.model.TableRow;
import com.google.cloud.dataflow.sdk.coders.DefaultCoder;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.*;
import io.protostuff.Tag;
import org.apache.avro.reflect.Nullable;

import io.momentum.demo.models.pipeline.coder.ModelCoder;


/**
 * Created by sam on 1/12/16.
 */
@Entity
@Cache
@DefaultCoder(ModelCoder.class)
public final class UserMessage extends AppModel {
  /** -- properties -- **/
  @Tag(value = 10, alias = "i")
  public @Id @JsonProperty("id") Long id;

  // user's full name
  @Tag(value = 20, alias = "n")
  public @Index @JsonProperty("name") String name;

  // message left by user
  @Tag(value = 30, alias = "m")
  public @Index @JsonProperty("message") String message;

  // email address, if logged in
  @Tag(value = 40, alias = "a")
  public @Index @Nullable @JsonProperty("account") Ref<Account> account;

  /** -- constructors -- **/
  public UserMessage() {}

  public UserMessage(String name,
                     String message) {
    this.name = name;
    this.message = message;
    this.account = null;
  }

  public UserMessage(String name,
                     String message,
                     Account account) {
    this.name = name;
    this.message = message;
    this.account = Ref.create(account);
  }

  @JsonCreator
  public UserMessage(@JsonProperty("name") String name,
                     @JsonProperty("message") String message,
                     @JsonProperty("account") Ref<Account> account) {
    this.name = name;
    this.message = message;
    this.account = account;
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

  public Ref<Account> getAccount() {
    return account;
  }

  public void setAccount(Ref<Account> account) {
    this.account = account;
  }

  /** -- table rows -- **/
  @Override
  public TableRow export() {
    TableRow row = new TableRow();
    row.set("name", name);
    row.set("message", message);
    row.set("modified", modified.getTime());
    row.set("created", created.getTime());

    if (account != null)
      row.set("account", Key.create(this).toWebSafeString());
    else
      row.set("account", null);

    return row;
  }
}
