package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.model.dto.PaymentRequest;
import com.innowise.paymentservice.model.dto.PaymentResponse;

import com.innowise.paymentservice.model.dto.PaymentSearchCriteria;
import com.innowise.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping
  @PreAuthorize("@authorisationService.isSelf(#paymentRequest.userId, authentication)")
  public ResponseEntity<PaymentResponse> createPayment(
          @RequestBody @Valid PaymentRequest paymentRequest) {
    PaymentResponse response = paymentService.createPayment(paymentRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize(
          "@authorisationService.hasAdminRole(authentication) or " +
                  "@authorisationService.isSelf(#userId, authentication)"
  )
  public ResponseEntity<Page<PaymentResponse>> getPaymentsFilteredByFields(
          @RequestParam(required = false) Long userId,
          @RequestParam(required = false) Long orderId,
          @RequestParam(required = false) String status,
          Pageable pageable
  ){
    PaymentSearchCriteria criteria = new PaymentSearchCriteria(userId,orderId,status);
    Page<PaymentResponse> response = paymentService.findByCriteria(criteria, pageable);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping("/users/{userId}/total")
  @PreAuthorize(
          "@authorisationService.hasAdminRole(authentication) or " +
                  "@authorisationService.isSelf(#userId, authentication)"
  )
  public ResponseEntity<BigDecimal> getPaymentAmountForCurrentUser(
          @PathVariable Long userId,
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
          ) {
    BigDecimal response = paymentService.getTotalSumForUserInRange(userId,from,to);
    return ResponseEntity.ok().body(response);
  }

  @GetMapping("/total")
  @PreAuthorize("@authorisationService.hasAdminRole(authentication)")
  public ResponseEntity<BigDecimal> getPaymentAmountOfUsersInRange(
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
  ) {
    BigDecimal response = paymentService.getTotalSumForAllUsersInRange(from,to);
    return ResponseEntity.ok().body(response);
  }

}
