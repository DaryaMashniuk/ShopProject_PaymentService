package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.model.SumResult;
import com.innowise.paymentservice.model.dto.PaymentRequest;
import com.innowise.paymentservice.model.dto.PaymentResponse;
import com.innowise.paymentservice.model.dto.PaymentSearchCriteria;
import com.innowise.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Tests")
class PaymentServiceImplTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private PaymentMapper paymentMapper;

  @Mock
  private MongoTemplate mongoTemplate;

  @Mock
  private AggregationResults<SumResult> aggregationResults;

  @InjectMocks
  private PaymentServiceImpl paymentService;

  @Captor
  private ArgumentCaptor<Payment> paymentCaptor;

  @Captor
  private ArgumentCaptor<Query> queryCaptor;

  @Captor
  private ArgumentCaptor<Aggregation> aggregationCaptor;

  private Payment testPayment;
  private PaymentRequest testPaymentRequest;
  private PaymentResponse testPaymentResponse;
  private PaymentSearchCriteria testCriteria;
  private Pageable pageable;
  private LocalDateTime now;
  private SumResult testSumResult;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();

    testPayment = Payment.builder()
            .id("payment123")
            .userId(1L)
            .orderId(100L)
            .paymentAmount(new BigDecimal("99.99"))
            .status(PaymentStatus.SUCCESS)
            .timestamp(now)
            .build();

    testPaymentRequest = PaymentRequest.builder()
            .userId(1L)
            .orderId(100L)
            .paymentAmount(new BigDecimal("99.99"))
            .timestamp(now)
            .build();

    testPaymentResponse = PaymentResponse.builder()
            .id("payment123")
            .userId(1L)
            .orderId(100L)
            .paymentAmount(new BigDecimal("99.99"))
            .status(PaymentStatus.SUCCESS)
            .timestamp(now)
            .build();

    testCriteria = PaymentSearchCriteria.builder()
            .status(PaymentStatus.SUCCESS.name())
            .userId(1L)
            .orderId(100L)
            .build();

    pageable = PageRequest.of(0, 10);

    testSumResult = new SumResult();
    testSumResult.setAmount(new BigDecimal("299.97"));

    lenient().when(mongoTemplate.getCollectionName(Payment.class)).thenReturn("payments");
  }

  @Nested
  @DisplayName("Create Payment Tests")
  class CreatePaymentTests {

    @Test
    @DisplayName("Should create payment successfully with provided timestamp")
    void shouldCreatePaymentSuccessfullyWithProvidedTimestamp() {
      PaymentStatus status = PaymentStatus.FAILED;

      when(paymentMapper.toEntity(testPaymentRequest)).thenReturn(testPayment);
      when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
      when(paymentMapper.toDto(testPayment)).thenReturn(testPaymentResponse);

      PaymentResponse result = paymentService.createPayment(testPaymentRequest, status);

      assertNotNull(result);
      assertEquals(testPaymentResponse.getId(), result.getId());
      assertEquals(testPaymentResponse.getUserId(), result.getUserId());
      assertEquals(testPaymentResponse.getOrderId(), result.getOrderId());
      assertEquals(status, testPayment.getStatus());
      assertEquals(now, testPayment.getTimestamp());

      verify(paymentMapper).toEntity(testPaymentRequest);
      verify(paymentRepository).save(testPayment);
      verify(paymentMapper).toDto(testPayment);
    }

    @Test
    @DisplayName("Should create payment with current timestamp when timestamp is null")
    void shouldCreatePaymentWithCurrentTimestampWhenTimestampIsNull() {
      PaymentStatus status = PaymentStatus.FAILED;
      testPaymentRequest.setTimestamp(null);
      testPayment.setTimestamp(null);

      when(paymentMapper.toEntity(testPaymentRequest)).thenReturn(testPayment);
      when(paymentRepository.save(paymentCaptor.capture())).thenReturn(testPayment);
      when(paymentMapper.toDto(testPayment)).thenReturn(testPaymentResponse);

      PaymentResponse result = paymentService.createPayment(testPaymentRequest, status);

      assertNotNull(result);
      Payment savedPayment = paymentCaptor.getValue();
      assertNotNull(savedPayment.getTimestamp());
      assertEquals(status, savedPayment.getStatus());

      verify(paymentMapper).toEntity(testPaymentRequest);
      verify(paymentRepository).save(testPayment);
      verify(paymentMapper).toDto(testPayment);
    }

    @Test
    @DisplayName("Should create payment with different status")
    void shouldCreatePaymentWithDifferentStatus() {
      PaymentStatus status = PaymentStatus.FAILED;

      when(paymentMapper.toEntity(testPaymentRequest)).thenReturn(testPayment);
      when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
      when(paymentMapper.toDto(testPayment)).thenReturn(testPaymentResponse);

      PaymentResponse result = paymentService.createPayment(testPaymentRequest, status);

      assertNotNull(result);
      assertEquals(status, testPayment.getStatus());
      verify(paymentRepository).save(testPayment);
    }
  }

  @Nested
  @DisplayName("Find By Criteria Tests")
  class FindByCriteriaTests {

    @Test
    @DisplayName("Should find payments by all criteria successfully")
    void shouldFindPaymentsByAllCriteriaSuccessfully() {
      List<Payment> payments = List.of(testPayment);
      when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(1L);
      when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(payments);
      when(paymentMapper.toDtoList(payments)).thenReturn(List.of(testPaymentResponse));

      Page<PaymentResponse> result = paymentService.findByCriteria(testCriteria, pageable);

      assertNotNull(result);
      assertEquals(1, result.getContent().size());
      assertEquals(testPaymentResponse.getId(), result.getContent().get(0).getId());

      verify(mongoTemplate).count(queryCaptor.capture(), eq(Payment.class));
      verify(mongoTemplate).find(queryCaptor.capture(), eq(Payment.class));

      Query capturedQuery = queryCaptor.getValue();
      assertNotNull(capturedQuery);
    }

    @Test
    @DisplayName("Should find payments by status only")
    void shouldFindPaymentsByStatusOnly() {
      PaymentSearchCriteria criteria = PaymentSearchCriteria.builder()
              .status(PaymentStatus.SUCCESS.name())
              .build();

      List<Payment> payments = List.of(testPayment);
      when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(1L);
      when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(payments);
      when(paymentMapper.toDtoList(payments)).thenReturn(List.of(testPaymentResponse));

      Page<PaymentResponse> result = paymentService.findByCriteria(criteria, pageable);

      assertNotNull(result);
      assertEquals(1, result.getContent().size());
      verify(mongoTemplate).count(any(Query.class), eq(Payment.class));
      verify(mongoTemplate).find(any(Query.class), eq(Payment.class));
    }

    @Test
    @DisplayName("Should find payments by user ID only")
    void shouldFindPaymentsByUserIdOnly() {
      PaymentSearchCriteria criteria = PaymentSearchCriteria.builder()
              .userId(1L)
              .build();

      List<Payment> payments = List.of(testPayment);
      when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(1L);
      when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(payments);
      when(paymentMapper.toDtoList(payments)).thenReturn(List.of(testPaymentResponse));

      Page<PaymentResponse> result = paymentService.findByCriteria(criteria, pageable);

      assertNotNull(result);
      assertEquals(1, result.getContent().size());
      verify(mongoTemplate).count(any(Query.class), eq(Payment.class));
      verify(mongoTemplate).find(any(Query.class), eq(Payment.class));
    }

    @Test
    @DisplayName("Should find payments by order ID only")
    void shouldFindPaymentsByOrderIdOnly() {
      PaymentSearchCriteria criteria = PaymentSearchCriteria.builder()
              .orderId(100L)
              .build();

      List<Payment> payments = List.of(testPayment);
      when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(1L);
      when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(payments);
      when(paymentMapper.toDtoList(payments)).thenReturn(List.of(testPaymentResponse));

      Page<PaymentResponse> result = paymentService.findByCriteria(criteria, pageable);

      assertNotNull(result);
      assertEquals(1, result.getContent().size());
      verify(mongoTemplate).count(any(Query.class), eq(Payment.class));
      verify(mongoTemplate).find(any(Query.class), eq(Payment.class));
    }

    @Test
    @DisplayName("Should return empty page when no payments found")
    void shouldReturnEmptyPageWhenNoPaymentsFound() {
      when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(0L);
      when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(Collections.emptyList());
      when(paymentMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

      Page<PaymentResponse> result = paymentService.findByCriteria(testCriteria, pageable);

      assertNotNull(result);
      assertTrue(result.getContent().isEmpty());
      assertEquals(0, result.getTotalElements());
    }
  }

  @Nested
  @DisplayName("Get Total Sum For User In Range Tests")
  class GetTotalSumForUserInRangeTests {

    @Test
    @DisplayName("Should return total sum for user with both from and to dates")
    void shouldReturnTotalSumForUserWithBothDates() {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = now;

      when(mongoTemplate.aggregate(
              any(Aggregation.class),
              anyString(),
              eq(SumResult.class)
      )).thenReturn(aggregationResults);
      when(aggregationResults.getUniqueMappedResult()).thenReturn(testSumResult);

      BigDecimal result = paymentService.getTotalSumForUserInRange(1L, from, to);

      assertNotNull(result);
      assertEquals(testSumResult.getAmount(), result);

      verify(mongoTemplate).aggregate(
              aggregationCaptor.capture(),
              anyString(),
              eq(SumResult.class)
      );
    }

    @Test
    @DisplayName("Should return total sum for user with from date only")
    void shouldReturnTotalSumForUserWithFromDateOnly() {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = null;

      when(mongoTemplate.aggregate(
              any(Aggregation.class),
              anyString(),
              eq(SumResult.class)
      )).thenReturn(aggregationResults);
      when(aggregationResults.getUniqueMappedResult()).thenReturn(testSumResult);

      BigDecimal result = paymentService.getTotalSumForUserInRange(1L, from, to);

      assertNotNull(result);
      assertEquals(testSumResult.getAmount(), result);
    }

    @Test
    @DisplayName("Should return total sum for user with to date only")
    void shouldReturnTotalSumForUserWithToDateOnly() {
      LocalDateTime from = null;
      LocalDateTime to = now;

      when(mongoTemplate.aggregate(
              any(Aggregation.class),
              anyString(),
              eq(SumResult.class)
      )).thenReturn(aggregationResults);
      when(aggregationResults.getUniqueMappedResult()).thenReturn(testSumResult);

      BigDecimal result = paymentService.getTotalSumForUserInRange(1L, from, to);

      assertNotNull(result);
      assertEquals(testSumResult.getAmount(), result);
    }

    @Test
    @DisplayName("Should return zero when no payments found for user in range")
    void shouldReturnZeroWhenNoPaymentsFoundForUserInRange() {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = now;

      when(mongoTemplate.aggregate(
              any(Aggregation.class),
              anyString(),
              eq(SumResult.class)
      )).thenReturn(aggregationResults);
      when(aggregationResults.getUniqueMappedResult()).thenReturn(null);

      BigDecimal result = paymentService.getTotalSumForUserInRange(1L, from, to);

      assertNotNull(result);
      assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Should handle both from and to dates as null")
    void shouldHandleBothFromAndToDatesAsNull() {
      when(mongoTemplate.aggregate(
              any(Aggregation.class),
              anyString(),
              eq(SumResult.class)
      )).thenReturn(aggregationResults);
      when(aggregationResults.getUniqueMappedResult()).thenReturn(testSumResult);

      BigDecimal result = paymentService.getTotalSumForUserInRange(1L, null, null);

      assertNotNull(result);
      assertEquals(testSumResult.getAmount(), result);
    }
  }

  @Nested
  @DisplayName("Get Total Sum For All Users In Range Tests")
  class GetTotalSumForAllUsersInRangeTests {

    @Test
    @DisplayName("Should return total sum for all users with both from and to dates")
    void shouldReturnTotalSumForAllUsersWithBothDates() {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = now;

      when(mongoTemplate.aggregate(
              any(Aggregation.class),
              anyString(),
              eq(SumResult.class)
      )).thenReturn(aggregationResults);
      when(aggregationResults.getUniqueMappedResult()).thenReturn(testSumResult);

      BigDecimal result = paymentService.getTotalSumForAllUsersInRange(from, to);

      assertNotNull(result);
      assertEquals(testSumResult.getAmount(), result);

      verify(mongoTemplate).aggregate(
              aggregationCaptor.capture(),
              anyString(),
              eq(SumResult.class)
      );
    }

    @Test
    @DisplayName("Should return total sum for all users with from date only")
    void shouldReturnTotalSumForAllUsersWithFromDateOnly() {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = null;

      when(mongoTemplate.aggregate(
              any(Aggregation.class),
              anyString(),
              eq(SumResult.class)
      )).thenReturn(aggregationResults);
      when(aggregationResults.getUniqueMappedResult()).thenReturn(testSumResult);

      BigDecimal result = paymentService.getTotalSumForAllUsersInRange(from, to);

      assertNotNull(result);
      assertEquals(testSumResult.getAmount(), result);
    }

    @Test
    @DisplayName("Should return total sum for all users with to date only")
    void shouldReturnTotalSumForAllUsersWithToDateOnly() {
      LocalDateTime from = null;
      LocalDateTime to = now;

      when(mongoTemplate.aggregate(
              any(Aggregation.class),
              anyString(),
              eq(SumResult.class)
      )).thenReturn(aggregationResults);
      when(aggregationResults.getUniqueMappedResult()).thenReturn(testSumResult);

      BigDecimal result = paymentService.getTotalSumForAllUsersInRange(from, to);

      assertNotNull(result);
      assertEquals(testSumResult.getAmount(), result);
    }

    @Test
    @DisplayName("Should return zero when no payments found in range")
    void shouldReturnZeroWhenNoPaymentsFoundInRange() {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = now;

      when(mongoTemplate.aggregate(
              any(Aggregation.class),
              anyString(),
              eq(SumResult.class)
      )).thenReturn(aggregationResults);
      when(aggregationResults.getUniqueMappedResult()).thenReturn(null);

      BigDecimal result = paymentService.getTotalSumForAllUsersInRange(from, to);

      assertNotNull(result);
      assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Should handle null from and to dates - aggregate all payments")
    void shouldHandleNullFromAndToDates() {
      when(mongoTemplate.aggregate(
              any(Aggregation.class),
              anyString(),
              eq(SumResult.class)
      )).thenReturn(aggregationResults);
      when(aggregationResults.getUniqueMappedResult()).thenReturn(testSumResult);

      BigDecimal result = paymentService.getTotalSumForAllUsersInRange(null, null);

      assertNotNull(result);
      assertEquals(testSumResult.getAmount(), result);

      verify(mongoTemplate).aggregate(
              aggregationCaptor.capture(),
              anyString(),
              eq(SumResult.class)
      );
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("Should create criteria list with both from and to dates")
    void shouldCreateCriteriaListWithBothDates() throws Exception {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = now;

      java.lang.reflect.Method method = PaymentServiceImpl.class.getDeclaredMethod(
              "getPaymentsInRange", LocalDateTime.class, LocalDateTime.class);
      method.setAccessible(true);

      @SuppressWarnings("unchecked")
      List<Criteria> result = (List<Criteria>) method.invoke(paymentService, from, to);

      assertNotNull(result);
      assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should create criteria list with from date only")
    void shouldCreateCriteriaListWithFromDateOnly() throws Exception {
      LocalDateTime from = now.minusDays(7);

      java.lang.reflect.Method method = PaymentServiceImpl.class.getDeclaredMethod(
              "getPaymentsInRange", LocalDateTime.class, LocalDateTime.class);
      method.setAccessible(true);

      @SuppressWarnings("unchecked")
      List<Criteria> result = (List<Criteria>) method.invoke(paymentService, from, null);

      assertNotNull(result);
      assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should create criteria list with to date only")
    void shouldCreateCriteriaListWithToDateOnly() throws Exception {
      LocalDateTime to = now;

      java.lang.reflect.Method method = PaymentServiceImpl.class.getDeclaredMethod(
              "getPaymentsInRange", LocalDateTime.class, LocalDateTime.class);
      method.setAccessible(true);

      @SuppressWarnings("unchecked")
      List<Criteria> result = (List<Criteria>) method.invoke(paymentService, null, to);

      assertNotNull(result);
      assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should return empty list when both dates are null")
    void shouldReturnEmptyListWhenBothDatesAreNull() throws Exception {
      java.lang.reflect.Method method = PaymentServiceImpl.class.getDeclaredMethod(
              "getPaymentsInRange", LocalDateTime.class, LocalDateTime.class);
      method.setAccessible(true);

      @SuppressWarnings("unchecked")
      List<Criteria> result = (List<Criteria>) method.invoke(paymentService, null, null);

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }
}