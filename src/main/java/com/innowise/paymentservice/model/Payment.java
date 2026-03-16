package com.innowise.paymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
  @Id
  private Long id;
  @Field(name = "user_id")
  private Long userId;
  @Field(name = "order_id")
  private Long orderId;

  private PaymentStatus status;

  private LocalDateTime timestamp;
  @Field(name = "payment_amount")
  private BigDecimal paymentAmount;

}
