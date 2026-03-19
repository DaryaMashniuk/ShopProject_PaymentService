package com.innowise.paymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.math.BigDecimal;

@Document(collection = "order_info_view")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderInfoView {
  @Id
  private Long orderId;

  @Field("expected_amount")
  private BigDecimal expectedAmount;
}