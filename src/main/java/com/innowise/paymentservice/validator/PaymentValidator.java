package com.innowise.paymentservice.validator;

import com.innowise.paymentservice.model.dto.PaymentRequest;


public interface PaymentValidator {

  /**
   * Validates a payment request before creation.
   *
   * This method performs comprehensive validation of the payment request:
   * <ol>
   *   <li><b>Duplicate Payment Check:</b> Ensures no successful payment exists for the order</li>
   *   <li><b>Amount Validation:</b> Verifies payment amount matches the order's expected amount</li>
   *   <li><b>Order Existence:</b> Confirms order information is available in the cache</li>
   * </ol>
   *
   * The validation uses cached order information received via Kafka from the Order Service.
   * If validation fails, appropriate exceptions are thrown with detailed messages.
   *
   * @param request the payment request to validate
   * @throws com.innowise.paymentservice.exceptions.PaymentIsAlreadyFinishedException
   *         if a successful payment already exists for the order
   * @throws com.innowise.paymentservice.exceptions.PaymentAmountMismatchException
   *         if the payment amount doesn't match the order total
   * @throws com.innowise.paymentservice.exceptions.ResourceNotFoundException
   *         if order information is not found in the cache
   * @throws IllegalArgumentException if payment amount is null
   */
  void validatePaymentCreation(PaymentRequest request);
}