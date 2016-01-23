package io.momentum.demo.models.service.accounts.v1;


import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.*;
import com.googlecode.objectify.Key;

import io.momentum.demo.models.logic.oauth.google.GoogleScopes;
import io.momentum.demo.models.logic.service.base.PlatformService;
import io.momentum.demo.models.logic.service.exceptions.BadRequest;
import io.momentum.demo.models.logic.service.exceptions.Conflict;
import io.momentum.demo.models.logic.service.exceptions.Forbidden;
import io.momentum.demo.models.logic.service.exceptions.ServiceError;
import io.momentum.demo.models.logic.service.models.QueryOptions;
import io.momentum.demo.models.logic.service.models.QueryResponse;
import io.momentum.demo.models.logic.service.models.SerializedKey;
import io.momentum.demo.models.schema.Account;


/**
 * Created by sam on 1/22/16.
 */
@Api(name = "account",
     title = "Account API",
     canonicalName = "Account API",
     description = "Sample auth API.",
     resource = "accounts",
     defaultVersion = AnnotationBoolean.TRUE,
     useDatastoreForAdditionalConfig = AnnotationBoolean.FALSE,
     authLevel = AuthLevel.REQUIRED,
     scopes = {GoogleScopes.ME, GoogleScopes.EMAIL},
     clientIds = {"292824132082.apps.googleusercontent.com"},
     namespace = @ApiNamespace(ownerName = "momentum ideas",
                               ownerDomain = "momentum.io",
                               packagePath = "platform/sample"))
public final class AccountService extends PlatformService {
  /** -- API errors -- **/
  // specify error types
  public enum AccountServiceError implements ServiceError {
    INVALID_PROFILE_DATA {
      @Override
      public Class<? extends ServiceException> exception() {
        return BadRequest.class;
      }
    },

    NOT_AUTHORIZED {
      @Override
      public Class<? extends ServiceException> exception() {
        return Forbidden.class;
      }
    },

    ACCOUNT_ALREADY_EXISTS {
      @Override
      public Class<? extends ServiceException> exception() {
        return Conflict.class;
      }
    }
  }

  // expose error types
  @Override
  protected AccountServiceError[] errorTypes() {
    return AccountServiceError.values();
  }

  /** -- internals -- **/
  private Account createAccountFromUser(User user, String firstname, String lastname) {
    Account existing = accountFromUser(user);
    if (existing != null)
      return null;  // cannot create multiple accounts

    Account newAccount = new Account(user, firstname, lastname);
    datastore().save().entity(newAccount).now();
    return newAccount;
  }

  /** -- `account.create`: create a new user account -- **/
  @ApiMethod(name = "create",
             path = "accounts",
             httpMethod = ApiMethod.HttpMethod.POST)
  public SerializedKey create(@Named("first") String firstname,
                              @Named("last") String lastname,
                              User user) throws ServiceException {
    // enforce auth
    if (user == null)
      throw this.fail(AccountServiceError.NOT_AUTHORIZED);

    // validate profile
    if (firstname == null || firstname.trim().isEmpty() || firstname.trim().length() < 2 ||
        lastname == null || lastname.trim().isEmpty() || lastname.trim().length() < 2)
      throw this.fail(AccountServiceError.INVALID_PROFILE_DATA);

    // okay, create it and return
    Account newAccount = createAccountFromUser(user, firstname, lastname);

    // if we don't get one back, there's a conflict
    if (newAccount == null)
      throw this.fail(AccountServiceError.ACCOUNT_ALREADY_EXISTS);

    return SerializedKey.fromKey(Key.create(newAccount));
  }

  /** -- API methods -- **/
  @ApiMethod(name = "list",
             path = "accounts",
             httpMethod = ApiMethod.HttpMethod.GET)
  public QueryResponse list(@Named("options") @Nullable QueryOptions options,
                            User user) throws ServiceException {
    if (user == null)
      throw this.fail(AccountServiceError.NOT_AUTHORIZED);

    // list all messages, in the order they were posted
    return this.prepare(datastore()
                            .load()
                            .type(Account.class)
                            .hybrid(true), options);
  }
}
