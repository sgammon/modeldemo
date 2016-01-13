package io.momentum.demo.models.logic.runtime.state.struct;


/**
 * Created by sam on 1/13/16.
 */
public final class ApplicationModule {
  /*  --  data  --  */
  private final String module;
  private final String version;
  private final String hostname;

  /*  --  construction  --  */
  public ApplicationModule(String moduleName,
                           String versionSpec,
                           String moduleHostname) {
    module = moduleName;
    version = versionSpec;
    hostname = moduleHostname;
  }

  public String getName() {
    return module;
  }
  public String getVersion() {
    return version;
  }
  public String getHostname() {
    return hostname;
  }
}
