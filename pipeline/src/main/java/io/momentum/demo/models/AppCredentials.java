package io.momentum.demo.models;


import io.momentum.demo.models.logic.oauth.google.GoogleScopes;


/**
 * Created by sam on 1/13/16.
 */
public final class AppCredentials {
  public static final class iOS {
    public final static String apiKey = "AIzaSyAwjqbXB8UyoXq2gWQK-5CwQwjcxEhRPAU";
    public final static String clientId = "167847261752-v6n5uqjlb19e82b175f0c48r6o8jq56o.apps.googleusercontent.com";
  }

  public static final class Web {
    public final static String apiKey = "AIzaSyBdJ6HBBzkdxeej0UFSaA0YI2DDUznsKbY";
    public final static String clientId = "167847261752-c5mk85ame47r1kfqi5612mvk6vkn3en3.apps.googleusercontent.com";
    public final static String clientSecret = "W8dJp2seV2UUDNefEwfZVlPw";
    public final static String clientEmail = "167847261752-c5mk85ame47r1kfqi5612mvk6vkn3en3.apps.googleusercontent.com";
    public final static String[] scopes = {
        GoogleScopes.COMPUTE,
        GoogleScopes.MONITORING,
        GoogleScopes.PUBSUB,
        GoogleScopes.BIGQUERY,
        GoogleScopes.DATASTORE,
        GoogleScopes.TASKQUEUE,
        GoogleScopes.CLOUD,
        GoogleScopes.STORAGE
    };
  }

  public static final class CLI {
    public final static String apiKey = "AIzaSyCBP7zD9TGGaNbv5yEOJJJu2f2uGU7rb0E";
    public final static String clientId = "167847261752-om52pli6p8hjbdek62ocrmulkkkvrr46.apps.googleusercontent.com";
    public final static String clientSecret = "6IrvXftSJGmIcp313Rra5Wo4";
  }

  public static final class Service {
    public final static String apiKey = "AIzaSyBSZoWrr3RfdsN6P4uiDLZNf5894yGjCZs";
    public final static String clientId = "167847261752-7etf5o1a76051bkldtbabrq85hpmokgp.apps.googleusercontent.com";
    public final static String clientSecret = "EJ3sbkTqPMKJqAvKx44RwS--";
    public final static String keyID = "b56ff804b0d70478fa8be3118f568d9b158cab34";
    public final static String clientEmail = "app-v1@mm-api-demo.iam.gserviceaccount.com";
    public final static String[] scopes = {
        GoogleScopes.COMPUTE,
        GoogleScopes.MONITORING,
        GoogleScopes.PUBSUB,
        GoogleScopes.BIGQUERY,
        GoogleScopes.DATASTORE,
        GoogleScopes.TASKQUEUE,
        GoogleScopes.CLOUD,
        GoogleScopes.STORAGE
    };
  }
}
