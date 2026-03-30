package com.innowise.paymentservice.service;

import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.model.dto.PaymentRequest;
import com.innowise.paymentservice.model.dto.PaymentResponse;
import com.innowise.paymentservice.model.dto.PaymentSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service interface for managing payment operations.
 * Provides methods for creating, retrieving, and aggregating payment data.
 *
 * This service handles the core business logic for payment processing,
 * including persistence, searching, and statistical calculations.
 *
 * @author Innowise Group
 * @version 1.0
 * @see PaymentRequest
 * @see PaymentResponse
 * @see PaymentSearchCriteria
 */
public interface PaymentService {

  /**
   * Creates a new payment record in the system.
   *
   * This method persists the payment data along with the provided status.
   * The payment status is determined externally before calling this method.
   *
   * @param paymentRequest the payment request containing payment details
   *                       (userId, orderId, timestamp, paymentAmount)
   * @param status the payment status determined by external random number API
   *              (SUCCESS for even numbers, FAILED for odd numbers)
   * @return PaymentResponse containing the persisted payment data with generated ID
   * @throws IllegalArgumentException if paymentRequest is null or contains invalid data
   * @throws com.innowise.paymentservice.exceptions.ExternalServiceUnavailableException
   *         if external services are unavailable during processing
   */
  PaymentResponse createPayment(PaymentRequest paymentRequest, PaymentStatus status);

  /**
   * Retrieves a paginated list of payments based on search criteria.
   *
   * Supports filtering by multiple criteria combined with AND logic:
   * <ul>
   *   <li>User ID - filters payments by specific user</li>
   *   <li>Order ID - filters payments by associated order</li>
   *   <li>Status - filters payments by SUCCESS/FAILED status</li>
   * </ul>
   *
   * Results are paginated to efficiently handle large datasets.
   *
   * @param criteria the search criteria containing optional filters
   *                (userId, orderId, status)
   * @param pageable pagination parameters including page number, page size, and sorting
   * @return a Page of PaymentResponse objects matching the criteria
   * @throws IllegalArgumentException if criteria or pageable is null
   */
  Page<PaymentResponse> findByCriteria(PaymentSearchCriteria criteria, Pageable pageable);

  /**
   * Calculates the total sum of all successful payments for a specific user.
   *
   * The calculation can be filtered by date range:
   * <ul>
   *   <li>If both from and to are provided: payments between these dates (inclusive)</li>
   *   <li>If only from is provided: payments from that date onwards</li>
   *   <li>If only to is provided: payments up to that date</li>
   *   <li>If neither is provided: all payments for the user</li>
   * </ul>
   *
   * @param userId the ID of the user to calculate total for
   * @param from optional start of date range (inclusive)
   * @param to optional end of date range (inclusive)
   * @return total sum of payments as BigDecimal, or ZERO if no payments found
   * @throws IllegalArgumentException if userId is negative
   */
  BigDecimal getTotalSumForUserInRange(long userId, LocalDateTime from, LocalDateTime to);

  /**
   * Calculates the total sum of all successful payments across all users.
   *
   * This method provides system-wide financial statistics and is typically
   * used for administrative reporting and analytics. Supports optional
   * date range filtering similar to {@link #getTotalSumForUserInRange}.
   *
   * @param from optional start of date range (inclusive)
   * @param to optional end of date range (inclusive)
   * @return total sum of all payments as BigDecimal, or ZERO if no payments found
   */
  BigDecimal getTotalSumForAllUsersInRange(LocalDateTime from, LocalDateTime to);
}