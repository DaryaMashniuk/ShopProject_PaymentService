package com.innowise.paymentservice.model.dto;

import com.innowise.paymentservice.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {
  private String id;
  private Long userId;

  private Long orderId;

  private PaymentStatus status;

  private LocalDateTime timestamp;
  private BigDecimal paymentAmount;
}
