package com.innowise.paymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
  @Id
  private String id;
  @Indexed
  @Field(name = "user_id")
  private Long userId;
  @Indexed
  @Field(name = "order_id")
  private Long orderId;
  @Indexed
  private PaymentStatus status;
  @Indexed
  private LocalDateTime timestamp;
  @Field(name = "payment_amount")
  private BigDecimal paymentAmount;

}
