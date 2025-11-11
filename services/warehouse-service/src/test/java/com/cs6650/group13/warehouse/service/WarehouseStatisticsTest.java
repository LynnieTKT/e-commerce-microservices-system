package com.cs6650.group13.warehouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class WarehouseStatisticsTest {

  private WarehouseStatistics statistics;

  @BeforeEach
  void setUp() {
    statistics = new WarehouseStatistics();
  }

  @Test
  void testRecordProduct() {
    statistics.recordProduct(1, 100, 5);
    
    assertEquals(5, statistics.getProductQuantity(100));
    assertEquals(1, statistics.getTotalUniqueProducts());
  }

  @Test
  void testRecordMultipleProducts() {
    statistics.recordProduct(1, 100, 5);
    statistics.recordProduct(1, 200, 3);
    statistics.recordProduct(2, 100, 2);
    
    assertEquals(7, statistics.getProductQuantity(100)); // 5 + 2
    assertEquals(3, statistics.getProductQuantity(200));
    assertEquals(2, statistics.getTotalUniqueProducts());
  }

  @Test
  void testIncrementOrderCount() {
    statistics.incrementOrderCount();
    statistics.incrementOrderCount();
    statistics.incrementOrderCount();
    
    assertEquals(3, statistics.getTotalOrders());
  }

  @Test
  void testGetTotalQuantity() {
    statistics.recordProduct(1, 100, 5);
    statistics.recordProduct(1, 200, 3);
    statistics.recordProduct(2, 100, 2);
    
    assertEquals(10, statistics.getTotalQuantity()); // 5 + 3 + 2
  }

  @Test
  void testThreadSafety() throws InterruptedException {
    int numThreads = 20;
    int operationsPerThread = 100;
    
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    
    for (int i = 0; i < numThreads; i++) {
      final int threadId = i;
      executor.submit(() -> {
        try {
          for (int j = 0; j < operationsPerThread; j++) {
            statistics.recordProduct(threadId * 1000 + j, 100, 1);
            statistics.incrementOrderCount();
          }
        } finally {
          latch.countDown();
        }
      });
    }
    
    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();
    
    // Total quantity for product 100 should be numThreads * operationsPerThread
    assertEquals(numThreads * operationsPerThread, statistics.getProductQuantity(100));
    
    // Total orders should be numThreads * operationsPerThread
    assertEquals(numThreads * operationsPerThread, statistics.getTotalOrders());
  }

  @Test
  void testReset() {
    statistics.recordProduct(1, 100, 5);
    statistics.incrementOrderCount();
    
    statistics.reset();
    
    assertEquals(0, statistics.getTotalOrders());
    assertEquals(0, statistics.getTotalUniqueProducts());
    assertEquals(0, statistics.getProductQuantity(100));
  }

  @Test
  void testGetProductQuantityForNonExistentProduct() {
    assertEquals(0, statistics.getProductQuantity(999));
  }

  @Test
  void testPrintStatistics() {
    // Just verify it doesn't throw exception
    statistics.recordProduct(1, 100, 5);
    statistics.incrementOrderCount();
    
    assertDoesNotThrow(() -> statistics.printStatistics());
  }
}

