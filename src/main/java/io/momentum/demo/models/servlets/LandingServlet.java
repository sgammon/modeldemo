package io.momentum.demo.models.servlets;


import com.google.api.client.auth.oauth2.Credential;
import com.google.appengine.api.datastore.ReadPolicy;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.momentum.demo.models.logic.service.models.SerializedModel;
import io.momentum.demo.models.logic.servlets.AppServlet;
import io.momentum.demo.models.schema.UserMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by sam on 1/13/16.
 */
public class LandingServlet extends AppServlet {
  @Override
  protected boolean authorization(HttpServletRequest request) {
    return false;  // don't require auth
  }

  protected List<SerializedModel> getMessages() {
    List<UserMessage> messages = datastore().consistency(ReadPolicy.Consistency.EVENTUAL)
                      .load()
                      .type(UserMessage.class)
                      .hybrid(true)
                      .list();

    List<SerializedModel> serializedModels = new ArrayList<>();
    if (messages.size() > 0) {
      for (UserMessage message : messages) {
        serializedModels.add(message.serialize());
      }
    }
    return serializedModels;
  }

  @Override
  protected void GET(HttpServletRequest request,
                     HttpServletResponse response,
                     Credential credential) throws IOException, ServletException {
    HashMap<String, Object> context = new HashMap<>();

    if (user() != null) {
      String name = user().getNickname();
      if (name.contains("@")) {
        String[] namesplit = name.split("@");
        name = namesplit[0];
      }
      context.put("name", name.substring(0, 1).toUpperCase() + name.substring(1));
    }
    context.put("messages", getMessages());
    render("landing", context, request, response, credential);
  }
}
