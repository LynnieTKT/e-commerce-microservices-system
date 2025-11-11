package com.cs6650.group13.warehouse.consumer;

import com.cs6650.group13.warehouse.model.CartItem;
import com.cs6650.group13.warehouse.model.OrderMessage;
import com.cs6650.group13.warehouse.service.WarehouseStatistics;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * RabbitMQ consumer that processes warehouse orders
 * - Uses manual acknowledgements
 * - Multithreaded (configured via application.properties)
 * - Records statistics for each order
 */
@Service
public class OrderMessageConsumer {

  private static final Logger logger = LoggerFactory.getLogger(OrderMessageConsumer.class);

  private final WarehouseStatistics statistics;

  public OrderMessageConsumer(WarehouseStatistics statistics) {
    this.statistics = statistics;
  }

  /**
   * Listen to warehouse orders queue and process orders
   * Manual acknowledgement is sent immediately after recording the order
   *
   * @param orderMessage The order message from shopping cart
   * @param channel The RabbitMQ channel for manual ACK/NACK
   * @param deliveryTag The message delivery tag
   */
  @RabbitListener(queues = "${app.rabbitmq.queue-name}")
  public void receiveOrder(OrderMessage orderMessage,
                           Channel channel,
                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                           Message message) {
    
    try {
      logger.info("Received order from queue: Order ID = {}, Customer ID = {}, Cart ID = {}, Items = {}",
          orderMessage.getOrderId(),
          orderMessage.getCustomerId(),
          orderMessage.getShoppingCartId(),
          orderMessage.getItems() != null ? orderMessage.getItems().size() : 0);

      // Validate message
      if (orderMessage.getOrderId() == null) {
        logger.error("Invalid order message: order_id is null. Message: {}", orderMessage);
        // NACK and requeue for retry
        channel.basicNack(deliveryTag, false, true);
        logger.warn("Order message NACK'd and requeued due to missing order_id");
        return;
      }

      if (orderMessage.getItems() == null || orderMessage.getItems().isEmpty()) {
        logger.warn("Order {} has no items. Acknowledging anyway.", orderMessage.getOrderId());
      }

      // Record the order for reporting purposes
      if (orderMessage.getItems() != null) {
        for (CartItem item : orderMessage.getItems()) {
          if (item.getProductId() == null || item.getQuantity() == null) {
            logger.error("Invalid cart item in order {}: productId={}, quantity={}",
                orderMessage.getOrderId(), item.getProductId(), item.getQuantity());
            // NACK and requeue
            channel.basicNack(deliveryTag, false, true);
            logger.warn("Order {} NACK'd and requeued due to invalid item", orderMessage.getOrderId());
            return;
          }
          
          statistics.recordProduct(
              orderMessage.getOrderId(),
              item.getProductId(),
              item.getQuantity()
          );
        }
      }

      // Increment order count
      statistics.incrementOrderCount();

      // Send manual ACK immediately after recording
      channel.basicAck(deliveryTag, false);
      logger.info("Order {} acknowledged successfully", orderMessage.getOrderId());

      // Simulate processing time (optional - can be removed for faster processing)
      // In a real system, this would be actual warehouse operations
      // Thread.sleep(10);

    } catch (IOException e) {
      // Error sending ACK/NACK
      logger.error("Error acknowledging message for order {}: {}",
          orderMessage.getOrderId(), e.getMessage(), e);
      // Channel might be closed, RabbitMQ will redeliver the message automatically
      
    } catch (Exception e) {
      // Unexpected error during processing
      logger.error("Error processing order {}: {}",
          orderMessage.getOrderId(), e.getMessage(), e);
      
      try {
        // NACK and requeue the message for retry
        channel.basicNack(deliveryTag, false, true);
        logger.warn("Order {} NACK'd and requeued due to processing error", orderMessage.getOrderId());
      } catch (IOException ioException) {
        logger.error("Error sending NACK for order {}: {}",
            orderMessage.getOrderId(), ioException.getMessage(), ioException);
      }
    }
  }
}

