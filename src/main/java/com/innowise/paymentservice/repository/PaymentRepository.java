package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

public  interface PaymentRepository extends MongoRepository<Payment, Long> {
}
