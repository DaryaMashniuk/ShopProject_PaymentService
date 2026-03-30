package com.innowise.paymentservice.facade;

import com.innowise.paymentservice.client.PaymentClient;
import com.innowise.paymentservice.config.RandomOrgProperties;
import com.innowise.paymentservice.exceptions.ExternalServiceUnavailableException;
import com.innowise.paymentservice.kafka.PaymentEventProducer;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.model.dto.PaymentRequest;
import com.innowise.paymentservice.model.dto.PaymentResponse;
import com.innowise.paymentservice.model.dto.PaymentSearchCriteria;
import com.innowise.paymentservice.model.events.PaymentStatusEvent;
import com.innowise.paymentservice.service.PaymentService;
import com.innowise.paymentservice.validator.PaymentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PaymentFacade {

  private final PaymentClient paymentClient;
  private final RandomOrgProperties randomOrgProperties;
  private final PaymentService paymentService;
  private final PaymentEventProducer paymentEventProducer;
  private final PaymentValidator paymentValidator;


  public PaymentResponse createPayment(PaymentRequest paymentRequest) {
    paymentValidator.validatePaymentCreation(paymentRequest);
    PaymentStatus status = getPaymentStatus();
    PaymentStatusEvent event = new PaymentStatusEvent(paymentRequest.getOrderId(),status);
    PaymentResponse response = paymentService.createPayment(paymentRequest,status);

    paymentEventProducer.sendPaymentEvent(event);
    return response;
  }


  public Page<PaymentResponse> findByCriteria(PaymentSearchCriteria criteria, Pageable pageable) {

    return paymentService.findByCriteria(criteria, pageable);
  }


  public BigDecimal getTotalSumForUserInRange(long userId, LocalDateTime from, LocalDateTime to) {

    return paymentService.getTotalSumForUserInRange(userId, from, to);
  }

  public BigDecimal getTotalSumForAllUsersInRange(LocalDateTime from, LocalDateTime to) {
    return paymentService.getTotalSumForAllUsersInRange(from, to);
  }

  private PaymentStatus getPaymentStatus() {
    int number = getRandomNumber();
    return number % 2 == 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
  }

  private int getRandomNumber() {
    String randomNumber = paymentClient.generateNumber(
            randomOrgProperties.getNum(),
            randomOrgProperties.getMin(),
            randomOrgProperties.getMax(),
            randomOrgProperties.getCol(),
            randomOrgProperties.getBase(),
            randomOrgProperties.getFormat(),
            randomOrgProperties.getRnd()
    );
    if (randomNumber == null || randomNumber.isBlank()) {
      throw new ExternalServiceUnavailableException("External service unavailable");
    }

    int number;
    try {
      number = Integer.parseInt(randomNumber.trim());
    } catch (NumberFormatException e) {
      throw new ExternalServiceUnavailableException("Invalid response from external service: " + randomNumber);
    }
    return number;
  }

}
