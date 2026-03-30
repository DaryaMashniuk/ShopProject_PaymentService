package com.innowise.paymentservice.model.dto;

import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.validator.ValidEnum;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PaymentSearchCriteria {
  @Positive
  private Long userId;

  @Positive
  private Long orderId;

  @ValidEnum(enumClass = PaymentStatus.class, message = "Invalid payment status")
  private String status;

}
