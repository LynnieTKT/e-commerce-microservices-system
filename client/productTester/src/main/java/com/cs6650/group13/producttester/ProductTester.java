package com.cs6650.group13.producttester;

import com.cs6650.group13.producttester.api.ProductClient;
import com.cs6650.group13.producttester.config.ProductTesterConfig;
import com.cs6650.group13.producttester.metrics.ProductTestMetrics;
import com.cs6650.group13.producttester.model.Product;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main Product Testing Client
 * Tests Product Service endpoints to demonstrate load balancer behavior
 */
public class ProductTester {

    private static final Logger logger = LoggerFactory.getLogger(ProductTester.class);

    // Test product templates
    private static final String[] MANUFACTURERS = {"Apple", "Samsung", "Sony", "Microsoft", "Dell", "HP", "Lenovo", "ASUS"};
    private static final String[] CATEGORIES = {"Electronics", "Computers", "Phones", "Tablets", "Accessories"};

    private final ProductTesterConfig config;
    private final ProductTestMetrics metrics;
    private final List<ProductClient> clients;
    private final ExecutorService executorService;

    public ProductTester(ProductTesterConfig config) {
        this.config = config;
        this.metrics = new ProductTestMetrics();
        this.clients = new ArrayList<>();
        this.executorService = Executors.newFixedThreadPool(config.getNumThreads());
        
        // Create clients based on test target
        initializeClients();
    }

    /**
     * Initialize HTTP clients based on test target
     */
    private void initializeClients() {
        switch (config.getTestTarget()) {
            case GOOD:
                clients.add(new ProductClient(config.getGoodServiceUrl()));
                break;
            case BAD:
                clients.add(new ProductClient(config.getBadServiceUrl()));
                break;
            case BOTH:
                clients.add(new ProductClient(config.getGoodServiceUrl()));
                clients.add(new ProductClient(config.getBadServiceUrl()));
                break;
            case LOAD_BALANCER:
                clients.add(new ProductClient(config.getLoadBalancerUrl()));
                break;
        }
    }

    /**
     * Run the product test
     */
    public void run() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PRODUCT TEST STARTING");
        System.out.println("=".repeat(80));
        System.out.println(config);
        System.out.println("=".repeat(80));
        System.out.println();

        CountDownLatch latch = new CountDownLatch(config.getTotalRequests());
        AtomicInteger requestCounter = new AtomicInteger(0);

        try {
            for (int i = 0; i < config.getTotalRequests(); i++) {
                final int requestNum = requestCounter.incrementAndGet();
                
                executorService.submit(() -> {
                    try {
                        executeProductCreation(requestNum);
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

            // Wait for all requests to complete
            latch.await();

        } catch (InterruptedException e) {
            logger.error("Test interrupted", e);
        } finally {
            executorService.shutdown();
            try {
                executorService.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Executor service shutdown interrupted");
            }
            
            metrics.markComplete();
            
            // Print final summary
            metrics.printSummary();
            
            // Close all clients
            for (ProductClient client : clients) {
                client.close();
            }
        }
    }

    /**
     * Execute a single product creation request
     */
    private void executeProductCreation(int requestNum) {
        long startTime = System.currentTimeMillis();

        try {
            // Create a test product with dummy data
            Product product = createTestProduct(requestNum);
            
            // Select client (round-robin if testing BOTH)
            ProductClient client;
            if (config.getTestTarget() == ProductTesterConfig.TestTarget.BOTH) {
                client = clients.get(requestNum % clients.size());
            } else {
                client = clients.get(0);
            }
            
            // Send request
            int statusCode = client.createProduct(product);
            long elapsed = System.currentTimeMillis() - startTime;
            
            // Record metrics
            metrics.recordRequest(statusCode, elapsed);
            
            // Log at debug level
            logger.debug("Request {}: Status {} in {}ms", requestNum, statusCode, elapsed);

        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            metrics.recordRequest(0, elapsed);  // 0 indicates connection error
            logger.debug("Request {} failed", requestNum, e);
        }
    }

    /**
     * Create a test product with dummy data
     */
    private Product createTestProduct(int requestNum) {
        int productId = 1000 + requestNum;
        String sku = String.format("SKU-%08d", requestNum);
        String manufacturer = MANUFACTURERS[requestNum % MANUFACTURERS.length];
        int categoryId = 100 + (requestNum % 5);
        int weight = 100 + (requestNum % 900);  // 100-999 grams
        int someOtherId = 2000 + requestNum;
        
        return new Product(productId, sku, manufacturer, categoryId, weight, someOtherId);
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        Options options = new Options();
        
        options.addOption("c", "config", true, "Configuration file path");
        options.addOption("t", "threads", true, "Number of threads (default: 10)");
        options.addOption("n", "requests", true, "Total requests (default: 1000)");
        options.addOption("d", "delay", true, "Delay between requests in ms (default: 0)");
        options.addOption("target", true, "Test target: GOOD, BAD, BOTH, or LOAD_BALANCER (default: BOTH)");
        options.addOption("h", "help", false, "Show help");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("product-tester", options);
                System.out.println("\nExamples:");
                System.out.println("  Local testing (both services):");
                System.out.println("    java -jar target/product-tester-1.0.0.jar -c config/local.properties -t 10 -n 1000");
                System.out.println("\n  AWS testing (load balancer):");
                System.out.println("    java -jar target/product-tester-1.0.0.jar -c config/aws.properties -t 20 -n 10000");
                System.out.println("\n  Quick test:");
                System.out.println("    java -jar target/product-tester-1.0.0.jar -t 5 -n 100");
                return;
            }

            ProductTesterConfig config;

            if (cmd.hasOption("config")) {
                config = ProductTesterConfig.loadFromFile(cmd.getOptionValue("config"));
            } else {
                config = ProductTesterConfig.forLocal();
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
            if (cmd.hasOption("target")) {
                config.setTestTarget(ProductTesterConfig.TestTarget.valueOf(cmd.getOptionValue("target").toUpperCase()));
            }

            ProductTester tester = new ProductTester(config);
            tester.run();

        } catch (org.apache.commons.cli.ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            formatter.printHelp("product-tester", options);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            System.exit(1);
        }
    }
}

