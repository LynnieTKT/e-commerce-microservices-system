package com.cs6650.group13.loadtester.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Client for Shopping Cart Service API
 */
public class ShoppingCartClient {

    private static final Logger logger = LoggerFactory.getLogger(ShoppingCartClient.class);

    private final String baseUrl;
    private final CloseableHttpClient httpClient;
    private final Gson gson;

    public ShoppingCartClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = createHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Create HTTP client with proper connection pooling for high concurrency
     */
    private CloseableHttpClient createHttpClient() {
        // Configure connection pooling
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);  // Max total connections
        connectionManager.setDefaultMaxPerRoute(100);  // Max connections per route
        
        // Configure connection settings
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(10))
            .setSocketTimeout(Timeout.ofSeconds(30))
            .setTimeToLive(TimeValue.ofMinutes(5))  // Connection TTL
            .build();
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        
        // Configure request settings
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofSeconds(5))  // Timeout for getting connection from pool
            .setResponseTimeout(Timeout.ofSeconds(30))  // Response timeout
            .build();
        
        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .evictIdleConnections(TimeValue.ofSeconds(30))  // Evict idle connections
            .evictExpiredConnections()  // Evict expired connections
            .build();
    }

    /**
     * Create a shopping cart
     * @param customerId Customer ID
     * @return Cart ID or -1 if failed
     */
    public int createCart(int customerId) throws IOException, ParseException {
        String url = baseUrl + "/shopping-cart";
        HttpPost request = new HttpPost(url);
        
        JsonObject body = new JsonObject();
        body.addProperty("customer_id", customerId);
        
        request.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (statusCode == 201) {
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                return json.get("shopping_cart_id").getAsInt();
            } else {
                logger.warn("Create cart failed with status {}: {}", statusCode, responseBody);
                return -1;
            }
        }
    }

    /**
     * Add item to shopping cart
     * @param cartId Shopping cart ID
     * @param productId Product ID
     * @param quantity Quantity
     * @return true if successful
     */
    public boolean addItem(int cartId, int productId, int quantity) throws IOException, ParseException {
        String url = baseUrl + "/shopping-carts/" + cartId + "/addItem";
        HttpPost request = new HttpPost(url);
        
        JsonObject body = new JsonObject();
        body.addProperty("product_id", productId);
        body.addProperty("quantity", quantity);
        
        request.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            
            if (statusCode == 204) {
                return true;
            } else {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.warn("Add item failed with status {}: {}", statusCode, responseBody);
                return false;
            }
        }
    }

    /**
     * Checkout shopping cart
     * @param cartId Shopping cart ID
     * @param creditCardNumber Credit card number
     * @return Order ID or -1 if failed
     */
    public CheckoutResult checkout(int cartId, String creditCardNumber) throws IOException, ParseException {
        String url = baseUrl + "/shopping-carts/" + cartId + "/checkout";
        HttpPost request = new HttpPost(url);
        
        JsonObject body = new JsonObject();
        body.addProperty("credit_card_number", creditCardNumber);
        
        request.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            CheckoutResult result = new CheckoutResult();
            result.statusCode = statusCode;
            
            if (statusCode == 200) {
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                result.orderId = json.get("order_id").getAsInt();
                result.success = true;
            } else {
                result.success = false;
                result.errorMessage = responseBody;
            }
            
            return result;
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
     * Result of checkout operation
     */
    public static class CheckoutResult {
        public boolean success;
        public int statusCode;
        public int orderId = -1;
        public String errorMessage;

        public String getFailureReason() {
            if (success) return null;
            
            if (statusCode == 400 && errorMessage != null) {
                if (errorMessage.contains("Payment declined")) {
                    return "Payment Declined (CCA)";
                } else if (errorMessage.contains("Invalid credit card")) {
                    return "Invalid Card Format";
                } else {
                    return "Bad Request";
                }
            } else if (statusCode == 404) {
                return "Cart Not Found";
            } else if (statusCode >= 500) {
                return "Server Error (" + statusCode + ")";
            } else {
                return "HTTP Error " + statusCode;
            }
        }
    }
}

