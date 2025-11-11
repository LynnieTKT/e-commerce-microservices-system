package com.cs6650.group13.shoppingcart.messaging;

import com.cs6650.group13.shoppingcart.model.ShoppingCart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class OrderMessageProducer {

  private static final Logger logger = LoggerFactory.getLogger(OrderMessageProducer.class);

  private final RabbitTemplate rabbitTemplate;

  @Value("${app.rabbitmq.exchange-name}")
  private String exchangeName;

  @Value("${app.rabbitmq.routing-key}")
  private String routingKey;

  public OrderMessageProducer(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Send order message to warehouse queue
   * @param orderId The order ID
   * @param cart The shopping cart containing items
   * @return true if message sent successfully
   */
  public boolean sendOrderToWarehouse(Integer orderId, ShoppingCart cart) {
    try {
      OrderMessage message = new OrderMessage(
          orderId,
          cart.getShoppingCartId(),
          cart.getCustomerId(),
          cart.getItemsList()
      );

      logger.info("Sending order {} to warehouse. Cart: {}, Items: {}",
          orderId, cart.getShoppingCartId(), cart.getItemsList().size());

      rabbitTemplate.convertAndSend(exchangeName, routingKey, message);

      logger.info("Order {} sent to warehouse successfully", orderId);
      return true;

    } catch (Exception e) {
      logger.error("Failed to send order {} to warehouse: {}", orderId, e.getMessage(), e);
      return false;
    }
  }
}