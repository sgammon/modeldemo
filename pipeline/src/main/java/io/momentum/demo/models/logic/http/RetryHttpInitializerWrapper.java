package io.momentum.demo.models.logic.http;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.*;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.Sleeper;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.logging.Logger;


/**
 * Created by sam on 1/13/16.
 */
public final class RetryHttpInitializerWrapper implements HttpRequestInitializer {
  /* -- internals -- */
  private static final Logger LOG =
      Logger.getLogger(RetryHttpInitializerWrapper.class.getName());

  private final Credential wrappedCredential;
  private final Sleeper sleeper;

  public RetryHttpInitializerWrapper(Credential wrappedCredential) {
    this(wrappedCredential, Sleeper.DEFAULT);
  }

  RetryHttpInitializerWrapper(Credential wrappedCredential, Sleeper sleeper) {
    this.wrappedCredential = Preconditions.checkNotNull(wrappedCredential);
    this.sleeper = sleeper;
  }

  @Override
  public void initialize(HttpRequest request) {
    final HttpUnsuccessfulResponseHandler backoffHandler =
        new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff())
            .setSleeper(sleeper);
    request.setInterceptor(wrappedCredential);
    request.setUnsuccessfulResponseHandler(new HttpUnsuccessfulResponseHandler() {
      @Override
      public boolean handleResponse(HttpRequest request,
                                    HttpResponse response,
                                    boolean supportsRetry)
          throws IOException {
        if (wrappedCredential.handleResponse(request, response, supportsRetry)) {
          // If credential decides it can handle it, the return code or
          // message indicated something specific to authentication,
          // and no backoff is desired.
          return true;
        } else if (backoffHandler.handleResponse(request, response, supportsRetry)) {
          // Otherwise, we defer to the judgement of
          // our internal backoff handler.
          LOG.info("Retrying " + request.getUrl());
          return true;
        } else {
          return false;
        }
      }
    });
    request.setIOExceptionHandler(new HttpBackOffIOExceptionHandler(new ExponentialBackOff())
                                      .setSleeper(sleeper));
  }
}
