package com.innowise.paymentservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "random-org")
@Validated
public class RandomOrgProperties {
  @NotBlank
  private String url;
  @Positive
  private int num;
  private int min;
  @Positive
  private int max;
  @Positive
  private int col;
  @Positive
  private int base;
  @NotBlank
  private String format;
  @NotBlank
  private String rnd;
}
