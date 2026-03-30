package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.model.OrderInfoView;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderInfoRepository extends MongoRepository<OrderInfoView,Long> {
}
