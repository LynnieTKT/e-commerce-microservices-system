package com.cs6650.group13.shoppingcart.service;

import com.cs6650.group13.shoppingcart.exception.CartNotFoundException;
import com.cs6650.group13.shoppingcart.messaging.OrderMessageProducer;
import com.cs6650.group13.shoppingcart.model.ShoppingCart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ShoppingCartService {

  private static final Logger logger = LoggerFactory.getLogger(ShoppingCartService.class);

  private final ConcurrentHashMap<Integer, ShoppingCart> carts = new ConcurrentHashMap<>();
  private final AtomicInteger cartIdGenerator = new AtomicInteger(1);
  private final AtomicInteger orderIdGenerator = new AtomicInteger(1000);

  private final CreditCardAuthorizerClient ccaClient;

  @Autowired(required = false)
  private OrderMessageProducer messageProducer;

  @Value("${app.rabbitmq.enabled:true}")
  private boolean rabbitmqEnabled;

  public ShoppingCartService(CreditCardAuthorizerClient ccaClient) {
    this.ccaClient = ccaClient;
  }

  /**
   * Create a new shopping cart
   */
  public Integer createCart(Integer customerId) {
    Integer cartId = cartIdGenerator.getAndIncrement();
    ShoppingCart cart = new ShoppingCart(cartId, customerId);
    carts.put(cartId, cart);

    logger.info("Created shopping cart {} for customer {}", cartId, customerId);
    return cartId;
  }

  /**
   * Add item to shopping cart
   */
  public void addItem(Integer shoppingCartId, Integer productId, Integer quantity) {
    ShoppingCart cart = getCart(shoppingCartId);

    if (cart.isCheckedOut()) {
      throw new IllegalStateException("Cannot add items to a checked-out cart");
    }

    cart.addItem(productId, quantity);
    logger.info("Added {} units of product {} to cart {}", quantity, productId, shoppingCartId);
  }

  /**
   * Checkout shopping cart
   * Returns order ID if successful
   */
  public Integer checkout(Integer shoppingCartId, String creditCardNumber) {
    ShoppingCart cart = getCart(shoppingCartId);

    if (cart.isCheckedOut()) {
      throw new IllegalStateException("Cart has already been checked out");
    }

    if (cart.getItems().isEmpty()) {
      throw new IllegalStateException("Cannot checkout an empty cart");
    }

    // Step 1: Authorize credit card
    logger.info("Checkout cart {}: Authorizing credit card", shoppingCartId);
    boolean authorized = ccaClient.authorize(creditCardNumber);

    if (!authorized) {
      logger.warn("Checkout cart {}: Credit card DECLINED", shoppingCartId);
      throw new IllegalStateException("Payment declined");
    }

    logger.info("Checkout cart {}: Credit card AUTHORIZED", shoppingCartId);

    // Step 2: Mark cart as checked out
    cart.setCheckedOut(true);

    // Step 3: Generate order ID
    Integer orderId = orderIdGenerator.getAndIncrement();

    // Step 4: Send to RabbitMQ
    if (rabbitmqEnabled && messageProducer != null) {
      boolean messageSent = messageProducer.sendOrderToWarehouse(orderId, cart);
      if (!messageSent) {
        logger.error("Failed to send order {} to warehouse, but order is created", orderId);
      }
    } else {
      logger.warn("RabbitMQ is disabled. Order {} not sent to warehouse", orderId);
    }

    logger.info("Checkout cart {}: Order {} created successfully", shoppingCartId, orderId);

    return orderId;
  }

  /**
   * Get shopping cart by ID
   */
  private ShoppingCart getCart(Integer shoppingCartId) {
    ShoppingCart cart = carts.get(shoppingCartId);
    if (cart == null) {
      throw new CartNotFoundException(shoppingCartId);
    }
    return cart;
  }

  /**
   * Get cart (public method for testing)
   */
  public ShoppingCart getCartById(Integer shoppingCartId) {
    return getCart(shoppingCartId);
  }
}