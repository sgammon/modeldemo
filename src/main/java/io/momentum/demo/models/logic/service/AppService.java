package io.momentum.demo.models.logic.service;

import com.google.api.server.spi.SystemService;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.ApiConfigWriter;
import com.google.api.server.spi.config.validation.ApiConfigValidator;


/**
 * Created by sam on 1/12/16.
 */
public final class AppService extends SystemService {
  public AppService(ApiConfigLoader configLoader,
                    ApiConfigValidator validator,
                    String appName,
                    ApiConfigWriter configWriter,
                    Object[] services,
                    boolean isIllegalArgumentBackendError) throws ApiConfigException {
    // call parent
    super(configLoader, validator, appName, configWriter, isIllegalArgumentBackendError);

    for (Object service : services) {
      this.registerService(service);
    }
  }
}
