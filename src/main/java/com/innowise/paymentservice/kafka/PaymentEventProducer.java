package com.innowise.paymentservice.kafka;

import com.innowise.paymentservice.model.events.PaymentStatusEvent;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {
  @Value("${KAFKA_PAYMENT_TOPIC_NAME:payment-events}")
  private String topicName;
  private static final Logger logger = LogManager.getLogger(PaymentEventProducer.class);
  private final KafkaTemplate<String, PaymentStatusEvent> kafkaTemplate;

  public void sendPaymentEvent(PaymentStatusEvent event) {
      kafkaTemplate.send(topicName, String.valueOf(event.getOrderId()),event)
              .whenComplete((message, exception) -> {
                if (exception != null) {
                  logger.error("Failed to send payment event for Order ID {}. Reason: {}", event.getOrderId(), exception.getMessage());
                } else {
                  logger.info("Successfully sent payment for order {} with status {}", event.getOrderId(), event.getPaymentStatus());
                }
              });
  }
}
