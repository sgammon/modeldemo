package io.momentum.demo.models.logic.servlets;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;

import io.momentum.demo.models.AppCredentials;
import io.momentum.demo.models.logic.oauth.google.AuthorizeUserException;
import io.momentum.demo.models.logic.runtime.render.TemplateService;
import io.momentum.demo.models.logic.runtime.state.AppEnginePlatformState;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by sam on 1/13/16.
 */
public abstract class AppServlet extends BaseServlet {
  // CSP headers
  public final static boolean enableCSPReport = false;
  public final static boolean enableCSPReportURI = true;

  // CSP rando
  private SecureRandom prng = null;

  protected String getUserToken(User user) {
    String source = user.getUserId() + "::" + (new AppEnginePlatformState()).getRuntimeSalt();
    try {
      MessageDigest d = MessageDigest.getInstance("SHA-256");
      d.update(source.getBytes("UTF-8"));
      byte[] hashbytes = d.digest();

      // encode hashbytes as hex
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < hashbytes.length; i++) {
        sb.append(Integer.toString((hashbytes[i] & 0xff) + 0x100, 16)
                         .substring(1));
      }
      return sb.toString();
    } catch (UnsupportedEncodingException e) {
      logging.severe("Unsupported encoding exception encountered in `getUserToken`.");
      throw new RuntimeException(e);
    } catch (NoSuchAlgorithmException e) {
      logging.severe("No such algorithm 'SHA-256'.");
      throw new RuntimeException(e);
    }
  }

  protected Credential authorizeUserForAdmin(HttpServletRequest request,
                                             HttpServletResponse response,
                                             boolean flush) throws IOException {
    // we are fetching credentials or it's a naked hit
    try {
      User targetUser = user();

      if (targetUser == null) {
        String serverName = request.getServerName();
        if (serverName.equals("mm-corp.systems"))
          serverName = "manage.mm-corp.systems";

        String current = request.getScheme() + "://" + serverName +
                             ("http".equals(request.getScheme()) && request.getServerPort() == 80 || "https".equals(
                                 request.getScheme()) && request.getServerPort() == 443 ? "" : ":" + request.getServerPort()) +
                             request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        String url = UserServiceFactory.getUserService()
                                       .createLoginURL(current);
        response.sendRedirect(url);
        return null;
      }

      return this.platform.google.oauth2.beginAdminFlow(targetUser.getUserId(), flush);
    } catch (GoogleJsonResponseException e) {
      logging.severe("Encountered error communicating with Google OAuth2 API. Attempting reauthorization.");
      logging.severe(e.getLocalizedMessage());

      if (!flush) {
        // try flushing credentials first
        return credential(request, response, true);
      } else {
        throw e;
      }

    } catch (AuthorizeUserException e) {
      // must redirect user to retrieve token
      e.uri.setState(getUserToken(user()));
      String redirectURI = e.uri.build();

      logging.warning("Sending user to OAuth2 endpoint: '" + redirectURI + "'.");
      response.sendRedirect(redirectURI);
      return null;
    }
  }

  protected Credential authorizeTokenCode(HttpServletRequest request,
                                          HttpServletResponse response) throws IOException {
    String code = request.getParameter("code");
    Boolean codeUsed = (Boolean) this.platform.memcache.sync.get("code::" + code);

    if (codeUsed != null && codeUsed) {
      // check existing creds just in case we already validated this code
      Credential localCreds = this.platform.google.oauth2.getCredential(user().getUserId());
      if (localCreds != null && localCreds.getAccessToken() != null) {
        // verify and return
        return this.platform.google.oauth2.verifyToken(localCreds);
      }

      // can't verify code again, perhaps it has been used (an exception will have been raised by this point)
      return null;

    } else {
      logging.info("Received redirect from OAuth2 endpoint with code '" + code + "'.");

      String state = request.getParameter("state");
      if (getUserToken(user()).equals(state)) {
        // continue the auth flow to get credentials
        Credential finalCreds = this.platform.google.oauth2.completeAdminFlow(user().getUserId(), code);
        this.platform.memcache.sync.put("code::" + code, true);  // leave a note not to use the code again
        return finalCreds;
      } else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter()
                .write("BAD_REQUEST");
        return null;
      }
    }
  }

  protected Credential credential(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
    return credential(request, response, false);
  }

  protected Credential credential(HttpServletRequest request,
                                  HttpServletResponse response,
                                  boolean flush) throws IOException {
    if (request.getParameter("code") == null) {
      return authorizeUserForAdmin(request,
                                   response,
                                   flush);
    } else {
      // we are fetching a code
      return authorizeTokenCode(request, response);
    }
  }

  protected boolean authorization(HttpServletRequest request) {
    return true;
  }

  protected String optimizeHTML(String responseData) {
    return responseData;
  }

  protected void render(String template,
                        Map<String, Object> context,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Credential credential)
      throws IOException, ServletException {
    // route and render JSP
    String responseData;
    String nonceValue = generateNonce();
    User currentUser = user();

    String cspHeader = "Content-Security-Policy";
    String hostParam = "'self'";
    if (!(new AppEnginePlatformState()).getProduction()) {
      hostParam = "'self'";
      if (enableCSPReport) {
        cspHeader = "Content-Security-Policy-Report-Only";
      }
    }

    if (cacheable(request)) {
      responseData = fetchFromCache(template, request);

      if (responseData == null) {

        // add it to the cache if it's not there
        renderTemplate(template,
                       prepareContext(context, credential, currentUser, nonceValue),
                       request,
                       response);
        storeInCache(template, request, response);
      } else {
        logging.fine("Found cached response for template '" + template + "'.");

        // fill response from cache -> we're not even using a wrapper, so return immediately
        PrintWriter writer = response.getWriter();
        writer.write(responseData);
        applyCachingHeaders(request, response);
        response.setHeader(cspHeader, generateCSPHeader(nonceValue, hostParam));
      }
    } else {
      renderTemplate(template,
                     prepareContext(context, credential, currentUser, nonceValue),
                     request,
                     response);
      response.setHeader(cspHeader, generateCSPHeader(nonceValue, hostParam));
    }
  }

  private String generateNonce()
      throws ServletException {
    String randomNum = Integer.toString(this.prng.nextInt());
    // --Get its digest
    MessageDigest sha;
    try {
      sha = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new ServletException(e);
    }
    byte[] digest = sha.digest(randomNum.getBytes());
    // --Encode it into HEXA
    return Hex.encodeHexString(digest)
              .substring(0, 8);
  }

  protected User user() {
    return UserServiceFactory.getUserService()
                             .getCurrentUser();
  }

  protected void authFailure(HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
    // do something to indicate failure
    logging.warning("Generic auth failure encountered. Returning status 403.");
    serve403(response);
  }

  protected boolean cacheable(HttpServletRequest request) {
    return false;
  }

  private String fetchFromCache(String template,
                                HttpServletRequest request) {
    //Object response = this.platform.memcache.sync.get(template);
    //if (response != null)
    //  return (String)response;
    return null;
  }

  protected static PebbleEngine templates() {
    return TemplateService.pebble();
  }

  private HttpServletResponse renderTemplate(String template,
                                             Map<String, Object> context,
                                             HttpServletRequest request,
                                             HttpServletResponse response)
      throws IOException, ServletException {
    // setup context
    setupContext(context, request, response);

    // resolve template
    PebbleTemplate tpl;
    try {
      tpl = templates().getTemplate(template);
    } catch (PebbleException e) {
      logging.severe("Failed to resolve template: '" + template + "'.");
      logging.severe(e.getMessage());
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    // render the template
    try {
      tpl.evaluate(response.getWriter(), context);
    } catch (PebbleException e) {
      logging.severe("Failed to render template '" + template + "'.");
      logging.severe(e.getMessage());
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return response;
  }

  protected Map<String, Object> prepareContext(Map<String, Object> context,
                                               Credential credential,
                                               User currentUser,
                                               String nonce) {
    // prepare extra context

    // add runtime state
    Map<String, Object> stateContext = new HashMap<>();
    AppEnginePlatformState platformState = new AppEnginePlatformState();
    stateContext.put("debug", !platformState.getProduction());
    stateContext.put("production", platformState.getProduction());
    context.put("state", stateContext);

    // add credentials
    Map<String, Object> credentialContext = new HashMap<>();
    credentialContext.put("id", AppCredentials.Web.clientId);
    credentialContext.put("secret", AppCredentials.Web.clientSecret);
    credentialContext.put("key", AppCredentials.Web.apiKey);
    credentialContext.put("scopes", AppCredentials.Web.scopes);

    if (credential != null) {
      credentialContext.put("access", credential.getAccessToken());
      credentialContext.put("refresh", credential.getRefreshToken());
      credentialContext.put("expiration", credential.getExpirationTimeMilliseconds());
    }
    context.put("credentials", credentialContext);

    // prepare user
    if (currentUser != null) {
      Map<String, Object> userContext = new HashMap<>();
      userContext.put("id", currentUser.getUserId());
      userContext.put("nickname", currentUser.getNickname());
      userContext.put("email", currentUser.getEmail());
      userContext.put("domain", currentUser.getAuthDomain());
      context.put("user", userContext);
    } else {
      context.put("user", null);
    }

    // prepare API load list
    Map<String, Object> apiContext = new HashMap<>();
    apiContext.put("load",
                   "[\"unified\",\"v1\"]");
    context.put("api", apiContext);

    // add CSP nonce
    context.put("nonce", nonce);
    return context;
  }

  private void storeInCache(String template,
                            HttpServletRequest request,
                            HttpServletResponse responseData) {
    logging.info("Caching page response for template '" + template + "'.");
    //this.platform.memcache.sync.put(template, responseData);
  }

  private void applyCachingHeaders(HttpServletRequest request,
                                   HttpServletResponse response) {
    // calculate static caching headers
    Integer frontendTimeout = frontendCacheable(request, response);
    if (frontendTimeout > 0) {
      String cacheMode = frontendCacheMode(request, response);

      // if it's active, set the headers accordingly
      if (cacheMode.equals("public") || cacheMode.equals("private")) {
        response.setHeader("Cache-Control", cacheMode + ", max-age=" + frontendTimeout.toString());
      }
    }
  }

  private String generateCSPHeader(String nonce, String hostValue) {
    String baseHeader = (
                            "reflected-xss filter; " +
                                "referrer origin; " +
                                "default-src " + hostValue + "; " +
                                "img-src " + hostValue + " //*.googleusercontent.com //csi.gstatic.com //maps.googleapis.com; " +
                                "style-src " + hostValue + " //fonts.googleapis.com //www.google.com //ajax.googleapis.com 'unsafe-inline'; " +
                                "font-src " + hostValue + " //fonts.gstatic.com; " +
                                "frame-src " + hostValue + " //mm-api-demo.appspot.com/ //accounts.google.com //apis.google.com //public.google.stackdriver.com //content.googleapis.com; " +
                                "script-src " + hostValue + " //apis.google.com //www.google.com //maps.googleapis.com //maps.gstatic.com //www.gstatic.com 'unsafe-eval' 'unsafe-inline';");
    if (enableCSPReportURI)
      baseHeader += " report-uri /api/event/csp;";
    return baseHeader;
  }

  private void setupContext(Map<String, Object> context,
                            HttpServletRequest request,
                            HttpServletResponse response) {
    context.put("request", request);
    context.put("response", response);
  }

  protected Integer frontendCacheable(HttpServletRequest request,
                                      HttpServletResponse response) {
    return 600;
  }

  protected String frontendCacheMode(HttpServletRequest request,
                                     HttpServletResponse response) {
    return "private";
  }

  protected void GET(HttpServletRequest request,
                     HttpServletResponse response,
                     Credential credential) throws IOException,
                                                   ServletException {
    logging.info("Default GET handler executed.");
  }

  protected void POST(HttpServletRequest request,
                      HttpServletResponse response,
                      Credential credential) throws IOException,
                                                    ServletException {
    logging.info("Default POST handler executed.");
  }

  @Override
  protected void doGet(HttpServletRequest request,
                       HttpServletResponse response)
      throws IOException, ServletException {
    if (authorization(request)) {
      try {
        Credential creds = credential(request, response);
        if (creds != null) {
          GET(request, response, creds);
        }
        // we're redirecting - let it die
      } catch (TokenResponseException e) {
        // handle auth failure
        authFailure(request, response);
      }
    } else {
      GET(request, response, null);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request,
                        HttpServletResponse response)
      throws IOException, ServletException {
    if (authorization(request)) {
      try {
        Credential creds = credential(request, response);
        if (creds != null) {
          POST(request, response, creds);
        }
        // we're redirecting - let it die
      } catch (TokenResponseException e) {
        // handle auth failure
        authFailure(request, response);
      }
    } else {
      POST(request, response, null);
    }
  }

  @Override
  public void init(ServletConfig config)
      throws ServletException {
    try {
      this.prng = SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException e) {
      throw new ServletException(e);
    }
    super.init(config);
  }
}
