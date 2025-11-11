package com.cs6650.group13.loadtester.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration for load testing
 * Supports both local and AWS deployments
 */
public class LoadTestConfig {

    // Default values
    private static final String DEFAULT_SHOPPING_CART_URL = "http://localhost:8083";
    private static final String DEFAULT_RABBITMQ_MGMT_URL = "http://localhost:15672";
    private static final String DEFAULT_RABBITMQ_USER = "guest";
    private static final String DEFAULT_RABBITMQ_PASS = "guest";
    private static final String DEFAULT_QUEUE_NAME = "warehouse-orders-queue";

    private String shoppingCartUrl;
    private String rabbitmqMgmtUrl;
    private String rabbitmqUser;
    private String rabbitmqPass;
    private String queueName;

    // Test parameters
    private int numThreads;
    private int totalRequests;
    private int delayMs;
    private TestMode testMode;

    public enum TestMode {
        FULL_WORKFLOW,  // Create cart -> Add item -> Checkout
        MASS_CHECKOUT   // Pre-create carts, then mass checkout
    }

    public LoadTestConfig() {
        // Default values
        this.shoppingCartUrl = DEFAULT_SHOPPING_CART_URL;
        this.rabbitmqMgmtUrl = DEFAULT_RABBITMQ_MGMT_URL;
        this.rabbitmqUser = DEFAULT_RABBITMQ_USER;
        this.rabbitmqPass = DEFAULT_RABBITMQ_PASS;
        this.queueName = DEFAULT_QUEUE_NAME;
        
        this.numThreads = 10;
        this.totalRequests = 200000;
        this.delayMs = 0;
        this.testMode = TestMode.MASS_CHECKOUT;
    }

    /**
     * Load configuration from properties file
     */
    public static LoadTestConfig loadFromFile(String configFile) throws IOException {
        LoadTestConfig config = new LoadTestConfig();
        Properties props = new Properties();

        try (InputStream input = new FileInputStream(configFile)) {
            props.load(input);
            
            config.shoppingCartUrl = props.getProperty("shopping.cart.url", DEFAULT_SHOPPING_CART_URL);
            config.rabbitmqMgmtUrl = props.getProperty("rabbitmq.mgmt.url", DEFAULT_RABBITMQ_MGMT_URL);
            config.rabbitmqUser = props.getProperty("rabbitmq.user", DEFAULT_RABBITMQ_USER);
            config.rabbitmqPass = props.getProperty("rabbitmq.pass", DEFAULT_RABBITMQ_PASS);
            config.queueName = props.getProperty("rabbitmq.queue.name", DEFAULT_QUEUE_NAME);
            
            config.numThreads = Integer.parseInt(props.getProperty("test.threads", "10"));
            config.totalRequests = Integer.parseInt(props.getProperty("test.total.requests", "200000"));
            config.delayMs = Integer.parseInt(props.getProperty("test.delay.ms", "0"));
            
            String mode = props.getProperty("test.mode", "MASS_CHECKOUT");
            config.testMode = TestMode.valueOf(mode.toUpperCase());
        }

        return config;
    }

    /**
     * Load configuration for local testing
     */
    public static LoadTestConfig forLocal() {
        return new LoadTestConfig();
    }

    /**
     * Load configuration for AWS testing
     */
    public static LoadTestConfig forAWS(String loadBalancerUrl) {
        LoadTestConfig config = new LoadTestConfig();
        config.shoppingCartUrl = loadBalancerUrl;
        // RabbitMQ management might not be accessible from outside AWS
        // User should configure this based on their AWS setup
        return config;
    }

    // Getters and Setters
    public String getShoppingCartUrl() {
        return shoppingCartUrl;
    }

    public void setShoppingCartUrl(String shoppingCartUrl) {
        this.shoppingCartUrl = shoppingCartUrl;
    }

    public String getRabbitmqMgmtUrl() {
        return rabbitmqMgmtUrl;
    }

    public void setRabbitmqMgmtUrl(String rabbitmqMgmtUrl) {
        this.rabbitmqMgmtUrl = rabbitmqMgmtUrl;
    }

    public String getRabbitmqUser() {
        return rabbitmqUser;
    }

    public void setRabbitmqUser(String rabbitmqUser) {
        this.rabbitmqUser = rabbitmqUser;
    }

    public String getRabbitmqPass() {
        return rabbitmqPass;
    }

    public void setRabbitmqPass(String rabbitmqPass) {
        this.rabbitmqPass = rabbitmqPass;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(int delayMs) {
        this.delayMs = delayMs;
    }

    public TestMode getTestMode() {
        return testMode;
    }

    public void setTestMode(TestMode testMode) {
        this.testMode = testMode;
    }

    @Override
    public String toString() {
        return "LoadTestConfig{" +
                "\n  shoppingCartUrl='" + shoppingCartUrl + '\'' +
                "\n  rabbitmqMgmtUrl='" + rabbitmqMgmtUrl + '\'' +
                "\n  queueName='" + queueName + '\'' +
                "\n  numThreads=" + numThreads +
                "\n  totalRequests=" + totalRequests +
                "\n  delayMs=" + delayMs +
                "\n  testMode=" + testMode +
                "\n}";
    }
}

