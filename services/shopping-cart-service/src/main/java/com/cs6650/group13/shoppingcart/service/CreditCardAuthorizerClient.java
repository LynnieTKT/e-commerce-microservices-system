package com.cs6650.group13.shoppingcart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class CreditCardAuthorizerClient {

  private static final Logger logger = LoggerFactory.getLogger(CreditCardAuthorizerClient.class);

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.cca.url:http://localhost:8082/credit-card-authorizer/authorize}")
  private String ccaUrl;

  @Value("${app.cca.mock:false}")
  private boolean mockMode;

  public CreditCardAuthorizerClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Authorize a credit card transaction
   * @param creditCardNumber Credit card number in format XXXX-XXXX-XXXX-XXXX
   * @return true if authorized, false if declined
   * @throws IllegalArgumentException if card format is invalid (400)
   * @throws RuntimeException if CCA service error
   */
  public boolean authorize(String creditCardNumber) {
    if (mockMode) {
      return authorizeMock(creditCardNumber);
    }

    return authorizeReal(creditCardNumber);
  }

  /**
   * Mock authorization for local testing
   * Simulates: 90% authorized, 10% declined
   */
  private boolean authorizeMock(String creditCardNumber) {
    logger.info("Mock CCA: Authorizing card: {}", maskCardNumber(creditCardNumber));

    // Validate format
    if (!creditCardNumber.matches("^[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}$")) {
      logger.warn("Mock CCA: Invalid card format");
      throw new IllegalArgumentException("Invalid credit card format");
    }

    // Simulate 90% approval rate
    boolean authorized = Math.random() < 0.9;
    logger.info("Mock CCA: Result = {}", authorized ? "AUTHORIZED" : "DECLINED");

    return authorized;
  }

  /**
   * Real authorization calling CCA service
   */
  private boolean authorizeReal(String creditCardNumber) {
    logger.info("Calling real CCA service at: {}", ccaUrl);

    try {
      // Prepare request - matches CCA AuthorizationRequest model
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, String> requestBody = new HashMap<>();
      requestBody.put("credit_card_number", creditCardNumber);

      HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

      // Call CCA service
      ResponseEntity<String> response = restTemplate.exchange(
          ccaUrl,
          HttpMethod.POST,
          request,
          String.class
      );

      // Returns 200 OK (with EMPTY BODY) for authorized transactions
      if (response.getStatusCode() == HttpStatus.OK) {
        logger.info("CCA: AUTHORIZED (received 200 OK)");
        return true;
      }

      // Handle any other 2xx status as authorized
      if (response.getStatusCode().is2xxSuccessful()) {
        logger.info("CCA: AUTHORIZED (received {})", response.getStatusCode());
        return true;
      }

      logger.warn("CCA: Unexpected status code: {}", response.getStatusCode());
      return false;

    } catch (HttpClientErrorException e) {
      HttpStatus statusCode = (HttpStatus) e.getStatusCode();

      // 400 = Bad Request (invalid format)
      if (statusCode == HttpStatus.BAD_REQUEST) {
        logger.error("CCA: Bad Request - Invalid card format");
        throw new IllegalArgumentException("Invalid credit card format");
      }

      // Returns 402 PAYMENT_REQUIRED
      if (statusCode == HttpStatus.PAYMENT_REQUIRED) {
        logger.info("CCA: DECLINED (received 402 Payment Required)");
        return false;
      }

      // Any other client error
      logger.error("CCA Client Error {}: {}", statusCode, e.getMessage());
      throw new RuntimeException("Credit card authorization service error: " + statusCode, e);

    } catch (Exception e) {
      logger.error("CCA Unexpected Error: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to authorize credit card", e);
    }
  }

  /**
   * Mask credit card number for logging (show only last 4 digits)
   */
  private String maskCardNumber(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 4) {
      return "****";
    }
    return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
  }
}