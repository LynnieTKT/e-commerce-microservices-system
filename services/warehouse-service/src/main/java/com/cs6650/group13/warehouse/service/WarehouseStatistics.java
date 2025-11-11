package com.cs6650.group13.warehouse.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe service to track warehouse statistics
 * - Total number of orders processed
 * - Total quantity ordered for each product ID
 */
@Service
public class WarehouseStatistics {

  private static final Logger logger = LoggerFactory.getLogger(WarehouseStatistics.class);

  // Thread-safe counter for total orders
  private final AtomicInteger totalOrders = new AtomicInteger(0);

  // Thread-safe map for product quantities (productId -> total quantity)
  private final ConcurrentHashMap<Integer, AtomicLong> productQuantities = new ConcurrentHashMap<>();

  /**
   * Record an order and update statistics
   * @param orderId The order ID
   * @param productId The product ID
   * @param quantity The quantity ordered
   */
  public void recordProduct(Integer orderId, Integer productId, Integer quantity) {
    // Update product quantity atomically
    productQuantities.computeIfAbsent(productId, k -> new AtomicLong(0))
        .addAndGet(quantity);

    logger.debug("Recorded product {} (qty: {}) for order {}", productId, quantity, orderId);
  }

  /**
   * Increment total order count
   */
  public void incrementOrderCount() {
    int count = totalOrders.incrementAndGet();
    logger.debug("Total orders: {}", count);
  }

  /**
   * Get total number of orders processed
   */
  public int getTotalOrders() {
    return totalOrders.get();
  }

  /**
   * Get total quantity for a specific product
   */
  public long getProductQuantity(Integer productId) {
    AtomicLong quantity = productQuantities.get(productId);
    return quantity != null ? quantity.get() : 0;
  }

  /**
   * Get total number of unique products
   */
  public int getTotalUniqueProducts() {
    return productQuantities.size();
  }

  /**
   * Get total quantity across all products
   */
  public long getTotalQuantity() {
    return productQuantities.values().stream()
        .mapToLong(AtomicLong::get)
        .sum();
  }

  /**
   * Print statistics summary
   * Called on shutdown
   */
  public void printStatistics() {
    logger.info("=====================================");
    logger.info("   WAREHOUSE STATISTICS SUMMARY");
    logger.info("=====================================");
    logger.info("Total Orders Processed: {}", getTotalOrders());
    logger.info("Total Unique Products: {}", getTotalUniqueProducts());
    logger.info("Total Items Quantity: {}", getTotalQuantity());
    logger.info("=====================================");
  }

  /**
   * Reset all statistics (for testing purposes)
   */
  public void reset() {
    totalOrders.set(0);
    productQuantities.clear();
    logger.info("Statistics reset");
  }
}

