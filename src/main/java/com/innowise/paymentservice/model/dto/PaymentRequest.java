package com.innowise.paymentservice.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class PaymentRequest {

  @NotNull
  @Positive
  private Long userId;

  @NotNull
  @Positive
  private Long orderId;

  private LocalDateTime timestamp;
  @Positive
  @NotNull
  private BigDecimal paymentAmount;

}
