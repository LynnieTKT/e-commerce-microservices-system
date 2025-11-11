package com.cs6650.group13.producttester.api;

import com.cs6650.group13.producttester.model.Product;
import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * HTTP Client for Product Service API
 */
public class ProductClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductClient.class);

    private final String baseUrl;
    private final CloseableHttpClient httpClient;
    private final Gson gson;

    public ProductClient(String baseUrl) {
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
     * Create a product
     * @param product Product to create
     * @return HTTP status code
     */
    public int createProduct(Product product) throws IOException {
        String url = baseUrl + "/product";
        HttpPost request = new HttpPost(url);
        
        String jsonBody = gson.toJson(product);
        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return response.getCode();
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
}

