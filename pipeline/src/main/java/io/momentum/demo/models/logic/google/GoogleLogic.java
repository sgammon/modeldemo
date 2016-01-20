package io.momentum.demo.models.logic.google;


import io.momentum.demo.models.logic.PlatformBridge;
import io.momentum.demo.models.logic.PlatformLogic;
import io.momentum.demo.models.logic.oauth.google.GoogleOAuth2Logic;


/**
 * Created by sam on 1/13/16.
 */
public final class GoogleLogic extends PlatformLogic {
  public GoogleOAuth2Logic oauth2;

  public PlatformLogic setBridge(PlatformBridge bridge) {
    super.setBridge(bridge);

    oauth2 = new GoogleOAuth2Logic();
    oauth2.setBridge(bridge);
    return this;
  }
}
