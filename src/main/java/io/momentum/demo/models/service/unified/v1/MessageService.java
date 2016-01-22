package io.momentum.demo.models.service.unified.v1;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.*;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;

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
@Api(name = "message",
     title = "Message API",
     canonicalName = "Message API",
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
public final class MessageService extends PlatformService {
  private static final boolean adminOnly = false;
  private static final String messageTopic = "apidemo.messages.input";

  /** -- API errors -- **/
  // specify error types
  public enum MessageServiceError implements ServiceError {
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
    },

    MESSAGE_DATA_INVALID {
      @Override
      public Class<? extends ServiceException> exception() {
        return BadRequest.class;
      }
    }
  }

  // expose error types
  @Override
  protected MessageServiceError[] errorTypes() {
    return MessageServiceError.values();
  }

  /** -- internals -- **/
  private SerializedKey publish(UserMessage message) {
    try {
      this.platform.pubsub.relay(messageTopic, message);
    } catch (IOException e) {
      logging.warning("Failed to publish UserMessage to pubsub topic: " + e.getLocalizedMessage());
    }
    return SerializedKey.fromKey(Key.create(message));
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
        throw this.fail(MessageServiceError.INVALID_EMAIL);

      // fetch account email references
      Account subject = accountFromEmail(userEmail);
      if (subject == null)
        throw this.fail(MessageServiceError.EMAIL_NOT_FOUND);

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
                              User user) throws ServiceException {
    // make our model, save it, and return
    UserMessage messageObject;
    if (name == null ||
        message == null ||
        name.trim().isEmpty() ||
        message.trim().isEmpty())
      throw this.fail(MessageServiceError.MESSAGE_DATA_INVALID);

    if (user != null) {
      Account account = accountFromUser(user);
      if (account == null)
        throw this.fail(MessageServiceError.USER_NOT_AUTHORIZED);

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
    // check strings
    if (messageKey == null ||
        message == null ||
        message.trim().isEmpty())
      throw this.fail(MessageServiceError.MESSAGE_DATA_INVALID);

    // check user
    if (user == null)
      throw this.fail(MessageServiceError.USER_NOT_AUTHORIZED);

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
      throw this.fail(MessageServiceError.MESSAGE_NOT_FOUND);
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
      throw this.fail(MessageServiceError.USER_NOT_AUTHORIZED);

    // fetch by key
    UserMessage messageObj = (UserMessage)datastore().load().key(messageKey.getKey()).now();

    if (messageObj == null) throw this.fail(MessageServiceError.MESSAGE_NOT_FOUND);
    datastore().delete().entity(messageObj).now();
  }
}
