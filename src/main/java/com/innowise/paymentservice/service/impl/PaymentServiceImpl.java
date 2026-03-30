package com.innowise.paymentservice.service.impl;


import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.model.SumResult;
import com.innowise.paymentservice.model.dto.PaymentRequest;
import com.innowise.paymentservice.model.dto.PaymentResponse;
import com.innowise.paymentservice.model.dto.PaymentSearchCriteria;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentMapper paymentMapper;
  private final MongoTemplate mongoTemplate;
  private static final String AMOUNT_FIELD = "amount";
  private static final String PAYMENT_AMOUNT_FIELD = "payment_amount";
  private static final String STATUS_FIELD = "status";
  private static final String TIMESTAMP_FIELD = "timestamp";

  @Override
  public PaymentResponse createPayment(PaymentRequest paymentRequest, PaymentStatus status) {
    Payment payment = paymentMapper.toEntity(paymentRequest);

    payment.setStatus(status);
    if (payment.getTimestamp() == null) {
      payment.setTimestamp(LocalDateTime.now());
    }
    Payment response = paymentRepository.save(payment);
    return paymentMapper.toDto(response);
  }

  @Transactional(readOnly = true)
  @Override
  public Page<PaymentResponse> findByCriteria(PaymentSearchCriteria criteria,Pageable pageable) {

    Query query = new Query();
    if (criteria.getStatus() != null) {
      PaymentStatus enumStatus = PaymentStatus.valueOf(criteria.getStatus());
      query.addCriteria(Criteria.where(STATUS_FIELD).is(enumStatus));
    }

    if (criteria.getUserId() != null) {
      query.addCriteria(Criteria.where("user_id").is(criteria.getUserId()));
    }
    if (criteria.getOrderId() != null) {
      query.addCriteria(Criteria.where("order_id").is(criteria.getOrderId()));
    }
    long count = mongoTemplate.count(query,Payment.class);
    query.with(pageable);
    List<Payment> payments = mongoTemplate.find(query, Payment.class);
    return new PageImpl<>(paymentMapper.toDtoList(payments), pageable, count);
  }

  @Transactional(readOnly = true)
  @Override
  public BigDecimal getTotalSumForUserInRange(long userId, LocalDateTime from, LocalDateTime to) {
    List<Criteria> criteriaList = getPaymentsInRange(from,to);
    criteriaList.add(Criteria.where("user_id").is(userId));
    criteriaList.add(Criteria.where(STATUS_FIELD).is(PaymentStatus.SUCCESS));
    Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(new Criteria().andOperator(criteriaList)),
            Aggregation.group()
                    .sum(PAYMENT_AMOUNT_FIELD)
                    .as(AMOUNT_FIELD)
    );
    AggregationResults<SumResult> results = mongoTemplate.aggregate(
            aggregation,
            mongoTemplate.getCollectionName(Payment.class),
            SumResult.class);
    return Optional.ofNullable(results.getUniqueMappedResult())
            .map(SumResult::getAmount)
            .orElse(BigDecimal.ZERO);
  }

  @Transactional(readOnly = true)
  @Override
  public BigDecimal getTotalSumForAllUsersInRange(LocalDateTime from, LocalDateTime to) {
    List<Criteria> criteriaList = getPaymentsInRange(from,to);
    Aggregation aggregation;
    if (criteriaList.isEmpty()) {
      aggregation = Aggregation.newAggregation(
              Aggregation.match(Criteria.where(STATUS_FIELD).is(PaymentStatus.SUCCESS)),
              Aggregation.group()
                      .sum(PAYMENT_AMOUNT_FIELD)
                      .as(AMOUNT_FIELD)
      );
    } else {
      aggregation = Aggregation.newAggregation(
              Aggregation.match(new Criteria().andOperator(criteriaList)),
              Aggregation.match(Criteria.where(STATUS_FIELD).is(PaymentStatus.SUCCESS)),
              Aggregation.group()
                      .sum(PAYMENT_AMOUNT_FIELD)
                      .as(AMOUNT_FIELD)
      );
    }

    AggregationResults<SumResult> results = mongoTemplate.aggregate(
            aggregation,
            mongoTemplate.getCollectionName(Payment.class),
            SumResult.class);
    return Optional.ofNullable(results.getUniqueMappedResult())
            .map(SumResult::getAmount)
            .orElse(BigDecimal.ZERO);
  }

  private List<Criteria> getPaymentsInRange(LocalDateTime from, LocalDateTime to) {
    List<Criteria> criterias = new ArrayList<>();
    if (from != null && to != null) {
      criterias.add(Criteria.where(TIMESTAMP_FIELD).gte(from).lte(to));
    } else if (from != null) {
      criterias.add(Criteria.where(TIMESTAMP_FIELD).gte(from));
    } else if (to != null) {
      criterias.add(Criteria.where(TIMESTAMP_FIELD).lte(to));
    }
    return criterias;
  }
}
