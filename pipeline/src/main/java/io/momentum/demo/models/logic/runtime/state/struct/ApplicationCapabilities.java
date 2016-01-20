package io.momentum.demo.models.logic.runtime.state.struct;


import com.google.appengine.api.capabilities.CapabilitiesService;
import com.google.appengine.api.capabilities.CapabilitiesServiceFactory;
import com.google.appengine.api.capabilities.CapabilityState;

import static com.google.appengine.api.capabilities.Capability.*;


/**
 * Created by sam on 1/13/16.
 */
public final class ApplicationCapabilities {
  private static CapabilitiesService capabilitiesService = CapabilitiesServiceFactory.getCapabilitiesService();

  public static CapabilityState getMail() {
    return capabilitiesService.getStatus(MAIL);
  }
  public static CapabilityState getXMPP() {
    return capabilitiesService.getStatus(XMPP);
  }
  public static CapabilityState getImages() {
    return capabilitiesService.getStatus(IMAGES);
  }
  public static CapabilityState getMemcache() {
    return capabilitiesService.getStatus(MEMCACHE);
  }
  public static CapabilityState getURLFetch() {
    return capabilitiesService.getStatus(URL_FETCH);
  }
  public static CapabilityState getTaskqueue() {
    return capabilitiesService.getStatus(TASKQUEUE);
  }
  public static CapabilityState getBlobstore() {
    return capabilitiesService.getStatus(BLOBSTORE);
  }
  public static CapabilityState getDatastore() {
    return capabilitiesService.getStatus(DATASTORE);
  }
  public static CapabilityState getDatastoreWrite() {
    return capabilitiesService.getStatus(DATASTORE_WRITE);
  }
  public static CapabilityState getProspectiveSearch() {
    return capabilitiesService.getStatus(PROSPECTIVE_SEARCH);
  }
}
