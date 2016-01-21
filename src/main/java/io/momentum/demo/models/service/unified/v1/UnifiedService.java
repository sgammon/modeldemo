package io.momentum.demo.models.service.unified.v1;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.*;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.Query;

import io.momentum.demo.models.logic.oauth.google.GoogleScopes;
import io.momentum.demo.models.logic.service.base.PlatformService;
import io.momentum.demo.models.logic.service.exceptions.BadRequest;
import io.momentum.demo.models.logic.service.exceptions.Forbidden;
import io.momentum.demo.models.logic.service.exceptions.NotFound;
import io.momentum.demo.models.logic.service.exceptions.ServiceError;
import io.momentum.demo.models.logic.service.models.EncodedKey;
import io.momentum.demo.models.logic.service.models.QueryOptions;
import io.momentum.demo.models.logic.service.models.QueryResponse;
import io.momentum.demo.models.logic.service.models.SerializedKey;
import io.momentum.demo.models.logic.service.transformers.KeyInflator;
import io.momentum.demo.models.schema.Account;
import io.momentum.demo.models.schema.UserMessage;

import java.io.IOException;


/**
 * Created by sam on 1/12/16.
 */
@Api(name = "unified",
     title = "Unified API",
     canonicalName = "Unified API",
     description = "Sample combined API.",
     resource = "messages",
     defaultVersion = AnnotationBoolean.TRUE,
     useDatastoreForAdditionalConfig = AnnotationBoolean.FALSE,
     authLevel = AuthLevel.OPTIONAL,
     scopes = {GoogleScopes.ME, GoogleScopes.EMAIL},
     clientIds = {"292824132082.apps.googleusercontent.com"},
     namespace = @ApiNamespace(ownerName = "momentum ideas",
                               ownerDomain = "momentum.io",
                               packagePath = "platform/sample"))
public final class UnifiedService extends PlatformService {
  private static final boolean adminOnly = false;
  private static final String messageTopic = "apidemo.messages.input";

  /** -- API errors -- **/
  // specify error types
  public enum UnifiedServiceError implements ServiceError {
    MESSAGE_NOT_FOUND {
      @Override
      public Class<? extends ServiceException> exception() {
        return NotFound.class;
      }
    },

    USER_NOT_AUTHORIZED {
      @Override
      public Class<? extends ServiceException> exception() {
        return Forbidden.class;
      }
    },

    INVALID_EMAIL {
      @Override
      public Class<? extends ServiceException> exception() {
        return BadRequest.class;
      }
    },

    EMAIL_NOT_FOUND {
      @Override
      public Class<? extends ServiceException> exception() {
        return NotFound.class;
      }
    }
  }

  // expose error types
  @Override
  protected UnifiedServiceError[] errorTypes() {
    return UnifiedServiceError.values();
  }

  /** -- internals -- **/
  private SerializedKey publish(UserMessage message) {
    try {
      this.platform.pubsub.publish(messageTopic, message);
    } catch (IOException e) {
      logging.warning("Failed to publish UserMessage to pubsub topic: " + e.getLocalizedMessage());
    }
    return SerializedKey.fromKey(Key.create(message));
  }

  private Account accountFromUser(User user) {
    // build us a new user
    String id = user.getId();
    Account existing = datastore().load()
                                  .type(Account.class)
                                  .id(id)
                                  .now();

    if (existing == null) {
      Account newAccount = new Account(user, "John", "Doe");
      datastore().save()
                 .entity(newAccount);
      return newAccount;
    }
    return existing;
  }

  private Account accountFromEmail(String email) {
    Query<Account> accountQuery = datastore().load()
                                             .type(Account.class)
                                             .filter("email =", email);
    return accountQuery.first().now();
  }

  /** -- API methods -- **/
  @ApiMethod(name = "list",
             path = "messages",
             httpMethod = ApiMethod.HttpMethod.GET)
  public QueryResponse list(@Named("email") @Nullable String userEmail,
                            @Named("options") @Nullable QueryOptions options,
                            User user) throws ServiceException {
    if (userEmail != null) {
      // validate email address
      if (userEmail.trim().isEmpty() || !userEmail.contains("@") || !userEmail.contains("."))
        throw this.fail(UnifiedServiceError.INVALID_EMAIL);

      // fetch account email references
      Account subject = accountFromEmail(userEmail);
      if (subject == null)
        throw this.fail(UnifiedServiceError.EMAIL_NOT_FOUND);

      // return query, filtered by account
      return this.prepare(datastore()
                              .load()
                              .type(UserMessage.class)
                              .hybrid(true)
                              .filter("account =", Ref.create(subject))
                              .order("-created"), options);
    } else {
      // list all messages, in the order they were posted
      return this.prepare(datastore()
                              .load()
                              .type(UserMessage.class)
                              .hybrid(true)
                              .order("-created"), options);
    }
  }

  @ApiMethod(name = "create",
             path = "messages",
             httpMethod = ApiMethod.HttpMethod.POST)
  public SerializedKey create(@Named("name") String name,
                              @Named("message") String message,
                              User user) {
    // make our model, save it, and return
    UserMessage messageObject;

    if (user != null) {
      Account account = accountFromUser(user);
      messageObject = new UserMessage(name, message, account);
    } else {
      messageObject = new UserMessage(name, message);
    }
    datastore().save().entity(messageObject).now();
    return publish(messageObject);
  }

  @ApiMethod(name = "update",
             path = "messages/{key}",
             httpMethod = ApiMethod.HttpMethod.POST)
  public void update(@Named("key")  @ApiTransformer(KeyInflator.class) EncodedKey messageKey,
                     @Named("message") String message,
                     User user) throws ServiceException {
    // fetch by key
    UserMessage messageObj;
    try {
      messageObj = (UserMessage)datastore()
                                     .load()
                                     .key(messageKey.getKey())
                                     .now();
      if (messageObj == null) {
        throw new NullPointerException();  // caught below for 404
      }

    } catch (RuntimeException e) {
      throw this.fail(UnifiedServiceError.MESSAGE_NOT_FOUND);
    }

    messageObj.message = message;
    datastore().save().entity(messageObj);
  }

  @ApiMethod(name = "delete",
             path = "messages/{key}",
             authLevel = AuthLevel.REQUIRED,
             httpMethod = ApiMethod.HttpMethod.DELETE)
  public void delete(@Named("key") @ApiTransformer(KeyInflator.class) EncodedKey messageKey,
                     User user) throws ServiceException {
    // user must be from momentum to delete items
    if (adminOnly && (user == null || !user.getEmail().endsWith("momentum.io")))
      throw this.fail(UnifiedServiceError.USER_NOT_AUTHORIZED);

    // fetch by key
    UserMessage messageObj = (UserMessage)datastore().load().key(messageKey.getKey()).now();

    if (messageObj == null) throw this.fail(UnifiedServiceError.MESSAGE_NOT_FOUND);
    datastore().delete().entity(messageObj).now();
  }
}
