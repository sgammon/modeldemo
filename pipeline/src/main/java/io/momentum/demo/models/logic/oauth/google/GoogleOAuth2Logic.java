package io.momentum.demo.models.logic.oauth.google;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.appengine.datastore.AppEngineDataStoreFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Person;

import io.momentum.demo.models.AppCredentials;
import io.momentum.demo.models.logic.PlatformLogic;
import io.momentum.demo.models.logic.runtime.state.AppEnginePlatformState;
import io.momentum.demo.models.logic.runtime.state.AppEngineRuntimeState;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by sam on 1/13/16.
 */
public final class GoogleOAuth2Logic extends PlatformLogic {
  public static final String localRedirect = "http://localhost:8080/dashboard";
  public static final String authRedirect = "https://manage.mm-corp.systems/dashboard";
  Set<String> scopes = new HashSet<>(Arrays.asList(AppCredentials.Web.scopes));

  public Credential getCredential(String userId)
      throws IOException {
    GoogleAuthorizationCodeFlow flow = adminAuthFlow(scopes);
    return getCredential(flow, userId);
  }

  private GoogleAuthorizationCodeFlow adminAuthFlow(Set<String> scopes)
      throws IOException {
    return new GoogleAuthorizationCodeFlow.Builder(GAE_TRANSPORT,
                                                   JSON_FACTORY,
                                                   AppCredentials.Web.clientId,
                                                   AppCredentials.Web.clientSecret,
                                                   scopes)
               .setAccessType("offline")
               .setDataStoreFactory(new AppEngineDataStoreFactory.Builder().build())
               .setApprovalPrompt("auto")
               .build();
  }

  public Credential getCredential(GoogleAuthorizationCodeFlow flow, String userId)
      throws IOException {
    return flow.loadCredential(userId);
  }

  public Credential beginAdminFlow(String userId, boolean flush)
      throws IOException, AuthorizeUserException {
    GoogleAuthorizationCodeFlow flow = adminAuthFlow(scopes);
    Credential creds = getCredential(flow, userId);
    if (!flush && (creds != null && creds.getAccessToken() != null)) {
      return verifyToken(creds);
    } else {
      GoogleAuthorizationCodeRequestUrl uri = flow.newAuthorizationUrl();

      if ((new AppEngineRuntimeState()).getDebug()) {
        uri.setRedirectUri(localRedirect);
      } else {
        uri.setRedirectUri(authRedirect);
      }

      uri.setScopes(scopes);
      uri.setClientId(AppCredentials.Web.clientId);
      throw new AuthorizeUserException(uri);
    }
  }

  public Credential verifyToken(Credential credential)
      throws IOException {
    Oauth2 oauth2 = new Oauth2.Builder(GAE_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName(new AppEnginePlatformState().getProject())
                        .build();
    Tokeninfo tokenInfo = oauth2.tokeninfo()
                                .setAccessToken(credential.getAccessToken())
                                .execute();
    if (tokenInfo.containsKey("error")) {
      logging.info("Failed to validate authorization token. Error: " + tokenInfo.get("error")
                                                                                .toString());
      throw new IOException(tokenInfo.get("error")
                                     .toString());
    }

    if (tokenInfo.getIssuedTo()
                 .equals(AppCredentials.Web.clientId)) {
      logging.info("Validated token and issuer. Proceeding.");
      return credential;
    } else {
      logging.severe("Invalid token: issued to foreign client ID '" + tokenInfo.getIssuedTo() + "'.");
      throw new IOException("Token issued to foreign client ID.");
    }
  }

  public Credential completeAdminFlow(String userId, String code)
      throws IOException {
    // get scopes and flow
    Set<String> scopes = new HashSet<>(Arrays.asList(AppCredentials.Web.scopes));
    GoogleAuthorizationCodeFlow flow = adminAuthFlow(scopes);

    // request token and store credential
    TokenResponse token = requestTokenFromCode(flow, code);
    return flow.createAndStoreCredential(token, userId);
  }

  public TokenResponse requestTokenFromCode(GoogleAuthorizationCodeFlow flow, String code)
      throws IOException {
    if ((new AppEngineRuntimeState()).getDebug())
      return flow.newTokenRequest(code)
                 .setRedirectUri(localRedirect)
                 .execute();
    return flow.newTokenRequest(code)
               .setRedirectUri(authRedirect)
               .execute();
  }

  public Person resolveGoogleUser(Credential credential)
      throws IOException {
    Plus plus = new Plus.Builder(GAE_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(new AppEnginePlatformState().getProject())
                    .build();
    Person profile = plus.people()
                         .get("me")
                         .execute();

    // it maybe worked
    logging.info("Resolved Google user profile '" + profile.getDisplayName() + "'.");
    return profile;
  }
}
