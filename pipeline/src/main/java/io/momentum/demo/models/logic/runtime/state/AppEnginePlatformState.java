package io.momentum.demo.models.logic.runtime.state;


import com.google.appengine.api.utils.SystemProperty;


/**
 * Created by sam on 1/13/16.
 */
public final class AppEnginePlatformState {
  public String getProject() {
    return System.getProperty("mm.project");
  }
  public String getContainer() {
    return System.getProperty("mm.k9.runtime.container");
  }
  public String getRuntimeSalt() {
    return System.getProperty("mm.k9.runtime.salt");
  }
  public String getContainerToken() {
    return System.getProperty("mm.k9.runtime.container.token");
  }
  public String getServiceKeyPath() {
    return System.getProperty("mm.k9.google.service.key");
  }
  public boolean getProduction() {
    return (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production);
  }
}
