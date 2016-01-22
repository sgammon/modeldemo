package io.momentum.demo.models.logic.service.base;


import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.cmd.Query;

import io.momentum.demo.models.logic.service.exceptions.ServiceError;
import io.momentum.demo.models.schema.Account;

import java.util.Arrays;
import java.util.List;


/**
 * Created by sam on 1/12/16.
 */
public abstract class PlatformService extends BaseService {
  public static class ErrorSpec {
    private final ServiceError[] errors;

    private ErrorSpec(PlatformService service) {
      errors = service.errorTypes();
    }

    @ApiResourceProperty(name = "errors")
    public final List<ServiceError> getErrors() {
      return Arrays.asList(errors);
    }
  }

  @Override
  protected ServiceError[] errorTypes() {
    ServiceError[] empty = {};
    return empty;
  }

  @ApiMethod(name = "errors",
             path = "errors")
  public final ErrorSpec getErrorSpec() {
    return new ErrorSpec(this);
  }

  protected Account accountFromUser(User user) {
    // build us a new user
    String id = user.getId();
    return datastore().load()
                      .type(Account.class)
                      .id(id)
                      .now();
  }

  protected Account accountFromEmail(String email) {
    Query<Account> accountQuery = datastore().load()
                                             .type(Account.class)
                                             .filter("email =", email);
    return accountQuery.first().now();
  }
}
