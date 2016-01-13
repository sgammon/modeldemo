package io.momentum.demo.models.logic.service.unified.v1;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.*;
import com.googlecode.objectify.Key;

import io.momentum.demo.models.logic.service.base.PlatformService;
import io.momentum.demo.models.logic.service.exceptions.Forbidden;
import io.momentum.demo.models.logic.service.exceptions.NotFound;
import io.momentum.demo.models.logic.service.exceptions.ServiceError;
import io.momentum.demo.models.logic.service.models.EncodedKey;
import io.momentum.demo.models.logic.service.models.QueryOptions;
import io.momentum.demo.models.logic.service.models.QueryResponse;
import io.momentum.demo.models.logic.service.models.SerializedKey;
import io.momentum.demo.models.logic.service.transformers.KeyInflator;
import io.momentum.demo.models.schema.UserMessage;


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
     clientIds = {"292824132082.apps.googleusercontent.com"},
     namespace = @ApiNamespace(ownerName = "momentum ideas",
                               ownerDomain = "momentum.io",
                               packagePath = "platform/sample"))
public final class UnifiedService extends PlatformService {
  private static final boolean adminOnly = false;

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
    }
  }

  // expose error types
  @Override
  protected UnifiedServiceError[] errorTypes() {
    return UnifiedServiceError.values();
  }

  /** -- API methods -- **/
  @ApiMethod(name = "list",
             path = "messages",
             httpMethod = ApiMethod.HttpMethod.GET)
  public QueryResponse list(@Named("options") @Nullable QueryOptions options,
                            User user) {
    // list all messages, in the order they were posted
    return this.prepare(datastore()
                            .load()
                            .type(UserMessage.class)
                            .hybrid(true)
                            .order("-created"), options);
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
      messageObject = new UserMessage(name, message, user.getEmail());
    } else {
      messageObject = new UserMessage(name, message);
    }
    datastore().save().entity(messageObject).now();
    return SerializedKey.fromKey(Key.create(messageObject));
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
        throw new NullPointerException();
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
