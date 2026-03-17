package com.innowise.paymentservice.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentRequest {

  @NotNull
  @Positive
  private Long userId;

  @NotNull
  @Positive
  private Long orderId;
  @Past
  @NotNull
  private LocalDateTime timestamp;
  @Positive
  @NotNull
  private BigDecimal paymentAmount;

}
