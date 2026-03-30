package com.innowise.paymentservice.model.events;

import com.innowise.paymentservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStatusEvent {

  private Long orderId;
  private PaymentStatus paymentStatus;

}
