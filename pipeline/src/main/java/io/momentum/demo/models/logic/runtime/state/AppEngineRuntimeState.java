package io.momentum.demo.models.logic.runtime.state;


import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.modules.ModulesException;
import com.google.appengine.api.modules.ModulesService;
import com.google.appengine.api.modules.ModulesServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.ApiProxy;

import io.momentum.demo.models.logic.runtime.state.struct.ApplicationInstance;
import io.momentum.demo.models.logic.runtime.state.struct.ApplicationModule;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;


/**
 * Created by sam on 1/13/16.
 */
public final class AppEngineRuntimeState {
  private static final Logger logging = Logger.getLogger(AppEngineRuntimeState.class.getName());

  /*  --  info  --  */
  private final boolean debug;
  private final String namespace;
  private final String runtimeVersion;
  private final String defaultHostname;
  private final ApplicationModule module;
  private final ApplicationInstance instance;

  /*  --  construction  --  */
  public AppEngineRuntimeState() {
    /* construct state for current module */
    Map<String, Object> proxy = ApiProxy.getCurrentEnvironment()
                                        .getAttributes();
    ModulesService modules = ModulesServiceFactory.getModulesService();
    String moduleName = modules.getCurrentModule();
    String moduleVersion = modules.getCurrentVersion();
    String instanceHost;
    String instanceId;

    /* fill out basic state */
    debug = SystemProperty.environment.value() != SystemProperty.Environment.Value.Production;
    namespace = NamespaceManager.get();
    runtimeVersion = (String) proxy.get("com.google.appengine.runtime.version");
    defaultHostname = (String) proxy.get("com.google.appengine.runtime.default_version_hostname");

    try {
      instanceId = modules.getCurrentInstanceId();
      instanceHost = modules.getInstanceHostname(moduleName, moduleVersion, instanceId);
      logging.info("Instance ID and hostname resolved as '" + instanceId + "' at host '" + instanceHost + "'.");
    } catch (ModulesException exc) {
      logging.warning("Instance ID could not be resolved (received exception '" + exc.getLocalizedMessage() + "').");
      instanceId = (String) proxy.get("com.google.appengine.instance.id");

      if (instanceId == null || instanceId.isEmpty()) {
        instanceId = UUID.randomUUID()
                         .toString();  // substitute with UUID
      }

      try {
        instanceHost = modules.getInstanceHostname(moduleName, moduleVersion, instanceId);
      } catch (ModulesException ixc) {
        instanceHost = instanceId + "." + moduleVersion + "." + moduleName + ".mm-corp.appspot.com";
      } catch (NumberFormatException nxc) {
        instanceHost = instanceId + "." + moduleVersion + "." + moduleName + ".mm-corp.appspot.com";
      }

    }

    // set module
    module = new ApplicationModule(moduleName, moduleVersion,
                                   modules.getVersionHostname(moduleName, moduleVersion));

    // set instance
    instance = new ApplicationInstance(module, instanceId, instanceHost);
  }

  public boolean getDebug() {
    return debug;
  }
  public String getNamespace() {
    return namespace;
  }
  public String getRuntimeVersion() {
    return runtimeVersion;
  }
  public String getDefaultHostname() {
    return defaultHostname;
  }
  public ApplicationModule getModule() {
    return module;
  }
  public ApplicationInstance getInstance() {
    return instance;
  }
}
