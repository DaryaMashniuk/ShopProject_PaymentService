package com.innowise.paymentservice.kafka;

import com.innowise.paymentservice.model.OrderInfoView;
import com.innowise.paymentservice.model.events.OrderCreatedEvent;
import com.innowise.paymentservice.repository.OrderInfoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventConsumer {
  private final OrderInfoRepository repository;
  private final Logger logger = LogManager.getLogger(OrderEventConsumer.class);

  @KafkaListener(topics = "${KAFKA_ORDER_TOPIC_NAME:order-events}")
  public void handleOrderCreated(OrderCreatedEvent event) {
    OrderInfoView view = new OrderInfoView(event.getOrderId(), event.getTotalPrice());
    repository.save(view);
    logger.info("Saved order info view for validation: Order ID {}, Amount {}",
            event.getOrderId(), event.getTotalPrice());
  }
}
