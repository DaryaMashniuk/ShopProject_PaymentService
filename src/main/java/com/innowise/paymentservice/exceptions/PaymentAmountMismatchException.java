package com.innowise.paymentservice.exceptions;

public class PaymentAmountMismatchException extends RuntimeException {
  public PaymentAmountMismatchException(String message) {
    super(message);
  }
  public PaymentAmountMismatchException(String message, Throwable cause) {
    super(message, cause);
  }
  public PaymentAmountMismatchException(Throwable cause) {
    super(cause);
  }
}
