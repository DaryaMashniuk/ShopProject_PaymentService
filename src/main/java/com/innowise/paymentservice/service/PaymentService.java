package com.innowise.paymentservice.service;

import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.model.dto.PaymentRequest;
import com.innowise.paymentservice.model.dto.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface PaymentService {
  void createPayment(PaymentRequest paymentRequest);
  Page<PaymentResponse> findByField(Long userId, Long orderId, PaymentStatus status, Pageable pageable);
  BigDecimal getTotalSumForUserInRange(long userId, LocalDateTime from, LocalDateTime to);
  BigDecimal getTotalSumForAllUsersInRange(LocalDateTime from, LocalDateTime to);
}