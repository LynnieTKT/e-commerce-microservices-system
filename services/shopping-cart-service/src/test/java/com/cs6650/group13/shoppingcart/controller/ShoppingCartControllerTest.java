package com.cs6650.group13.shoppingcart.controller;

import com.cs6650.group13.shoppingcart.dto.*;
import com.cs6650.group13.shoppingcart.exception.CartNotFoundException;
import com.cs6650.group13.shoppingcart.service.ShoppingCartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShoppingCartController.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShoppingCartControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ShoppingCartService shoppingCartService;

  @Test
  void testCreateCart_Success() throws Exception {
    // Given
    CreateCartRequest request = new CreateCartRequest(100);
    Integer expectedCartId = 1;

    when(shoppingCartService.createCart(100)).thenReturn(expectedCartId);

    // When & Then
    mockMvc.perform(post("/shopping-cart")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.shopping_cart_id").value(expectedCartId));
  }

  @Test
  void testCreateCart_InvalidCustomerId_Negative() throws Exception {
    // Given
    CreateCartRequest request = new CreateCartRequest(-1);

    // When & Then
    mockMvc.perform(post("/shopping-cart")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
  }

  @Test
  void testCreateCart_MissingCustomerId() throws Exception {
    // Given
    String emptyJson = "{}";

    // When & Then
    mockMvc.perform(post("/shopping-cart")
            .contentType(MediaType.APPLICATION_JSON)
            .content(emptyJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
  }

  @Test
  void testAddItem_Success() throws Exception {
    // Given
    Integer cartId = 1;
    AddItemRequest request = new AddItemRequest(5, 2);

    // When & Then
    mockMvc.perform(post("/shopping-carts/" + cartId + "/addItem")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());
  }

  @Test
  void testAddItem_CartNotFound() throws Exception {
    // Given
    Integer invalidCartId = 999;
    AddItemRequest request = new AddItemRequest(5, 2);

    doThrow(new CartNotFoundException(invalidCartId))
        .when(shoppingCartService).addItem(eq(invalidCartId), anyInt(), anyInt());

    // When & Then
    mockMvc.perform(post("/shopping-carts/" + invalidCartId + "/addItem")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("CART_NOT_FOUND"));
  }

  @Test
  void testAddItem_InvalidProductId_Negative() throws Exception {
    // Given
    Integer cartId = 1;
    AddItemRequest request = new AddItemRequest(-1, 2);

    // When & Then
    mockMvc.perform(post("/shopping-carts/" + cartId + "/addItem")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
  }

  @Test
  void testAddItem_InvalidQuantity_Zero() throws Exception {
    // Given
    Integer cartId = 1;
    AddItemRequest request = new AddItemRequest(5, 0);

    // When & Then
    mockMvc.perform(post("/shopping-carts/" + cartId + "/addItem")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
  }

  @Test
  void testAddItem_AlreadyCheckedOut() throws Exception {
    // Given
    Integer cartId = 1;
    AddItemRequest request = new AddItemRequest(5, 2);

    doThrow(new IllegalStateException("Cannot add items to a checked-out cart"))
        .when(shoppingCartService).addItem(eq(cartId), anyInt(), anyInt());

    // When & Then
    mockMvc.perform(post("/shopping-carts/" + cartId + "/addItem")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_STATE"));
  }

  @Test
  void testCheckout_Success() throws Exception {
    // Given
    Integer cartId = 1;
    CheckoutRequest request = new CheckoutRequest("1234-5678-9012-3456");
    Integer expectedOrderId = 1000;

    when(shoppingCartService.checkout(cartId, request.getCreditCardNumber()))
        .thenReturn(expectedOrderId);

    // When & Then
    mockMvc.perform(post("/shopping-carts/" + cartId + "/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.order_id").value(expectedOrderId));
  }

  @Test
  void testCheckout_CartNotFound() throws Exception {
    // Given
    Integer invalidCartId = 999;
    CheckoutRequest request = new CheckoutRequest("1234-5678-9012-3456");

    when(shoppingCartService.checkout(eq(invalidCartId), anyString()))
        .thenThrow(new CartNotFoundException(invalidCartId));

    // When & Then
    mockMvc.perform(post("/shopping-carts/" + invalidCartId + "/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("CART_NOT_FOUND"));
  }

  @Test
  void testCheckout_InvalidCardFormat() throws Exception {
    // Given
    Integer cartId = 1;
    CheckoutRequest request = new CheckoutRequest("invalid-card");

    // When & Then
    mockMvc.perform(post("/shopping-carts/" + cartId + "/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
  }

  @Test
  void testCheckout_PaymentDeclined() throws Exception {
    // Given
    Integer cartId = 1;
    CheckoutRequest request = new CheckoutRequest("1234-5678-9012-3456");

    when(shoppingCartService.checkout(cartId, request.getCreditCardNumber()))
        .thenThrow(new IllegalStateException("Payment declined"));

    // When & Then
    mockMvc.perform(post("/shopping-carts/" + cartId + "/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_STATE"))
        .andExpect(jsonPath("$.message").value("Payment declined"));
  }

  @Test
  void testCheckout_EmptyCart() throws Exception {
    // Given
    Integer cartId = 1;
    CheckoutRequest request = new CheckoutRequest("1234-5678-9012-3456");

    when(shoppingCartService.checkout(cartId, request.getCreditCardNumber()))
        .thenThrow(new IllegalStateException("Cannot checkout an empty cart"));

    // When & Then
    mockMvc.perform(post("/shopping-carts/" + cartId + "/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_STATE"));
  }

  @Test
  void testCheckout_AlreadyCheckedOut() throws Exception {
    // Given
    Integer cartId = 1;
    CheckoutRequest request = new CheckoutRequest("1234-5678-9012-3456");

    when(shoppingCartService.checkout(cartId, request.getCreditCardNumber()))
        .thenThrow(new IllegalStateException("Cart has already been checked out"));

    // When & Then
    mockMvc.perform(post("/shopping-carts/" + cartId + "/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_STATE"));
  }
}