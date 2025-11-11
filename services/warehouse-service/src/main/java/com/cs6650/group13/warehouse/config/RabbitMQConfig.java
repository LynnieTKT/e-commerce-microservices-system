package com.cs6650.group13.warehouse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

  /**
   * JSON message converter for deserializing OrderMessage
   */
  @Bean
  public MessageConverter jsonMessageConverter() {
    logger.info("Configuring JSON message converter");
    return new Jackson2JsonMessageConverter();
  }
}

