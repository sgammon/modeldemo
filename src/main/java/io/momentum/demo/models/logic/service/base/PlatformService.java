package io.momentum.demo.models.logic.service.base;


import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiResourceProperty;

import io.momentum.demo.models.logic.service.exceptions.ServiceError;

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
}
