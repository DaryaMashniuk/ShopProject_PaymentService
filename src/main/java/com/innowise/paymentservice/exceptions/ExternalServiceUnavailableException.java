package com.innowise.paymentservice.exceptions;

public class ExternalServiceUnavailableException extends RuntimeException{
  public ExternalServiceUnavailableException(String message) {
    super(message);
  }
  public ExternalServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
  public ExternalServiceUnavailableException(Throwable cause) {
    super(cause);
  }
}
