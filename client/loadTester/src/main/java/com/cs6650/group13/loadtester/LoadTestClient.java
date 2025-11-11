package com.cs6650.group13.loadtester;

import com.cs6650.group13.loadtester.api.ShoppingCartClient;
import com.cs6650.group13.loadtester.config.LoadTestConfig;
import com.cs6650.group13.loadtester.metrics.TestMetrics;
import com.cs6650.group13.loadtester.monitor.RabbitMQMonitor;
import org.apache.commons.cli.*;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main Load Testing Client
 * Supports two test modes:
 * 1. Full Workflow: Create cart -> Add item -> Checkout
 * 2. Mass Checkout: Pre-create carts with items, then mass checkout
 */
public class LoadTestClient {

    private static final Logger logger = LoggerFactory.getLogger(LoadTestClient.class);

    // Credit card numbers for testing
    private static final String VALID_CARD = "1234-5678-9012-3456";  // Valid format, ~90% approval
    private static final String INVALID_CARD = "0000-0000-0000-0000"; // Invalid format, 100% failure

    // Product IDs to use in testing
    private static final int[] PRODUCT_IDS = {1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010};

    private final LoadTestConfig config;
    private final TestMetrics metrics;
    private final RabbitMQMonitor rabbitMQMonitor;
    private final ShoppingCartClient shoppingCartClient;
    private final ExecutorService executorService;
    private final Random random;

    public LoadTestClient(LoadTestConfig config) {
        this.config = config;
        this.metrics = new TestMetrics();
        this.rabbitMQMonitor = new RabbitMQMonitor(
            config.getRabbitmqMgmtUrl(),
            config.getRabbitmqUser(),
            config.getRabbitmqPass(),
            config.getQueueName()
        );
        this.shoppingCartClient = new ShoppingCartClient(config.getShoppingCartUrl());
        this.executorService = Executors.newFixedThreadPool(config.getNumThreads());
        this.random = new Random();
    }

    /**
     * Run the load test
     */
    public void run() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("LOAD TEST STARTING");
        System.out.println("=".repeat(80));
        System.out.println(config);
        System.out.println("=".repeat(80));
        System.out.println();

        // Start metrics display thread
        Thread metricsDisplayThread = new Thread(this::displayMetricsLoop);
        metricsDisplayThread.setDaemon(true);
        metricsDisplayThread.start();

