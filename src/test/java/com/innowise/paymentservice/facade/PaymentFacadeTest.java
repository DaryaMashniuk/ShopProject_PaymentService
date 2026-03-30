package com.innowise.paymentservice.facade;

import com.innowise.paymentservice.client.PaymentClient;
import com.innowise.paymentservice.config.RandomOrgProperties;
import com.innowise.paymentservice.exceptions.ExternalServiceUnavailableException;
import com.innowise.paymentservice.kafka.PaymentEventProducer;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.model.dto.PaymentRequest;
import com.innowise.paymentservice.model.dto.PaymentResponse;
import com.innowise.paymentservice.model.dto.PaymentSearchCriteria;
import com.innowise.paymentservice.model.events.PaymentStatusEvent;
import com.innowise.paymentservice.service.PaymentService;
import com.innowise.paymentservice.validator.PaymentValidator;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentFacade Unit Tests")
class PaymentFacadeTest {

  @Mock
  private PaymentClient paymentClient;

  @Mock
  private RandomOrgProperties randomOrgProperties;

  @Mock
  private PaymentService paymentService;

  @Mock
  private PaymentEventProducer paymentEventProducer;

  @Mock
  private PaymentValidator paymentValidator;

  @InjectMocks
  private PaymentFacade paymentFacade;

  @Captor
  private ArgumentCaptor<PaymentStatusEvent> eventCaptor;

  @Captor
  private ArgumentCaptor<PaymentStatus> statusCaptor;

