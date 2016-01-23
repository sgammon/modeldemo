package io.momentum.demo.models.logic.service.base;

import com.google.api.server.spi.BackendService;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.SystemService;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.ApiConfigWriter;
import com.google.api.server.spi.config.validation.ApiConfigValidator;
import com.google.api.server.spi.request.ParamReader;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.ResultWriter;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.oauth.OAuthRequestException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by sam on 1/21/16.
 */
public final class SystemAppService extends SystemService {
  private final boolean isIllegalArgumentBackendError;
  protected static final Logger logger = Logger.getLogger(SystemAppService.class.getSimpleName());

  public SystemAppService(ApiConfigLoader configLoader, ApiConfigValidator validator, String appName, ApiConfigWriter configWriter, Object[] services, boolean isIllegalArgumentBackendError) throws ApiConfigException {
    super(configLoader, validator, appName, configWriter, isIllegalArgumentBackendError);
    this.isIllegalArgumentBackendError = isIllegalArgumentBackendError;
  }

  public SystemAppService(ApiConfigLoader configLoader, ApiConfigValidator validator, String appName, ApiConfigWriter configWriter, boolean isIllegalArgumentBackendError) throws ApiConfigException {
    super(configLoader, validator, appName, configWriter, isIllegalArgumentBackendError);
    this.isIllegalArgumentBackendError = isIllegalArgumentBackendError;
  }

  public void invokeServiceMethod(Object service, Method method, ParamReader paramReader, ResultWriter resultWriter) throws IOException {
    Object[] params;
    try {
      params = paramReader.read();
      logger.log(Level.FINE, "params={0} (String)", Arrays.toString(params));
    } catch (BadRequestException var10) {
      resultWriter.writeError(var10);
      return;
    }

    Object response;
    try {
      response = method.invoke(service, params);
    } catch (IllegalArgumentException var11) {
      logger.log(Level.SEVERE, "exception occurred while calling backend method", var11);
      resultWriter.writeError(new BadRequestException(var11));
      return;
    } catch (IllegalAccessException var12) {
      logger.log(Level.SEVERE, "exception occurred while calling backend method", var12);
      resultWriter.writeError(new BadRequestException(var12));
      return;
    } catch (InvocationTargetException var13) {
      Throwable cause = var13.getCause();
      Level level = Level.INFO;
      if(cause instanceof ServiceException) {
        resultWriter.writeError((ServiceException)cause);
      } else if(cause instanceof IllegalArgumentException) {
        if(this.isIllegalArgumentBackendError || BackendService.class.equals(method.getDeclaringClass())) {
          level = Level.SEVERE;
        }

        resultWriter.writeError((ServiceException)(this.isIllegalArgumentBackendError?new InternalServerErrorException(cause):new BadRequestException(cause)));
      } else if(cause instanceof OAuthRequestException) {
        resultWriter.writeError(new UnauthorizedException(cause));
      } else if(cause.getCause() != null && cause.getCause() instanceof ServiceException) {
        cause = cause.getCause();
        resultWriter.writeError((ServiceException)cause);
      } else {
        level = Level.SEVERE;
        resultWriter.writeError(new InternalServerErrorException(cause));
      }

      logger.log(level, "exception occurred while calling backend method", cause);
      return;
    }

    resultWriter.write(response);
  }
}
