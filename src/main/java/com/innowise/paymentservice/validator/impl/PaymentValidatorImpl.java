package com.innowise.paymentservice.validator.impl;

import com.innowise.paymentservice.exceptions.PaymentAmountMismatchException;
import com.innowise.paymentservice.exceptions.PaymentIsAlreadyFinishedException;
import com.innowise.paymentservice.exceptions.ResourceNotFoundException;
import com.innowise.paymentservice.model.OrderInfoView;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.model.dto.PaymentRequest;
import com.innowise.paymentservice.model.dto.PaymentResponse;
import com.innowise.paymentservice.model.dto.PaymentSearchCriteria;
import com.innowise.paymentservice.repository.OrderInfoRepository;
import com.innowise.paymentservice.service.PaymentService;
import com.innowise.paymentservice.validator.PaymentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentValidatorImpl implements PaymentValidator {

  private final PaymentService paymentService;
  private final OrderInfoRepository orderInfoRepository;

  @Override
  public void validatePaymentCreation(PaymentRequest request) {

    validateAlreadySuccessful(request.getOrderId());
    validateAmount(request);
  }

  private void validateAlreadySuccessful(Long orderId) {
    PaymentSearchCriteria criteria = new PaymentSearchCriteria();
    criteria.setOrderId(orderId);

    Page<PaymentResponse> page =
            paymentService.findByCriteria(criteria, Pageable.unpaged());

    boolean alreadySuccessful = page.stream()
            .anyMatch(p -> PaymentStatus.SUCCESS.equals(p.getStatus()));

    if (alreadySuccessful) {
      throw new PaymentIsAlreadyFinishedException(
              "Payment already successfully completed for orderId=" + orderId
      );
    }
  }

  private void validateAmount(PaymentRequest request) {

    OrderInfoView orderInfo = orderInfoRepository.findById(request.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Order info not found for validation. Order ID: " + request.getOrderId()
            ));

    if (request.getPaymentAmount() == null) {
      throw new IllegalArgumentException("Payment amount must not be null");
    }

    if (orderInfo.getExpectedAmount().compareTo(request.getPaymentAmount()) != 0) {
      throw new PaymentAmountMismatchException(
              String.format(
                      "Payment amount %s does not match order price %s",
                      request.getPaymentAmount(),
                      orderInfo.getExpectedAmount()
              )
      );
    }
  }
}
