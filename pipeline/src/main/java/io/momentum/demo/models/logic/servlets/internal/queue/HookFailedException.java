package io.momentum.demo.models.logic.servlets.internal.queue;


/**
 * Created by sam on 1/22/16.
 */
public class HookFailedException extends Exception {
  public Integer code;
  public Throwable cause;
  public String message;
  public boolean normal;

  public HookFailedException() {
  }

  public HookFailedException(String message) {
    this.code = 500;
    this.message = message;
    this.normal = false;
  }

  public HookFailedException(Throwable cause) {
    if (cause instanceof HookFailedException) {
      HookFailedException failure = (HookFailedException) cause;
      this.code = failure.code;
      this.message = failure.getMessage();
      this.cause = failure.cause;
      this.normal = failure.normal;
    } else {
      this.code = 500;
      this.message = cause.getMessage();
      this.cause = cause;
      this.normal = false;
    }
  }

  @Override
  public String getMessage() {
    if (cause != null) return cause.getMessage();
    if (message != null) return message;
    if (code != null) return "Generic error with code " + String.valueOf(code) + ".";
    return "Generic error occurred.";
  }

  public HookFailedException(Integer code,
                             String message) {
    this.code = code;
    this.message = message;
    this.normal = false;
  }

  public HookFailedException(Integer code,
                             String message,
                             Throwable cause) {
    this.code = code;
    this.message = message;
    this.cause = cause;
    this.normal = false;
  }

  public void setNormal(boolean normal) {
    this.normal = normal;
  }

  public HookFailedException setCause(Throwable cause) {
    this.cause = cause;
    if (cause.getMessage() != null) message = cause.getMessage();
    return this;
  }
}
