package io.momentum.demo.models.logic.servlets;


import com.google.appengine.api.appidentity.PublicCertificate;
import com.google.appengine.api.capabilities.CapabilityState;
import com.google.appengine.api.capabilities.CapabilityStatus;
import com.google.apphosting.api.ApiProxy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.momentum.demo.models.AppCredentials;
import io.momentum.demo.models.logic.runtime.state.AppEngineRuntimeState;
import io.momentum.demo.models.logic.runtime.state.struct.ApplicationCapabilities;
import io.momentum.demo.models.logic.runtime.state.struct.ApplicationIdentity;

import java.io.IOException;
import java.util.Map;


/**
 * Created by sam on 1/13/16.
 */
public final class WarmupServlet extends AppServlet {
  @Override
  public void doGet(HttpServletRequest request,
                    HttpServletResponse response)
      throws IOException {
    warmup(request, response);
  }

  @Override
  public void doPost(HttpServletRequest request,
                     HttpServletResponse response)
      throws IOException {
    warmup(request, response);
  }

  public void warmDatastore() {
    // warmup datastore
    datastore();
  }

  public void warmup(HttpServletRequest request,
                     HttpServletResponse response)
      throws IOException {
    warmDatastore();
    AppEngineRuntimeState state = new AppEngineRuntimeState();

    /* compute */
    String cert_list = "";
    Map<String, PublicCertificate> certs = ApplicationIdentity.getPublicCertificates();
    for (String certname : certs.keySet()) {
      cert_list += "\n - " + certname + ": length=" + certs.get(certname)
                                                           .getX509CertificateInPemFormat()
                                                           .length();
    }

    /* report */
    logging.info("Warming up instance with `StartupServlet`..." +
                     "\n\nModule:" +
                     "\n - Name: " + state.getModule()
                                          .getName() +
                     "\n - Version: " + state.getModule()
                                             .getVersion() +
                     "\n - Namespace: " + state.getNamespace() +
                     "\n\nInstance:" +
                     "\n - ID: " + state.getInstance()
                                        .getId() +
                     "\n - Host: " + state.getInstance()
                                          .getHostname() +
                     "\n\nCredentials:" +
                     "\n - Web ID: " + AppCredentials.Web.clientId +
                     "\n - Service ID: " + AppCredentials.Service.clientId +
                     "\n - Service Email: " + AppCredentials.Service.clientEmail +
                     "\n\nCertificates:" + cert_list);

    try {
      logging.info("\n\nCapabilities:" +
                       "\n - Mail API: " + formatCapability(ApplicationCapabilities.getMail()) +
                       "\n - XMPP API: " + formatCapability(ApplicationCapabilities.getXMPP()) +
                       "\n - Images API: " + formatCapability(ApplicationCapabilities.getImages()) +
                       "\n - Matcher API: " + formatCapability(ApplicationCapabilities.getProspectiveSearch()) +
                       "\n - Memcache API: " + formatCapability(ApplicationCapabilities.getMemcache()) +
                       "\n - URLFetch API: " + formatCapability(ApplicationCapabilities.getURLFetch()) +
                       "\n - Taskqueue API: " + formatCapability(ApplicationCapabilities.getTaskqueue()) +
                       "\n - Blobstore API: " + formatCapability(ApplicationCapabilities.getBlobstore()) +
                       "\n - Datastore API: " +
                       "\n   - Reads: " + formatCapability(ApplicationCapabilities.getDatastore()) +
                       "\n   - Writes: " + formatCapability(ApplicationCapabilities.getDatastoreWrite()));
    } catch (ApiProxy.FeatureNotEnabledException exc) {
      logging.fine("Capabilities service not enabled for managed VMs.");
    }

    response.setContentType("text/plain");
    response.getWriter()
            .println("OK");
  }

  private String formatCapability(CapabilityState state) {
    CapabilityStatus status = state.getStatus();
    switch (status) {
      case ENABLED: {
        return "ACTIVE";
      }
      case DISABLED: {
        return "INACTIVE";
      }
      case SCHEDULED_MAINTENANCE: {
        return "MAINTENANCE";
      }
      case UNKNOWN: {
        return "UNKNOWN";
      }
      default: {
        return "UNKNOWN";
      }
    }
  }
}
