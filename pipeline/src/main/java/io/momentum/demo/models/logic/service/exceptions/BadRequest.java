package io.momentum.demo.models.logic.service.exceptions;


import com.google.api.server.spi.response.BadRequestException;


/**
 * Created by sam on 1/20/16.
 */
public final class BadRequest extends BadRequestException {
  /** -- state -- **/
  private final ServiceError error;
  private final String errorMessage;
  private final String code;

  /** -- constructors -- **/
  public BadRequest(ServiceError error) {
    super("[" + error.toString() + "]");
    this.error = error;
    this.code = error.toString();
    this.errorMessage = null;
  }

  public BadRequest(ServiceError error,
                    String message) {
    super(message != null ? "[" + error.toString() + "]: " + message : "[" + error.toString() + "]");
    this.error = error;
    this.errorMessage = message;
    this.code = error.toString();
  }

  public BadRequest(String code,
                    String message) {
    super(message != null ? "[" + code + "]: " + message : "[" + code + "]");
    this.code = code;
    this.errorMessage = message;
    this.error = null;
  }

  /** -- getters / setters -- **/
  public String getCode() {
    return code;
  }

  public ServiceError getError() {
    return error;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
