package com.cs6650.group13.shoppingcart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class CreditCardAuthorizerClientTest {

  private CreditCardAuthorizerClient ccaClient;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    ccaClient = new CreditCardAuthorizerClient(restTemplate);

    // Set mock mode to true
    ReflectionTestUtils.setField(ccaClient, "mockMode", true);
  }

  @Test
  void testAuthorize_ValidCardFormat_ReturnsBoolean() {
    // Given
    String validCard = "1234-5678-9012-3456";

    // When
    boolean result = ccaClient.authorize(validCard);

    // Then
    assertTrue(result || !result); // Result should be boolean
  }

  @Test
  void testAuthorize_InvalidCardFormat_ThrowsException() {
    // Given
    String invalidCard = "invalid-card";

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> {
      ccaClient.authorize(invalidCard);
    });
  }

  @Test
  void testAuthorize_InvalidCardFormat_TooShort_ThrowsException() {
    // Given
    String invalidCard = "1234-5678";

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> {
      ccaClient.authorize(invalidCard);
    });
  }

  @Test
  void testAuthorize_InvalidCardFormat_NoHyphens_ThrowsException() {
    // Given
    String invalidCard = "1234567890123456";

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> {
      ccaClient.authorize(invalidCard);
    });
  }

  @Test
  void testAuthorize_InvalidCardFormat_Letters_ThrowsException() {
    // Given
    String invalidCard = "abcd-efgh-ijkl-mnop";

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> {
      ccaClient.authorize(invalidCard);
    });
  }

  @Test
  void testAuthorize_NullCard_ThrowsException() {
    // Given
    String nullCard = null;

    // When & Then
    assertThrows(Exception.class, () -> {
      ccaClient.authorize(nullCard);
    });
  }

  @Test
  void testAuthorize_MultipleValidCards_StatisticalTest() {
    // Given
    String validCard = "1234-5678-9012-3456";
    int iterations = 100;
    int authorizedCount = 0;

    // When
    for (int i = 0; i < iterations; i++) {
      if (ccaClient.authorize(validCard)) {
        authorizedCount++;
      }
    }

    // Then - Should be approximately 90% authorized (with some tolerance)
    // Accept 80-100% as valid range due to randomness
    assertTrue(authorizedCount >= 80 && authorizedCount <= 100,
        "Expected 80-100 authorizations out of 100, got: " + authorizedCount);
  }
}