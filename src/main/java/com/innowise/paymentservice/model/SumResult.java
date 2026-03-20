package com.innowise.paymentservice.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

@Data
public class SumResult {
  @Field("amount")
  private BigDecimal amount;
}
