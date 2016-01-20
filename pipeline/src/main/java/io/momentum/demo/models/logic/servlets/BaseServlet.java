package io.momentum.demo.models.logic.servlets;


import com.google.api.client.http.HttpStatusCodes;
import com.googlecode.objectify.Objectify;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import io.momentum.demo.models.logic.PlatformBridge;
import io.momentum.demo.models.logic.runtime.datastore.DatastoreService;

import java.io.IOException;
import java.util.logging.Logger;


/**
 * Created by sam on 1/13/16.
 */
public abstract class BaseServlet extends HttpServlet {
  protected static final Logger logging = Logger.getAnonymousLogger();
  protected final Objectify datastore() { return DatastoreService.ofy(); }
  protected PlatformBridge platform = PlatformBridge.acquire();

  protected void serveOK(HttpServletResponse response)
      throws IOException {
    response.setStatus(HttpStatusCodes.STATUS_CODE_OK);
    response.getWriter()
            .println("OK");
    response.setHeader("Content-Type", "text/plain");
  }

  protected void serve201(HttpServletResponse response)
      throws IOException {
    response.setStatus(201);
    response.getWriter()
            .println("Created");
    response.setHeader("Content-Type", "text/plain");
  }

  protected void serve403(HttpServletResponse response)
      throws IOException {
    response.setStatus(HttpStatusCodes.STATUS_CODE_FORBIDDEN);
    response.getWriter()
            .println("403 Forbidden");
    response.setHeader("Content-Type", "text/plain");
  }

  protected void serve404(HttpServletResponse response)
      throws IOException {
    response.setStatus(HttpStatusCodes.STATUS_CODE_NOT_FOUND);
    response.getWriter()
            .println("404 Not Found");
    response.setHeader("Content-Type", "text/plain");
  }

  protected void serve405(HttpServletResponse response)
      throws IOException {
    serve405(null, response);
  }

  protected void serve405(String methodToTry, HttpServletResponse response)
      throws IOException {
    response.setStatus(HttpStatusCodes.STATUS_CODE_NOT_FOUND);
    response.getWriter()
            .println("405 Method Not Allowed" + (methodToTry != null ? ": Try " + methodToTry : ""));
    response.setHeader("Content-Type", "text/plain");
  }

  protected void serve400(HttpServletResponse response)
      throws IOException {
    response.setStatus(400);
    response.getWriter()
            .println("400 Bad Request");
    response.setHeader("Content-Type", "text/plain");
  }
}
