package com.innowise.paymentservice.exceptions;

public class PaymentIsAlreadyFinishedException extends RuntimeException {
  public PaymentIsAlreadyFinishedException(String message) {
    super(message);
  }
  public PaymentIsAlreadyFinishedException(String message, Throwable cause) {
    super(message, cause);
  }
  public PaymentIsAlreadyFinishedException(Throwable cause) {
    super(cause);
  }
}
