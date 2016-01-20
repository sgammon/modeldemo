package io.momentum.demo.models.logic.oauth.google;


import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;


/**
 * Created by sam on 1/13/16.
 */
public final class AuthorizeUserException extends Exception {
  public GoogleAuthorizationCodeRequestUrl uri;

  public AuthorizeUserException(GoogleAuthorizationCodeRequestUrl uri) {
    this.uri = uri;
  }
}
