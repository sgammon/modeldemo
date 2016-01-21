package io.momentum.demo.models.logic.servlets.services;


import com.google.api.server.spi.*;
import com.google.api.server.spi.config.*;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.datastore.ApiConfigDatastoreReader;
import com.google.api.server.spi.config.jsonwriter.JsonConfigWriter;
import com.google.api.server.spi.config.validation.ApiConfigValidator;
import com.google.api.server.spi.request.Attribute;
import com.google.api.server.spi.request.ParamReader;
import com.google.api.server.spi.request.ServletRequestParamReader;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ResultWriter;
import com.google.api.server.spi.response.ServletResponseResultWriter;
import com.google.appengine.repackaged.com.google.common.collect.UnmodifiableIterator;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.momentum.demo.models.logic.service.base.SystemAppService;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by sam on 1/21/16.
 */
public final class AppServiceServlet extends HttpServlet {
  private static final Pattern PATH_PATTERN = Pattern.compile("/([^/]+)\\.([^/]+)");
  private static final Logger logger = Logger.getLogger(SystemServiceServlet.class.getName());
  private volatile SystemAppService systemService;
  private volatile ServletInitializationParameters initParameters;

  public AppServiceServlet() {}

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    ClassLoader classLoader = this.getUserClassLoader(config);
    this.initParameters = ServletInitializationParameters.fromServletConfig(config, classLoader);
    logger.log(Level.INFO, "SPI restricted: {0}", Boolean.valueOf(this.initParameters.isServletRestricted()));
    this.systemService = this.createSystemService(classLoader, this.initParameters);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String path = request.getPathInfo();
    String[] pathParams = this.getPathParams(path);
    if (pathParams != null) {
      try {
        String e = pathParams[0];
        String methodName = pathParams[1];
        this.execute(request, response, e, methodName);
      } catch (ServiceException var7) {
        this.getErrorResponseWriter(response)
            .writeError(var7);
      }
    } else {
      this.getErrorResponseWriter(response)
          .writeError(new BadRequestException("missing /{Service}.{method}"));
    }

  }

  protected void execute(HttpServletRequest request, HttpServletResponse response, String serviceName, String methodName) throws IOException, ServiceException {
    logger.log(Level.FINE, "serviceName={0} methodName={1}", new Object[]{serviceName, methodName});
    EndpointMethod serviceMethod = this.systemService.resolveService(serviceName, methodName);
    ApiSerializationConfig serializationConfig = this.systemService.getSerializationConfig(serviceName);
    ResultWriter responseWriter = this.getResponseWriter(serializationConfig, response);
    logger.log(Level.FINE, "serviceMethod={0}", serviceMethod);
    ApiMethodConfig methodConfig = this.systemService.resolveAndUpdateServiceConfig(serviceName, methodName);
    this.bindRequestAttributes(request, methodConfig);
    //if(!PeerAuth.from(request).authorizePeer()) {
    //  logger.info("SPI restricted and request denied");
    //  this.getErrorResponseWriter(response).writeError(new NotFoundException("Not found"));
    //} else {
      ParamReader requestReader = this.getParamReader(serviceMethod, serializationConfig, request);
      this.systemService.invokeServiceMethod(this.systemService.findService(serviceName), serviceMethod.getMethod(), requestReader, responseWriter);
    //}
  }

  private ParamReader getParamReader(EndpointMethod method, ApiSerializationConfig serializationConfig, HttpServletRequest request) {
    return new ServletRequestParamReader(method, request, this.getServletContext(), serializationConfig);
  }

  private ResultWriter getResponseWriter(ApiSerializationConfig serializationConfig, HttpServletResponse response) {
    return new ServletResponseResultWriter(response, serializationConfig);
  }

  private ResultWriter getErrorResponseWriter(HttpServletResponse response) {
    return this.getResponseWriter((ApiSerializationConfig)null, response);
  }

  private String[] getPathParams(String path) {
    Matcher matcher = PATH_PATTERN.matcher(path);
    return matcher.matches() && matcher.groupCount() == 2 ? new String[]{matcher.group(1), matcher.group(2)} : null;
  }

  private SystemAppService createSystemService(ClassLoader classLoader, ServletInitializationParameters initParameters)
      throws ServletException {
    try {
      ApiConfigLoader e = createConfigLoader(classLoader);
      ApiConfigValidator validator = new ApiConfigValidator();
      JsonConfigWriter configWriter = new JsonConfigWriter(classLoader, validator);
      BackendProperties backendProperties = new BackendProperties();
      return this.createSystemService(e, validator, backendProperties.getApplicationId(), configWriter, initParameters);
    } catch (ApiConfigException var7) {
      throw new ServletException(var7);
    } catch (ClassNotFoundException var8) {
      throw new ServletException(var8);
    }
  }

  private static ApiConfigLoader createConfigLoader(ClassLoader classLoader) throws ClassNotFoundException {
    TypeLoader typeLoader = new TypeLoader(classLoader);
    ApiConfigAnnotationReader annotationReader = new ApiConfigAnnotationReader(typeLoader.getAnnotationTypes());
    if (EnvUtil.isRunningOnAppEngine()) {
      ApiConfigDatastoreReader datastoreReader = new ApiConfigDatastoreReader();
      return new ApiConfigLoader(new ApiConfig.Factory(), typeLoader, annotationReader,
                                 new ApiConfigSource[]{datastoreReader});
    } else {
      return new ApiConfigLoader(new ApiConfig.Factory(), typeLoader, annotationReader, new ApiConfigSource[0]);
    }
  }

  private SystemAppService createSystemService(ApiConfigLoader configLoader, ApiConfigValidator validator, String appName,
                                               ApiConfigWriter configWriter,
                                               ServletInitializationParameters initParameters) throws ApiConfigException {
    SystemAppService newSystemService = new SystemAppService(configLoader, validator, appName, configWriter,
                                                             initParameters.isIllegalArgumentBackendError());
    UnmodifiableIterator var7 = initParameters.getServiceClasses()
                                              .iterator();

    while (var7.hasNext()) {
      Class serviceClass = (Class) var7.next();
      this.registerService(newSystemService, serviceClass);
    }

    return newSystemService;
  }

  private <T> void registerService(SystemService newSystemService, Class<T> serviceClass) throws ApiConfigException {
    newSystemService.registerService(serviceClass, this.createService(serviceClass));
  }

  protected <T> T createService(Class<T> serviceClass) {
    try {
      return serviceClass.newInstance();
    } catch (InstantiationException var3) {
      throw new RuntimeException(String.format("Cannot instantiate service class: %s", new Object[]{serviceClass.getName()}), var3);
    } catch (IllegalAccessException var4) {
      throw new RuntimeException(String.format("Cannot access service class: %s", new Object[]{serviceClass.getName()}), var4);
    }
  }

  private ClassLoader getUserClassLoader(ServletConfig config) throws ServletException {
    try {
      Class e = Class.forName("com.google.apphosting.utils.jetty.AppEngineWebAppContext$AppEngineServletContext");
      Method method = e.getMethod("getClassLoader", new Class[0]);
      return (ClassLoader) method.invoke(config.getServletContext(), new Object[0]);
    } catch (ClassNotFoundException var4) {
      return this.getClass()
                 .getClassLoader();
    } catch (NoSuchMethodException var5) {
      throw new ServletException(var5);
    } catch (IllegalAccessException var6) {
      throw new ServletException(var6);
    } catch (InvocationTargetException var7) {
      throw new ServletException(var7);
    }
  }

  void bindRequestAttributes(HttpServletRequest request, ApiMethodConfig methodConfig) {
    Attribute attr = Attribute.from(request);
    attr.set("endpoints:Restrict-Servlet", Boolean.valueOf(this.initParameters.isServletRestricted()));
    attr.set("endpoints:Enable-Client-Id-Whitelist", Boolean.valueOf(this.initParameters.isClientIdWhitelistEnabled()));
    attr.set("endpoints:Api-Method-Config", methodConfig);
    if(this.initParameters.isClientIdWhitelistEnabled() && Strings.isEmptyOrNull(methodConfig.getClientIds())) {
      attr.set("endpoints:Skip-Token-Auth", Boolean.valueOf(true));
    }
  }
}
