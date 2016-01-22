package io.momentum.demo.models.logic.servlets.internal.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

import io.momentum.demo.models.logic.servlets.BaseServlet;
import io.momentum.demo.models.pipeline.PlatformCodec;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by sam on 1/22/16.
 */
public abstract class HookEventServlet extends BaseServlet {
  /* util methods */
  protected BufferedReader getBufferedBodyReader(HttpServletRequest request) throws IOException {
    return new BufferedReader(new InputStreamReader(request.getInputStream()));
  }

  protected Map<String, Object> decodeJSONPayload(HttpServletRequest request,
                                                  BufferedReader reader) throws IOException, HookFailedException {
    return decodeJSONPayload(request, reader, payloadAsString(reader));
  }

  protected Map<String, Object> decodeJSONPayload(HttpServletRequest request,
                                                  BufferedReader reader,
                                                  String rawPayload) throws IOException, HookFailedException {

    // build payload and map
    return new PlatformCodec().getReader().readValue(rawPayload, new TypeReference<HashMap<String, Object>>() { });
  }

  protected byte[] rawPayload(HttpServletRequest request) throws IOException {
    return IOUtils.toByteArray(request.getInputStream());
  }

  protected String payloadAsString(BufferedReader reader) throws IOException, HookFailedException {
    String rawPayload = "";
    // read payload, without getting crazy
    int lines = 0;
    int line_limit = 1000;
    String payloadLine;

    while (((payloadLine = reader.readLine()) != null) && lines != line_limit) {
      rawPayload += payloadLine;
      lines += 1;
    }

    // refuse large payloads
    if (lines == line_limit) {
      failHook(400, "Payload too large.");
    }
    return rawPayload;
  }

  protected void failHook(int status, String message) throws HookFailedException {
    logging.severe("Hook failed with message: \"" + message + "\".");
    throw new HookFailedException(status, message);
  }

  /* failure utilities */
  protected void failHook(String message) throws HookFailedException {
    logging.severe("Hook failed with message: \"" + message + "\".");
    throw new HookFailedException(message);
  }

  protected void failHook(int status, String message, Boolean normal) throws HookFailedException {
    logging.severe("Hook failed with message: \"" + message + "\".");
    HookFailedException err = new HookFailedException(status, message);
    err.setNormal(normal);
    throw err;
  }

  /* HTTP methods */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    executeHook(req, resp);
  }

  protected void executeHook(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    int status;
    try {
      status = onHook(req);

    } catch (HookFailedException e) {
      logging.severe("Hook failed: '"
                         + e.getMessage() + "'.");
      onHookFail(e, resp, !e.normal);  // only print if it is an abnormal exception
      return;
    } catch (Exception exc) {
      logging.severe("Hook failed with exception: '"
                         + exc.getClass()
                              .getName() + "'.");
      logging.severe("Exception message: '"
                         + exc.getMessage() + "'.");

      onHookFail(new HookFailedException(exc), resp, true);  // always print - unhandled *is* abnormal
      return;
    }

    // everything went so well
    String message = "OK";
    if (status == 201) {
      message = "CREATED";
    } else if (status == 202) {
      message = "ACCEPTED";
    } else if (status >= 400) {
      message = "ERROR";
    }

    sendResponse(resp, status, message);
  }

  /* override-able methods */
  protected int onHook(HttpServletRequest req) throws HookFailedException, IOException {
    logging.info("Default hook executed.");
    return 200;
  }

  protected void onHookFail(HookFailedException exc, HttpServletResponse response, boolean print) throws IOException {
    logging.severe("Hook failed with unhandled exception.");

    if (print) {
      StringWriter w = new StringWriter();
      PrintWriter p = new PrintWriter(w);
      if (exc.cause != null) {
        exc.cause.printStackTrace(p);
      } else {
        exc.printStackTrace(p);
      }

      logging.severe("Stacktrace: " + w.toString());
    }
    sendResponse(response, exc.code, "FAIL");
  }

  /* control methods */
  private void sendResponse(HttpServletResponse resp, int code, String status) throws IOException {
    resp.setStatus(code);
    logging.info("Responding with code " +
                     String.valueOf(code) + ".");
    resp.setContentType("text/plain");
    resp.setHeader("K9-Event-State", status);
    resp.getWriter()
        .println(status);
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    executeHook(req, resp);
  }
}