  private PaymentRequest testPaymentRequest;
  private PaymentResponse testPaymentResponse;
  private PaymentSearchCriteria testCriteria;
  private Pageable pageable;
  private LocalDateTime now;
  private Page<PaymentResponse> paymentPage;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();

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
    paymentPage = new PageImpl<>(List.of(testPaymentResponse), pageable, 1);
  }

  @Nested
  @DisplayName("Create Payment Tests")
  class CreatePaymentTests {

    @Test
    @DisplayName("Should create payment with SUCCESS status when random number is even")
    void shouldCreatePaymentWithSuccessStatusWhenNumberIsEven() {
      when(randomOrgProperties.getNum()).thenReturn(1);
      when(randomOrgProperties.getMin()).thenReturn(1);
      when(randomOrgProperties.getMax()).thenReturn(100);
      when(randomOrgProperties.getCol()).thenReturn(1);
      when(randomOrgProperties.getBase()).thenReturn(10);
      when(randomOrgProperties.getFormat()).thenReturn("plain");
      when(randomOrgProperties.getRnd()).thenReturn("new");

      when(paymentClient.generateNumber(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString()))
              .thenReturn("42");

      when(paymentService.createPayment(eq(testPaymentRequest), any(PaymentStatus.class)))
              .thenReturn(testPaymentResponse);

      PaymentResponse result = paymentFacade.createPayment(testPaymentRequest);

      assertNotNull(result);
      assertEquals(testPaymentResponse.getId(), result.getId());
      assertEquals(PaymentStatus.SUCCESS, result.getStatus());

      verify(paymentValidator).validatePaymentCreation(testPaymentRequest);
      verify(paymentClient).generateNumber(1, 1, 100, 1, 10, "plain", "new");
      verify(paymentService).createPayment(eq(testPaymentRequest), statusCaptor.capture());
      assertEquals(PaymentStatus.SUCCESS, statusCaptor.getValue());
      verify(paymentEventProducer).sendPaymentEvent(eventCaptor.capture());

      PaymentStatusEvent capturedEvent = eventCaptor.getValue();
      assertEquals(testPaymentRequest.getOrderId(), capturedEvent.getOrderId());
      assertEquals(PaymentStatus.SUCCESS, capturedEvent.getPaymentStatus());
    }

    @Test
    @DisplayName("Should create payment with FAILED status when random number is odd")
    void shouldCreatePaymentWithFailedStatusWhenNumberIsOdd() {
      when(randomOrgProperties.getNum()).thenReturn(1);
      when(randomOrgProperties.getMin()).thenReturn(1);
      when(randomOrgProperties.getMax()).thenReturn(100);
      when(randomOrgProperties.getCol()).thenReturn(1);
      when(randomOrgProperties.getBase()).thenReturn(10);
      when(randomOrgProperties.getFormat()).thenReturn("plain");
      when(randomOrgProperties.getRnd()).thenReturn("new");

      when(paymentClient.generateNumber(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString()))
              .thenReturn("43");

      PaymentResponse failedResponse = PaymentResponse.builder()
              .id("payment123")
              .userId(1L)
              .orderId(100L)
              .paymentAmount(new BigDecimal("99.99"))
              .status(PaymentStatus.FAILED)
              .timestamp(now)
              .build();

      when(paymentService.createPayment(eq(testPaymentRequest), any(PaymentStatus.class)))
              .thenReturn(failedResponse);

      PaymentResponse result = paymentFacade.createPayment(testPaymentRequest);

      assertNotNull(result);
      assertEquals(PaymentStatus.FAILED, result.getStatus());

      verify(paymentService).createPayment(eq(testPaymentRequest), statusCaptor.capture());
      assertEquals(PaymentStatus.FAILED, statusCaptor.getValue());
      verify(paymentEventProducer).sendPaymentEvent(eventCaptor.capture());

      PaymentStatusEvent capturedEvent = eventCaptor.getValue();
      assertEquals(testPaymentRequest.getOrderId(), capturedEvent.getOrderId());
      assertEquals(PaymentStatus.FAILED, capturedEvent.getPaymentStatus());
    }

    @Test
    @DisplayName("Should throw ExternalServiceUnavailableException when random number is null")
    void shouldThrowExceptionWhenRandomNumberIsNull() {
      when(randomOrgProperties.getNum()).thenReturn(1);
      when(randomOrgProperties.getMin()).thenReturn(1);
      when(randomOrgProperties.getMax()).thenReturn(100);
      when(randomOrgProperties.getCol()).thenReturn(1);
      when(randomOrgProperties.getBase()).thenReturn(10);
      when(randomOrgProperties.getFormat()).thenReturn("plain");
      when(randomOrgProperties.getRnd()).thenReturn("new");

      when(paymentClient.generateNumber(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString()))
              .thenReturn(null);

      assertThrows(ExternalServiceUnavailableException.class,
              () -> paymentFacade.createPayment(testPaymentRequest));

      verify(paymentValidator).validatePaymentCreation(testPaymentRequest);
      verify(paymentService, never()).createPayment(any(), any());
      verify(paymentEventProducer, never()).sendPaymentEvent(any());
    }

    @Test
    @DisplayName("Should throw ExternalServiceUnavailableException when random number is blank")
    void shouldThrowExceptionWhenRandomNumberIsBlank() {
      when(randomOrgProperties.getNum()).thenReturn(1);
      when(randomOrgProperties.getMin()).thenReturn(1);
      when(randomOrgProperties.getMax()).thenReturn(100);
      when(randomOrgProperties.getCol()).thenReturn(1);
      when(randomOrgProperties.getBase()).thenReturn(10);
      when(randomOrgProperties.getFormat()).thenReturn("plain");
      when(randomOrgProperties.getRnd()).thenReturn("new");

      when(paymentClient.generateNumber(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString()))
              .thenReturn("   ");

      assertThrows(ExternalServiceUnavailableException.class,
              () -> paymentFacade.createPayment(testPaymentRequest));

      verify(paymentService, never()).createPayment(any(), any());
    }

    @Test
    @DisplayName("Should throw ExternalServiceUnavailableException when random number is not a number")
    void shouldThrowExceptionWhenRandomNumberIsNotANumber() {
      when(randomOrgProperties.getNum()).thenReturn(1);
      when(randomOrgProperties.getMin()).thenReturn(1);
      when(randomOrgProperties.getMax()).thenReturn(100);
      when(randomOrgProperties.getCol()).thenReturn(1);
      when(randomOrgProperties.getBase()).thenReturn(10);
      when(randomOrgProperties.getFormat()).thenReturn("plain");
      when(randomOrgProperties.getRnd()).thenReturn("new");

      when(paymentClient.generateNumber(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString()))
              .thenReturn("abc");

      assertThrows(ExternalServiceUnavailableException.class,
              () -> paymentFacade.createPayment(testPaymentRequest));

      verify(paymentService, never()).createPayment(any(), any());
    }
  }

  @Nested
  @DisplayName("Find By Criteria Tests")
  class FindByCriteriaTests {

    @Test
    @DisplayName("Should find payments by criteria successfully")
    void shouldFindPaymentsByCriteriaSuccessfully() {
      when(paymentService.findByCriteria(testCriteria, pageable)).thenReturn(paymentPage);

      Page<PaymentResponse> result = paymentFacade.findByCriteria(testCriteria, pageable);

      assertNotNull(result);
      assertEquals(1, result.getContent().size());
      assertEquals(testPaymentResponse.getId(), result.getContent().get(0).getId());
      verify(paymentService).findByCriteria(testCriteria, pageable);
    }

    @Test
    @DisplayName("Should handle null criteria")
    void shouldHandleNullCriteria() {
      when(paymentService.findByCriteria(null, pageable)).thenReturn(Page.empty());

      Page<PaymentResponse> result = paymentFacade.findByCriteria(null, pageable);

      assertNotNull(result);
      assertTrue(result.getContent().isEmpty());
      verify(paymentService).findByCriteria(null, pageable);
    }

    @Test
    @DisplayName("Should return empty page when no payments found")
    void shouldReturnEmptyPageWhenNoPaymentsFound() {
      Page<PaymentResponse> emptyPage = Page.empty();
      when(paymentService.findByCriteria(testCriteria, pageable)).thenReturn(emptyPage);

      Page<PaymentResponse> result = paymentFacade.findByCriteria(testCriteria, pageable);

      assertNotNull(result);
      assertTrue(result.getContent().isEmpty());
      verify(paymentService).findByCriteria(testCriteria, pageable);
    }
  }

  @Nested
  @DisplayName("Get Total Sum For User In Range Tests")
  class GetTotalSumForUserInRangeTests {

    @Test
    @DisplayName("Should return total sum for user with date range")
    void shouldReturnTotalSumForUserWithDateRange() {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = now;
      BigDecimal expectedSum = new BigDecimal("299.97");

      when(paymentService.getTotalSumForUserInRange(1L, from, to)).thenReturn(expectedSum);

      BigDecimal result = paymentFacade.getTotalSumForUserInRange(1L, from, to);

      assertNotNull(result);
      assertEquals(expectedSum, result);
      verify(paymentService).getTotalSumForUserInRange(1L, from, to);
    }

    @Test
    @DisplayName("Should return zero when no payments found for user")
    void shouldReturnZeroWhenNoPaymentsFoundForUser() {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = now;
      BigDecimal expectedSum = BigDecimal.ZERO;

      when(paymentService.getTotalSumForUserInRange(1L, from, to)).thenReturn(expectedSum);

      BigDecimal result = paymentFacade.getTotalSumForUserInRange(1L, from, to);

      assertNotNull(result);
      assertEquals(BigDecimal.ZERO, result);
      verify(paymentService).getTotalSumForUserInRange(1L, from, to);
    }

    @Test
    @DisplayName("Should handle null dates for user sum")
    void shouldHandleNullDatesForUserSum() {
      BigDecimal expectedSum = new BigDecimal("299.97");

      when(paymentService.getTotalSumForUserInRange(1L, null, null)).thenReturn(expectedSum);

      BigDecimal result = paymentFacade.getTotalSumForUserInRange(1L, null, null);

      assertNotNull(result);
      assertEquals(expectedSum, result);
      verify(paymentService).getTotalSumForUserInRange(1L, null, null);
    }
  }

  @Nested
  @DisplayName("Get Total Sum For All Users In Range Tests")
  class GetTotalSumForAllUsersInRangeTests {

    @Test
    @DisplayName("Should return total sum for all users with date range")
    void shouldReturnTotalSumForAllUsersWithDateRange() {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = now;
      BigDecimal expectedSum = new BigDecimal("599.94");

      when(paymentService.getTotalSumForAllUsersInRange(from, to)).thenReturn(expectedSum);

      BigDecimal result = paymentFacade.getTotalSumForAllUsersInRange(from, to);

      assertNotNull(result);
      assertEquals(expectedSum, result);
      verify(paymentService).getTotalSumForAllUsersInRange(from, to);
    }

    @Test
    @DisplayName("Should return zero when no payments found for all users")
    void shouldReturnZeroWhenNoPaymentsFoundForAllUsers() {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = now;
      BigDecimal expectedSum = BigDecimal.ZERO;

      when(paymentService.getTotalSumForAllUsersInRange(from, to)).thenReturn(expectedSum);

      BigDecimal result = paymentFacade.getTotalSumForAllUsersInRange(from, to);

      assertNotNull(result);
      assertEquals(BigDecimal.ZERO, result);
      verify(paymentService).getTotalSumForAllUsersInRange(from, to);
    }

    @Test
    @DisplayName("Should handle null dates for all users sum")
    void shouldHandleNullDatesForAllUsersSum() {
      BigDecimal expectedSum = new BigDecimal("599.94");

      when(paymentService.getTotalSumForAllUsersInRange(null, null)).thenReturn(expectedSum);

      BigDecimal result = paymentFacade.getTotalSumForAllUsersInRange(null, null);

      assertNotNull(result);
      assertEquals(expectedSum, result);
      verify(paymentService).getTotalSumForAllUsersInRange(null, null);
    }
  }

}