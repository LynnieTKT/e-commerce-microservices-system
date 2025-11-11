package com.cs6650.group13.producttester.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration for product testing
 * Supports local, AWS direct, and AWS load balancer testing
 */
public class ProductTesterConfig {

    // Default values
    private static final String DEFAULT_GOOD_URL = "http://localhost:8080/api";
    private static final String DEFAULT_BAD_URL = "http://localhost:8081/api";
    private static final String DEFAULT_LOAD_BALANCER_URL = "";

    private String goodServiceUrl;
    private String badServiceUrl;
    private String loadBalancerUrl;

    // Test parameters
    private int numThreads;
    private int totalRequests;
    private int delayMs;
    private TestTarget testTarget;

    public enum TestTarget {
        GOOD,           // Test only good service
        BAD,            // Test only bad service
        BOTH,           // Test both services (split load)
        LOAD_BALANCER   // Test through load balancer
    }

    public ProductTesterConfig() {
        // Default values
        this.goodServiceUrl = DEFAULT_GOOD_URL;
        this.badServiceUrl = DEFAULT_BAD_URL;
        this.loadBalancerUrl = DEFAULT_LOAD_BALANCER_URL;
        
        this.numThreads = 10;
        this.totalRequests = 1000;
        this.delayMs = 0;
        this.testTarget = TestTarget.BOTH;
    }

    /**
     * Load configuration from properties file
     */
    public static ProductTesterConfig loadFromFile(String configFile) throws IOException {
        ProductTesterConfig config = new ProductTesterConfig();
        Properties props = new Properties();

        try (InputStream input = new FileInputStream(configFile)) {
            props.load(input);
            
            config.goodServiceUrl = props.getProperty("product.service.good.url", DEFAULT_GOOD_URL);
            config.badServiceUrl = props.getProperty("product.service.bad.url", DEFAULT_BAD_URL);
            config.loadBalancerUrl = props.getProperty("product.service.load.balancer.url", DEFAULT_LOAD_BALANCER_URL);
            
            config.numThreads = Integer.parseInt(props.getProperty("test.threads", "10"));
            config.totalRequests = Integer.parseInt(props.getProperty("test.total.requests", "1000"));
            config.delayMs = Integer.parseInt(props.getProperty("test.delay.ms", "0"));
            
            String target = props.getProperty("test.target", "BOTH");
            config.testTarget = TestTarget.valueOf(target.toUpperCase());
        }

        return config;
    }

    /**
     * Load configuration for local testing
     */
    public static ProductTesterConfig forLocal() {
        return new ProductTesterConfig();
    }

    // Getters and Setters
    public String getGoodServiceUrl() {
        return goodServiceUrl;
    }

    public void setGoodServiceUrl(String goodServiceUrl) {
        this.goodServiceUrl = goodServiceUrl;
    }

    public String getBadServiceUrl() {
        return badServiceUrl;
    }

    public void setBadServiceUrl(String badServiceUrl) {
        this.badServiceUrl = badServiceUrl;
    }

    public String getLoadBalancerUrl() {
        return loadBalancerUrl;
    }

    public void setLoadBalancerUrl(String loadBalancerUrl) {
        this.loadBalancerUrl = loadBalancerUrl;
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

    public TestTarget getTestTarget() {
        return testTarget;
    }

    public void setTestTarget(TestTarget testTarget) {
        this.testTarget = testTarget;
    }

    @Override
    public String toString() {
        return "ProductTesterConfig{" +
                "\n  goodServiceUrl='" + goodServiceUrl + '\'' +
                "\n  badServiceUrl='" + badServiceUrl + '\'' +
                "\n  loadBalancerUrl='" + loadBalancerUrl + '\'' +
                "\n  numThreads=" + numThreads +
                "\n  totalRequests=" + totalRequests +
                "\n  delayMs=" + delayMs +
                "\n  testTarget=" + testTarget +
                "\n}";
    }
}

