package com.innowise.paymentservice.validator;

import com.innowise.paymentservice.model.dto.PaymentRequest;


public interface PaymentValidator {

  void validatePaymentCreation(PaymentRequest request);

}