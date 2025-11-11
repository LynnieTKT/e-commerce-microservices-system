package com.cs6650.group13.shoppingcart.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

  private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

  @Value("${app.rabbitmq.queue-name}")
  private String queueName;

  @Value("${app.rabbitmq.exchange-name}")
  private String exchangeName;

  @Value("${app.rabbitmq.routing-key}")
  private String routingKey;

  /**
   * Declare the queue for warehouse orders
   */
  @Bean
  public Queue warehouseQueue() {
    logger.info("Creating queue: {}", queueName);
    return QueueBuilder.durable(queueName).build();
  }

  /**
   * Declare the exchange for warehouse orders
   */
  @Bean
  public DirectExchange warehouseExchange() {
    logger.info("Creating exchange: {}", exchangeName);
    return new DirectExchange(exchangeName);
  }

  /**
   * Bind queue to exchange with routing key
   */
  @Bean
  public  binding(Queue warehouseQueue, DirectExchange warehouseExchange) {
    logger.info("Binding queue {} to exchange {} with routing key {}",
        queueName, exchangeName, routingKey);
    return BindingBuilder.bind(warehouseQueue)
        .to(warehouseExchange)
        .with(routingKey);
  }

  /**
   * JSON message converter
   */
  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  /**
   * RabbitTemplate with JSON converter and publisher confirms
   */
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jsonMessageConverter());
    template.setMandatory(true);

    // Publisher Confirms callback
    template.setConfirmCallback((correlationData, ack, cause) -> {
      if (ack) {
        logger.debug("Message confirmed by broker");
      } else {
        logger.error("Message not confirmed by broker. Cause: {}", cause);
      }
    });

    // Publisher Returns callback
    template.setReturnsCallback(returned -> {
      logger.error("Message returned: {}, reply code: {}, reply text: {}",
          returned.getMessage(),
          returned.getReplyCode(),
          returned.getReplyText());
    });

    return template;
  }
}