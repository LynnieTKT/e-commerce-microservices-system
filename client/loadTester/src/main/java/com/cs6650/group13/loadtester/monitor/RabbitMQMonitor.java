package com.cs6650.group13.loadtester.monitor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Monitor RabbitMQ queue metrics via Management API
 */
public class RabbitMQMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQMonitor.class);

    private final String managementUrl;
    private final String queueName;
    private final String authHeader;
    private final CloseableHttpClient httpClient;

    private long lastMessageCount = 0;
    private long lastCheckTime = System.currentTimeMillis();

    public RabbitMQMonitor(String managementUrl, String username, String password, String queueName) {
        this.managementUrl = managementUrl;
        this.queueName = queueName;
        
        // Create Basic Auth header
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        this.authHeader = "Basic " + encodedAuth;
        
        this.httpClient = createHttpClient();
    }
    
    /**
     * Create HTTP client with proper connection pooling
     */
    private CloseableHttpClient createHttpClient() {
        // Configure connection pooling (smaller pool for monitoring)
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(10);
        
        // Configure connection settings
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(5))
            .setSocketTimeout(Timeout.ofSeconds(10))
            .setTimeToLive(TimeValue.ofMinutes(5))
            .build();
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        
        // Configure request settings
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofSeconds(3))
            .setResponseTimeout(Timeout.ofSeconds(10))
            .build();
        
        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .evictIdleConnections(TimeValue.ofSeconds(30))
            .evictExpiredConnections()
            .build();
    }

    /**
     * Get current queue statistics
     */
    public QueueStats getQueueStats() {
        String url = String.format("%s/api/queues/%%2F/%s", managementUrl, queueName);
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", authHeader);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String body = EntityUtils.toString(response.getEntity());
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            QueueStats stats = new QueueStats();
            stats.messages = json.has("messages") ? json.get("messages").getAsLong() : 0;
            stats.consumers = json.has("consumers") ? json.get("consumers").getAsInt() : 0;
            
            // Message rates
            if (json.has("message_stats")) {
                JsonObject messageStats = json.getAsJsonObject("message_stats");
                
                if (messageStats.has("publish_details")) {
                    stats.publishRate = messageStats.getAsJsonObject("publish_details")
                        .get("rate").getAsDouble();
                }
                
                if (messageStats.has("deliver_get_details")) {
                    stats.consumeRate = messageStats.getAsJsonObject("deliver_get_details")
                        .get("rate").getAsDouble();
                }
                
                if (messageStats.has("ack_details")) {
                    stats.ackRate = messageStats.getAsJsonObject("ack_details")
                        .get("rate").getAsDouble();
                }
            }

            // Calculate messages processed per second
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastCheckTime;
            if (timeDiff > 0 && lastMessageCount > 0) {
                long messageDiff = lastMessageCount - stats.messages;
                if (messageDiff > 0) {
                    stats.messagesProcessedPerSec = (messageDiff * 1000.0) / timeDiff;
                }
            }
            
            lastMessageCount = stats.messages;
            lastCheckTime = currentTime;

            return stats;

        } catch (IOException | ParseException e) {
            logger.warn("Failed to fetch RabbitMQ stats: {}", e.getMessage());
            return new QueueStats();
        }
    }

    /**
     * Close the HTTP client
     */
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.warn("Error closing HTTP client: {}", e.getMessage());
        }
    }

    /**
     * Queue statistics data class
     */
    public static class QueueStats {
        public long messages = 0;
        public int consumers = 0;
        public double publishRate = 0.0;
        public double consumeRate = 0.0;
        public double ackRate = 0.0;
        public double messagesProcessedPerSec = 0.0;

        @Override
        public String toString() {
            return String.format(
                "Queue: %,d msgs | Consumers: %d | Pub: %.1f/s | Consume: %.1f/s | Ack: %.1f/s",
                messages, consumers, publishRate, consumeRate, ackRate
            );
        }
    }
}

