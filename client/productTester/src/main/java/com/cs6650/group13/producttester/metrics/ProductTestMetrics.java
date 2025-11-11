package com.cs6650.group13.producttester.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Thread-safe metrics tracking for product testing
 */
public class ProductTestMetrics {

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxResponseTime = new AtomicLong(0);
    
    // Track by status code
    private final Map<Integer, AtomicInteger> statusCodeCounts = new ConcurrentHashMap<>();
    
    private final long startTime;
    private long endTime;

    public ProductTestMetrics() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Record a request result
     */
    public void recordRequest(int statusCode, long responseTimeMs) {
        totalRequests.incrementAndGet();
        
        if (statusCode == 201) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        
        recordResponseTime(responseTimeMs);
        
        // Track status code
        statusCodeCounts.computeIfAbsent(statusCode, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Record response time
     */
    private void recordResponseTime(long responseTimeMs) {
        totalResponseTime.addAndGet(responseTimeMs);
        
        // Update min
        long currentMin;
        do {
            currentMin = minResponseTime.get();
            if (responseTimeMs >= currentMin) break;
        } while (!minResponseTime.compareAndSet(currentMin, responseTimeMs));
        
        // Update max
        long currentMax;
        do {
            currentMax = maxResponseTime.get();
            if (responseTimeMs <= currentMax) break;
        } while (!maxResponseTime.compareAndSet(currentMax, responseTimeMs));
    }

    /**
     * Mark test as complete
     */
    public void markComplete() {
        this.endTime = System.currentTimeMillis();
    }

    /**
     * Get total elapsed time in seconds
     */
    public double getElapsedSeconds() {
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return (end - startTime) / 1000.0;
    }

    /**
     * Get average throughput (requests per second)
     */
    public double getAverageThroughput() {
        double elapsed = getElapsedSeconds();
        return elapsed > 0 ? totalRequests.get() / elapsed : 0;
    }

    /**
     * Get average response time in milliseconds
     */
    public double getAverageResponseTime() {
        int total = totalRequests.get();
        return total > 0 ? (double) totalResponseTime.get() / total : 0;
    }

    /**
     * Get success rate as percentage
     */
    public double getSuccessRate() {
        int total = totalRequests.get();
        return total > 0 ? (successfulRequests.get() * 100.0) / total : 0;
    }

    // Getters
    public int getTotalRequests() {
        return totalRequests.get();
    }

    public int getSuccessfulRequests() {
        return successfulRequests.get();
    }

    public int getFailedRequests() {
        return failedRequests.get();
    }

    public long getMinResponseTime() {
        long min = minResponseTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxResponseTime() {
        return maxResponseTime.get();
    }

    public Map<Integer, AtomicInteger> getStatusCodeCounts() {
        return statusCodeCounts;
    }

    /**
     * Print summary statistics
     */
    public void printSummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PRODUCT TEST SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println(String.format("Total Requests:        %,d", getTotalRequests()));
        System.out.println(String.format("Successful (201):      %,d (%.2f%%)", 
            getSuccessfulRequests(), getSuccessRate()));
        System.out.println(String.format("Failed:                %,d (%.2f%%)", 
            getFailedRequests(), 100 - getSuccessRate()));
        System.out.println();
        System.out.println(String.format("Total Time:            %.2f seconds", getElapsedSeconds()));
        System.out.println(String.format("Average Throughput:    %.2f requests/sec", getAverageThroughput()));
        System.out.println();
        System.out.println(String.format("Response Time (avg):   %.2f ms", getAverageResponseTime()));
        System.out.println(String.format("Response Time (min):   %d ms", getMinResponseTime()));
        System.out.println(String.format("Response Time (max):   %d ms", getMaxResponseTime()));
        
        if (!statusCodeCounts.isEmpty()) {
            System.out.println();
            System.out.println("Status Code Breakdown:");
            statusCodeCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    int code = entry.getKey();
                    int count = entry.getValue().get();
                    double percentage = (count * 100.0) / getTotalRequests();
                    String description = getStatusCodeDescription(code);
                    System.out.println(String.format("  %3d %-25s: %,7d (%.2f%%)", 
                        code, description, count, percentage));
                });
        }
        System.out.println("=".repeat(80));
    }
    
    private String getStatusCodeDescription(int code) {
        switch (code) {
            case 201: return "Created (Success)";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            case 503: return "Service Unavailable";
            default: return "";
        }
    }
}

