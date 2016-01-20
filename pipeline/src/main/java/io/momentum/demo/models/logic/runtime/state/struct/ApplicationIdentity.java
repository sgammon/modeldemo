package io.momentum.demo.models.logic.runtime.state.struct;


import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.appidentity.PublicCertificate;
import com.google.appengine.api.utils.SystemProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by sam on 1/13/16.
 */
public class ApplicationIdentity {
  private static AppIdentityService appIdentity = AppIdentityServiceFactory.getAppIdentityService();

  /*  --  data  --  */
  private static String defaultBucket;
  private static String serviceAccount;
  private static Map<String, PublicCertificate> certificates;

  /*  --  getters  --  */
  public static String getApplicationId() {
    return SystemProperty.applicationId.get();
  }

  public static String getDefaultBucket() {
    if (defaultBucket == null) {
      defaultBucket = appIdentity.getDefaultGcsBucketName();
    }
    return defaultBucket;
  }

  public static String getServiceAccount() {
    if (serviceAccount == null) {
      serviceAccount = appIdentity.getServiceAccountName();
    }
    return serviceAccount;
  }

  public static Map<String, PublicCertificate> getPublicCertificates() {
    if (certificates == null || certificates.isEmpty()) {
      Collection<PublicCertificate> certs = appIdentity.getPublicCertificatesForApp();

      certificates = new HashMap<String, PublicCertificate>(certs.size());
      for (PublicCertificate cert : certs) {
        certificates.put(cert.getCertificateName(), cert);
      }
    }
    return certificates;
  }
}
