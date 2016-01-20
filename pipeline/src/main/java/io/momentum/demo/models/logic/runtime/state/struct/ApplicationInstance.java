package io.momentum.demo.models.logic.runtime.state.struct;


/**
 * Created by sam on 1/13/16.
 */
public final class ApplicationInstance {
  /*  --  data  --  */
  private final String id;
  private final String hostname;
  private final ApplicationModule module;

  /*  --  construction  --  */
  public ApplicationInstance(ApplicationModule ownerModule,
                             String instanceId,
                             String instanceHostname) {
    module = ownerModule;
    id = instanceId;
    hostname = instanceHostname;
  }

  public String getId() {
    return id;
  }
  public String getHostname() {
    return hostname;
  }
  public ApplicationModule getModule() {
    return module;
  }
}
