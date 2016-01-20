package io.momentum.demo.models.logic;


import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.compute.ComputeCredential;
import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.services.pubsub.PubsubScopes;

import com.googlecode.objectify.Objectify;

import io.momentum.demo.models.AppCredentials;
import io.momentum.demo.models.logic.runtime.datastore.DatastoreService;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;


/**
 * Created by sam on 1/13/16.
 */
public abstract class PlatformLogic {
  protected static final HttpTransport GAE_TRANSPORT = UrlFetchTransport.getDefaultInstance();
  protected static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  protected static final Logger logging = Logger.getAnonymousLogger();
  protected final Objectify datastore() {
    return DatastoreService.ofy();
  }

  protected PlatformBridge bridge;

  protected Credential resolveCredentials()
      throws IOException {
    try {
      if (!bridge.getPlatformState()
                 .getProduction()) {
        if (bridge.getPlatformState()
                  .getContainer()
                  .equals("compute"))
          return computeEngineCredentials(GoogleNetHttpTransport.newTrustedTransport());
      } else {
        if (bridge.getPlatformState()
                  .getContainer()
                  .equals("appengine"))
          return appEngineCredentials();
      }

      return serviceAccountCredentials(GoogleNetHttpTransport.newTrustedTransport(),
                                       bridge.getPlatformState()
                                             .getServiceKeyPath());
    } catch (GeneralSecurityException e) {
      logging.severe("General security exception encountered: '" + e.getLocalizedMessage() + "'.");
      throw new IOException(e);  // wrap in `IOException`
    }
  }

  /* -- API client -- */
  private ComputeCredential computeEngineCredentials(HttpTransport transport)
      throws IOException {
    logging.finer("Using Compute Engine OAuth credentials.");
    return new ComputeCredential.Builder(transport, JSON_FACTORY).build();
  }

  private GoogleCredential appEngineCredentials()
      throws IOException {
    logging.finer("Using App Engine OAuth credentials.");
    return new AppIdentityCredential.AppEngineCredentialWrapper(GAE_TRANSPORT, JSON_FACTORY).createScoped(
        PubsubScopes.all());
  }

  private GoogleCredential serviceAccountCredentials(HttpTransport transport,
                                                     String path)
      throws IOException, GeneralSecurityException {
    logging.finer("Using service account credentials.");
    GoogleCredential.Builder builder = new GoogleCredential.Builder().setTransport(transport)
                                         .setJsonFactory(JSON_FACTORY)
                                         .setServiceAccountScopes(
                                             new ArrayList<String>(Arrays.asList(AppCredentials.Service.scopes)))
                                         .setServiceAccountId(AppCredentials.Service.clientEmail)
                                         .setServiceAccountPrivateKeyFromP12File(new File(path));

    return builder.build();
  }

  public PlatformLogic setBridge(PlatformBridge platformBridge) {
    bridge = platformBridge;
    return this;
  }
}