        try {
            if (config.getTestMode() == LoadTestConfig.TestMode.FULL_WORKFLOW) {
                runFullWorkflowTest();
            } else {
                runMassCheckoutTest();
            }
        } catch (Exception e) {
            logger.error("Load test failed", e);
        } finally {
            executorService.shutdown();
            try {
                executorService.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Executor service shutdown interrupted");
            }
            
            metrics.markComplete();
            
            // Final metrics display
            System.out.println();
            displayFinalRabbitMQStats();
            metrics.printSummary();
            
            // Close resources
            shoppingCartClient.close();
            rabbitMQMonitor.close();
        }
    }

    /**
     * Test Mode 1: Full workflow (Create cart -> Add item -> Checkout)
     */
    private void runFullWorkflowTest() {
        System.out.println("Running FULL WORKFLOW test...");
        System.out.println();

        CountDownLatch latch = new CountDownLatch(config.getTotalRequests());
        AtomicInteger requestCounter = new AtomicInteger(0);

        for (int i = 0; i < config.getTotalRequests(); i++) {
            final int requestNum = requestCounter.incrementAndGet();
            
            executorService.submit(() -> {
                try {
                    executeFullWorkflow(requestNum);
                } finally {
                    latch.countDown();
                }
            });

            // Apply delay if configured
            if (config.getDelayMs() > 0) {
                try {
                    Thread.sleep(config.getDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.warn("Test interrupted");
        }
    }

    /**
     * Execute full workflow for one request
     */
    private void executeFullWorkflow(int requestNum) {
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Create cart
            int customerId = 1000 + requestNum;
            int cartId = shoppingCartClient.createCart(customerId);
            
            if (cartId < 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                metrics.recordFailure(elapsed, "Create Cart Failed");
                return;
            }

            // Step 2: Add item
            int productId = PRODUCT_IDS[requestNum % PRODUCT_IDS.length];
            boolean itemAdded = shoppingCartClient.addItem(cartId, productId, 1);
            
            if (!itemAdded) {
                long elapsed = System.currentTimeMillis() - startTime;
                metrics.recordFailure(elapsed, "Add Item Failed");
                return;
            }

            // Step 3: Checkout
            String creditCard = shouldFail(requestNum) ? INVALID_CARD : VALID_CARD;
            ShoppingCartClient.CheckoutResult result = shoppingCartClient.checkout(cartId, creditCard);
            
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (result.success) {
                metrics.recordSuccess(elapsed);
            } else {
                metrics.recordFailure(elapsed, result.getFailureReason());
            }

        } catch (IOException | ParseException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            metrics.recordFailure(elapsed, "Exception: " + e.getMessage());
            logger.debug("Request {} failed", requestNum, e);
        }
    }

    /**
     * Test Mode 2: Mass checkout (Pre-create carts, then checkout)
     */
    private void runMassCheckoutTest() {
        System.out.println("Running MASS CHECKOUT test...");
        System.out.println();

        // Phase 1: Pre-create carts with items
        System.out.println("Phase 1: Pre-creating " + config.getTotalRequests() + " carts with items...");
        ConcurrentLinkedQueue<Integer> cartIds = new ConcurrentLinkedQueue<>();
        CountDownLatch setupLatch = new CountDownLatch(config.getTotalRequests());

        for (int i = 0; i < config.getTotalRequests(); i++) {
            final int requestNum = i;
            executorService.submit(() -> {
                try {
                    int cartId = prepareCart(requestNum);
                    if (cartId > 0) {
                        cartIds.add(cartId);
                    }
                } finally {
                    setupLatch.countDown();
                }
            });
        }

        try {
            setupLatch.await();
        } catch (InterruptedException e) {
            logger.warn("Setup interrupted");
        }

        System.out.println("Phase 1 complete: " + cartIds.size() + " carts ready");
        System.out.println();

        // Phase 2: Mass checkout
        System.out.println("Phase 2: Starting mass checkout...");
        System.out.println();

        CountDownLatch checkoutLatch = new CountDownLatch(cartIds.size());
        AtomicInteger checkoutCounter = new AtomicInteger(0);

        for (Integer cartId : cartIds) {
            final int requestNum = checkoutCounter.incrementAndGet();
            
            executorService.submit(() -> {
                try {
                    executeCheckout(cartId, requestNum);
                } finally {
                    checkoutLatch.countDown();
                }
            });

            // Apply delay if configured
            if (config.getDelayMs() > 0) {
                try {
                    Thread.sleep(config.getDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        try {
            checkoutLatch.await();
        } catch (InterruptedException e) {
            logger.warn("Checkout interrupted");
        }
    }

    /**
     * Prepare a cart (create + add item) for mass checkout test
     */
    private int prepareCart(int requestNum) {
        try {
            int customerId = 1000 + requestNum;
            int cartId = shoppingCartClient.createCart(customerId);
            
            if (cartId < 0) {
                logger.warn("Failed to create cart for request {}", requestNum);
                return -1;
            }

            int productId = PRODUCT_IDS[requestNum % PRODUCT_IDS.length];
            boolean itemAdded = shoppingCartClient.addItem(cartId, productId, 1);
            
            if (!itemAdded) {
                logger.warn("Failed to add item to cart {} for request {}", cartId, requestNum);
                return -1;
            }

            return cartId;

        } catch (IOException | ParseException e) {
            logger.warn("Failed to prepare cart for request {}", requestNum, e);
            return -1;
        }
    }

    /**
     * Execute checkout for mass checkout test
     */
    private void executeCheckout(int cartId, int requestNum) {
        long startTime = System.currentTimeMillis();

        try {
            String creditCard = shouldFail(requestNum) ? INVALID_CARD : VALID_CARD;
            ShoppingCartClient.CheckoutResult result = shoppingCartClient.checkout(cartId, creditCard);
            
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (result.success) {
                metrics.recordSuccess(elapsed);
            } else {
                metrics.recordFailure(elapsed, result.getFailureReason());
            }

        } catch (IOException | ParseException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            metrics.recordFailure(elapsed, "Exception: " + e.getMessage());
            logger.debug("Checkout failed for cart {}", cartId, e);
        }
    }

    /**
     * Determine if this request should fail (10% failure rate)
     */
    private boolean shouldFail(int requestNum) {
        // Every 10th request fails (exactly 10%)
        return requestNum % 10 == 0;
    }

    /**
     * Display real-time metrics loop
     */
    private void displayMetricsLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000);
                displayRealtimeMetrics();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Display real-time metrics
     */
    private void displayRealtimeMetrics() {
        int requestsThisSecond = metrics.getAndResetRequestsLastSecond();
        RabbitMQMonitor.QueueStats queueStats = rabbitMQMonitor.getQueueStats();
        
        double progress = (metrics.getTotalRequests() * 100.0) / config.getTotalRequests();
        
        System.out.printf(
            "[%6.2f%%] Requests: %,7d / %,7d | RPS: %,4d | Success: %,7d | Failed: %,6d | " +
            "Queue: %,7d msgs | Pub: %6.1f/s | Consume: %6.1f/s | Ack: %6.1f/s%n",
            progress,
            metrics.getTotalRequests(),
            config.getTotalRequests(),
            requestsThisSecond,
            metrics.getSuccessfulRequests(),
            metrics.getFailedRequests(),
            queueStats.messages,
            queueStats.publishRate,
            queueStats.consumeRate,
            queueStats.ackRate
        );
    }

    /**
     * Display final RabbitMQ stats
     */
    private void displayFinalRabbitMQStats() {
        System.out.println("\nFinal RabbitMQ Queue Status:");
        RabbitMQMonitor.QueueStats stats = rabbitMQMonitor.getQueueStats();
        System.out.println("  Messages in queue: " + String.format("%,d", stats.messages));
        System.out.println("  Active consumers:  " + stats.consumers);
        System.out.println();
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        Options options = new Options();
        
        options.addOption("c", "config", true, "Configuration file path");
        options.addOption("t", "threads", true, "Number of threads (default: 10)");
        options.addOption("n", "requests", true, "Total requests (default: 200000)");
        options.addOption("d", "delay", true, "Delay between requests in ms (default: 0)");
        options.addOption("m", "mode", true, "Test mode: FULL_WORKFLOW or MASS_CHECKOUT (default: MASS_CHECKOUT)");
        options.addOption("u", "url", true, "Shopping cart service URL (default: http://localhost:8083)");
        options.addOption("h", "help", false, "Show help");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("load-test-client", options);
                return;
            }

            LoadTestConfig config;

            if (cmd.hasOption("config")) {
                config = LoadTestConfig.loadFromFile(cmd.getOptionValue("config"));
            } else {
                config = LoadTestConfig.forLocal();
            }

            // Override with command line arguments
            if (cmd.hasOption("threads")) {
                config.setNumThreads(Integer.parseInt(cmd.getOptionValue("threads")));
            }
            if (cmd.hasOption("requests")) {
                config.setTotalRequests(Integer.parseInt(cmd.getOptionValue("requests")));
            }
            if (cmd.hasOption("delay")) {
                config.setDelayMs(Integer.parseInt(cmd.getOptionValue("delay")));
            }
            if (cmd.hasOption("mode")) {
                config.setTestMode(LoadTestConfig.TestMode.valueOf(cmd.getOptionValue("mode").toUpperCase()));
            }
            if (cmd.hasOption("url")) {
                config.setShoppingCartUrl(cmd.getOptionValue("url"));
            }

            LoadTestClient client = new LoadTestClient(config);
            client.run();

        } catch (org.apache.commons.cli.ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            formatter.printHelp("load-test-client", options);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            System.exit(1);
        }
    }
}

