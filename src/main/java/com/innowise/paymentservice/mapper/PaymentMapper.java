package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.dto.PaymentRequest;
import com.innowise.paymentservice.model.dto.PaymentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        componentModel = "spring"
)
public interface PaymentMapper {

  Payment toEntity(PaymentRequest paymentRequest);

  PaymentResponse toDto(Payment payment);

  List<PaymentResponse> toDtoList(List<Payment> payments);
}
