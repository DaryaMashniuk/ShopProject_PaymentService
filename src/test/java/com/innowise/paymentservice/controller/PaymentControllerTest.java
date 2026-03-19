package com.innowise.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.paymentservice.AbstractIntegrationTest;
import com.innowise.paymentservice.model.OrderInfoView;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.model.dto.PaymentRequest;
import com.innowise.paymentservice.model.dto.PaymentResponse;
import com.innowise.paymentservice.repository.OrderInfoRepository;
import com.innowise.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import static org.hamcrest.Matchers.is;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;


@EmbeddedKafka(partitions = 1, topics = {"payment-events", "order-created-topic"})
@TestPropertySource(properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=test-group",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "random-org.num=1",
        "random-org.min=1",
        "random-org.max=100",
        "random-org.col=1",
        "random-org.base=10",
        "random-org.format=plain",
        "random-org.rnd=new"
})
class PaymentControllerTest extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private OrderInfoRepository orderInfoRepository;

  private ObjectMapper objectMapper;
  private PaymentRequest testPaymentRequest;
  private LocalDateTime now;
  private DateTimeFormatter dateTimeFormatter;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    now = LocalDateTime.now().withNano(0);
    dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;

    testPaymentRequest = PaymentRequest.builder()
            .userId(1L)
            .orderId(100L)
            .paymentAmount(new BigDecimal("99.99"))
            .timestamp(now)
            .build();

    paymentRepository.deleteAll();
    orderInfoRepository.deleteAll();

    WireMock.reset();
  }

  private void setupWireMockStub(int statusCode, String responseBody) {
    stubFor(WireMock.get(urlPathEqualTo("/integers/"))
            .withQueryParam("num", equalTo("1"))
            .withQueryParam("min", equalTo("1"))
            .withQueryParam("max", equalTo("100"))
            .withQueryParam("col", equalTo("1"))
            .withQueryParam("base", equalTo("10"))
            .withQueryParam("format", equalTo("plain"))
            .withQueryParam("rnd", equalTo("new"))
            .willReturn(aResponse()
                    .withStatus(statusCode)
                    .withHeader("Content-Type", "text/plain")
                    .withBody(responseBody)));
  }

  @Nested
  @DisplayName("Create Payment Integration Tests")
  class CreatePaymentIntegrationTests {

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should create payment successfully with even random number")
    void shouldCreatePaymentSuccessfullyWithEvenNumber() throws Exception {
      orderInfoRepository.save(new OrderInfoView(100L, new BigDecimal("99.99")));

      setupWireMockStub(200, "42");

      MvcResult result = mockMvc.perform(post("/api/v1/payments")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(testPaymentRequest)))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.id").isNotEmpty())
              .andExpect(jsonPath("$.userId").value(1))
              .andExpect(jsonPath("$.orderId").value(100))
              .andExpect(jsonPath("$.paymentAmount").value(99.99))
              .andExpect(jsonPath("$.status").value(PaymentStatus.SUCCESS.name()))
              .andExpect(jsonPath("$.timestamp").exists())
              .andReturn();

      PaymentResponse response = objectMapper.readValue(
              result.getResponse().getContentAsString(),
              PaymentResponse.class
      );

      assertThat(paymentRepository.findById(response.getId())).isPresent();

      WireMock.verify(getRequestedFor(urlPathEqualTo("/integers/"))
              .withQueryParam("num", equalTo("1"))
              .withQueryParam("min", equalTo("1"))
              .withQueryParam("max", equalTo("100"))
              .withQueryParam("col", equalTo("1"))
              .withQueryParam("base", equalTo("10"))
              .withQueryParam("format", equalTo("plain"))
              .withQueryParam("rnd", equalTo("new")));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should create payment with FAILED status when random number is odd")
    void shouldCreatePaymentWithFailedStatusWhenNumberIsOdd() throws Exception {
      orderInfoRepository.save(new OrderInfoView(100L, new BigDecimal("99.99")));

      setupWireMockStub(200, "43");

      mockMvc.perform(post("/api/v1/payments")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(testPaymentRequest)))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.status").value(PaymentStatus.FAILED.name()));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should return 400 when amount mismatches")
    void shouldReturn400WhenAmountMismatches() throws Exception {
      orderInfoRepository.save(new OrderInfoView(100L, new BigDecimal("150.00")));

      mockMvc.perform(post("/api/v1/payments")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(testPaymentRequest)))
              .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should return 404 when order not found")
    void shouldReturn404WhenOrderNotFound() throws Exception {
      mockMvc.perform(post("/api/v1/payments")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(testPaymentRequest)))
              .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should return 503 when external service is unavailable")
    void shouldReturn503WhenExternalServiceUnavailable() throws Exception {
      orderInfoRepository.save(new OrderInfoView(100L, new BigDecimal("99.99")));

      setupWireMockStub(500, "");

      mockMvc.perform(post("/api/v1/payments")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(testPaymentRequest)))
              .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should return 400 when payment request is invalid")
    void shouldReturn400WhenPaymentRequestIsInvalid() throws Exception {
      PaymentRequest invalidRequest = PaymentRequest.builder()
              .userId(null)
              .orderId(null)
              .paymentAmount(null)
              .timestamp(null)
              .build();

      mockMvc.perform(post("/api/v1/payments")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(invalidRequest)))
              .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 403 when user tries to create payment for another user")
    void shouldReturn403WhenUserCreatesPaymentForAnotherUser() throws Exception {
      orderInfoRepository.save(new OrderInfoView(100L, new BigDecimal("99.99")));

      mockMvc.perform(post("/api/v1/payments")
                      .with(user("2"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(testPaymentRequest)))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Get Payments Filtered Integration Tests")
  class GetPaymentsFilteredIntegrationTests {

    @BeforeEach
    void setUpData() throws Exception {
      createTestPayment(1L, 100L, PaymentStatus.SUCCESS, now.minusDays(5));
      createTestPayment(1L, 101L, PaymentStatus.FAILED, now.minusDays(3));
      createTestPayment(2L, 102L, PaymentStatus.SUCCESS, now.minusDays(1));
      createTestPayment(2L, 103L, PaymentStatus.SUCCESS, now);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get all payments with pagination")
    void shouldGetAllPaymentsWithPagination() throws Exception {
      mockMvc.perform(get("/api/v1/payments")
                      .param("page", "0")
                      .param("size", "10"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.content", hasSize(4)))
              .andExpect(jsonPath("$.totalElements", is(4)))
              .andExpect(jsonPath("$.totalPages", is(1)))
              .andExpect(jsonPath("$.size", is(10)))
              .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should get payments filtered by user ID for self")
    void shouldGetPaymentsFilteredByUserIdForSelf() throws Exception {
      mockMvc.perform(get("/api/v1/payments")
                      .param("userId", "1")
                      .param("page", "0")
                      .param("size", "10"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.content", hasSize(2)))
              .andExpect(jsonPath("$.content[0].userId", is(1)))
              .andExpect(jsonPath("$.content[1].userId", is(1)));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should return 403 when user tries to get payments for another user")
    void shouldReturn403WhenUserGetsPaymentsForAnotherUser() throws Exception {
      mockMvc.perform(get("/api/v1/payments")
                      .param("userId", "2"))
              .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get payments filtered by status")
    void shouldGetPaymentsFilteredByStatus() throws Exception {
      mockMvc.perform(get("/api/v1/payments")
                      .param("status", PaymentStatus.SUCCESS.name()))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.content[*].status", everyItem(is(PaymentStatus.SUCCESS.name()))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get payments filtered by order ID")
    void shouldGetPaymentsFilteredByOrderId() throws Exception {
      mockMvc.perform(get("/api/v1/payments")
                      .param("orderId", "100"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.content", hasSize(1)))
              .andExpect(jsonPath("$.content[0].orderId", is(100)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get payments with multiple filters")
    void shouldGetPaymentsWithMultipleFilters() throws Exception {
      mockMvc.perform(get("/api/v1/payments")
                      .param("userId", "1")
                      .param("status", PaymentStatus.SUCCESS.name()))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.content", hasSize(1)))
              .andExpect(jsonPath("$.content[0].userId", is(1)))
              .andExpect(jsonPath("$.content[0].status", is(PaymentStatus.SUCCESS.name())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return empty page when no payments match filters")
    void shouldReturnEmptyPageWhenNoPaymentsMatchFilters() throws Exception {
      mockMvc.perform(get("/api/v1/payments")
                      .param("userId", "999"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.content", hasSize(0)))
              .andExpect(jsonPath("$.totalElements", is(0)));
    }
  }

  @Nested
  @DisplayName("Get Payment Amount For User Integration Tests")
  class GetPaymentAmountForUserIntegrationTests {

    @BeforeEach
    void setUp() throws Exception {

      paymentRepository.deleteAll();
      orderInfoRepository.deleteAll();

      createTestPayment(1L, 100L, PaymentStatus.SUCCESS, now.minusDays(10));
      createTestPayment(1L, 101L, PaymentStatus.SUCCESS, now.minusDays(5));
      createTestPayment(1L, 102L, PaymentStatus.SUCCESS, now.minusDays(2));
      createTestPayment(2L, 103L, PaymentStatus.SUCCESS, now.minusDays(1));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should get total amount for user with date range")
    void shouldGetTotalAmountForUserWithDateRange() throws Exception {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = now;

      mockMvc.perform(get("/api/v1/payments/users/{userId}/total", 1L)
                      .param("from", from.format(dateTimeFormatter))
                      .param("to", to.format(dateTimeFormatter)))
              .andExpect(status().isOk())
              .andExpect(content().string("199.98"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should get total amount for user without date range")
    void shouldGetTotalAmountForUserWithoutDateRange() throws Exception {
      mockMvc.perform(get("/api/v1/payments/users/{userId}/total", 1L))
              .andExpect(status().isOk())
              .andExpect(content().string("299.97"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should return zero when user has no payments in range")
    void shouldReturnZeroWhenUserHasNoPaymentsInRange() throws Exception {
      LocalDateTime from = now.plusDays(1);
      LocalDateTime to = now.plusDays(7);

      mockMvc.perform(get("/api/v1/payments/users/{userId}/total", 1L)
                      .param("from", from.format(dateTimeFormatter))
                      .param("to", to.format(dateTimeFormatter)))
              .andExpect(status().isOk())
              .andExpect(content().string("0"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should allow admin to get total for any user")
    void shouldAllowAdminToGetTotalForAnyUser() throws Exception {
      mockMvc.perform(get("/api/v1/payments/users/{userId}/total", 2L))
              .andExpect(status().isOk())
              .andExpect(content().string("99.99"));
    }

    @Test
    @WithMockUser(username = "2")
    @DisplayName("Should return 403 when user tries to get total for another user")
    void shouldReturn403WhenUserGetsTotalForAnotherUser() throws Exception {
      mockMvc.perform(get("/api/v1/payments/users/{userId}/total", 1L))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Get Total Amount For All Users Integration Tests")
  class GetTotalAmountForAllUsersIntegrationTests {

    @BeforeEach
    void setUp() throws Exception {
      paymentRepository.deleteAll();
      orderInfoRepository.deleteAll();

      createTestPayment(1L, 100L, PaymentStatus.SUCCESS, now.minusDays(10));
      createTestPayment(1L, 101L, PaymentStatus.SUCCESS, now.minusDays(5));
      createTestPayment(2L, 102L, PaymentStatus.SUCCESS, now.minusDays(2));
      createTestPayment(3L, 103L, PaymentStatus.FAILED, now.minusDays(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get total amount for all users with date range")
    void shouldGetTotalAmountForAllUsersWithDateRange() throws Exception {
      LocalDateTime from = now.minusDays(7);
      LocalDateTime to = now;

      mockMvc.perform(get("/api/v1/payments/total")
                      .param("from", from.format(dateTimeFormatter))
                      .param("to", to.format(dateTimeFormatter)))
              .andExpect(status().isOk())
              .andExpect(content().string("199.98"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get total amount for all users without date range")
    void shouldGetTotalAmountForAllUsersWithoutDateRange() throws Exception {
      mockMvc.perform(get("/api/v1/payments/total"))
              .andExpect(status().isOk())
              .andExpect(content().string("299.97"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return zero when no payments in range")
    void shouldReturnZeroWhenNoPaymentsInRange() throws Exception {
      LocalDateTime from = now.plusDays(1);
      LocalDateTime to = now.plusDays(7);

      mockMvc.perform(get("/api/v1/payments/total")
                      .param("from", from.format(dateTimeFormatter))
                      .param("to", to.format(dateTimeFormatter)))
              .andExpect(status().isOk())
              .andExpect(content().string("0"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle from date only")
    void shouldHandleFromDateOnly() throws Exception {
      LocalDateTime from = now.minusDays(5);

      mockMvc.perform(get("/api/v1/payments/total")
                      .param("from", from.format(dateTimeFormatter)))
              .andExpect(status().isOk())
              .andExpect(content().string("199.98"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle to date only")
    void shouldHandleToDateOnly() throws Exception {
      LocalDateTime to = now.minusDays(5);

      mockMvc.perform(get("/api/v1/payments/total")
                      .param("to", to.format(dateTimeFormatter)))
              .andExpect(status().isOk())
              .andExpect(content().string("199.98"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("Should return 403 when non-admin tries to get total for all users")
    void shouldReturn403WhenNonAdminTriesToGetTotalForAllUsers() throws Exception {
      mockMvc.perform(get("/api/v1/payments/total"))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Error Handling Integration Tests")
  class ErrorHandlingIntegrationTests {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when endpoint not found")
    void shouldReturn404WhenEndpointNotFound() throws Exception {
      mockMvc.perform(get("/api/v1/payments/non-existent"))
              .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle invalid date format gracefully")
    void shouldHandleInvalidDateFormatGracefully() throws Exception {
      mockMvc.perform(get("/api/v1/payments/total")
                      .param("from", "invalid-date"))
              .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle pagination parameters correctly")
    void shouldHandlePaginationParametersCorrectly() throws Exception {
      paymentRepository.deleteAll();
      orderInfoRepository.deleteAll();

      for (int i = 0; i < 25; i++) {
        createTestPayment(1L, 100L + i, PaymentStatus.SUCCESS, now);
      }

      mockMvc.perform(get("/api/v1/payments")
                      .param("page", "1")
                      .param("size", "10"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.number", is(1)))
              .andExpect(jsonPath("$.size", is(10)))
              .andExpect(jsonPath("$.totalPages", is(3)))
              .andExpect(jsonPath("$.totalElements", is(25)));
    }
  }

  private void createTestPayment(Long userId, Long orderId, PaymentStatus status, LocalDateTime timestamp) throws Exception {
    orderInfoRepository.save(new OrderInfoView(orderId, new BigDecimal("99.99")));

    setupWireMockStub(200, status == PaymentStatus.SUCCESS ? "42" : "43");

    PaymentRequest request = PaymentRequest.builder()
            .userId(userId)
            .orderId(orderId)
            .paymentAmount(new BigDecimal("99.99"))
            .timestamp(timestamp)
            .build();

    mockMvc.perform(post("/api/v1/payments")
                    .with(user(userId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
  }
}