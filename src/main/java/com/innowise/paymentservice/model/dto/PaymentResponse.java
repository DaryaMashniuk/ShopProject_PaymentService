package com.innowise.paymentservice.model.dto;

import com.innowise.paymentservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
  private String id;
  private Long userId;

  private Long orderId;

  private PaymentStatus status;

  private LocalDateTime timestamp;
  private BigDecimal paymentAmount;
}
