package com.innowise.paymentservice.service;

import com.innowise.paymentservice.model.dto.PaymentRequest;
import com.innowise.paymentservice.model.dto.PaymentResponse;
import com.innowise.paymentservice.model.dto.PaymentSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentService {
  PaymentResponse createPayment(PaymentRequest paymentRequest);
  Page<PaymentResponse> findByCriteria(PaymentSearchCriteria criteria, Pageable pageable);
  BigDecimal getTotalSumForUserInRange(long userId, LocalDateTime from, LocalDateTime to);
  BigDecimal getTotalSumForAllUsersInRange(LocalDateTime from, LocalDateTime to);
}