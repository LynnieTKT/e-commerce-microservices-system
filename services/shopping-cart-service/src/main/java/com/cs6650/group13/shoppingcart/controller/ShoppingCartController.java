package com.cs6650.group13.shoppingcart.controller;

import com.cs6650.group13.shoppingcart.dto.*;
import com.cs6650.group13.shoppingcart.service.ShoppingCartService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ShoppingCartController {

  private static final Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);

  private final ShoppingCartService shoppingCartService;

  public ShoppingCartController(ShoppingCartService shoppingCartService) {
    this.shoppingCartService = shoppingCartService;
  }

  /**
   * POST /shopping-cart
   * Create a new shopping cart
   */
  @PostMapping("/shopping-cart")
  public ResponseEntity<CreateCartResponse> createCart(@Valid @RequestBody CreateCartRequest request) {
    logger.info("POST /shopping-cart - Customer ID: {}", request.getCustomerId());

    Integer cartId = shoppingCartService.createCart(request.getCustomerId());
    CreateCartResponse response = new CreateCartResponse(cartId);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * POST /shopping-carts/{shoppingCartId}/addItem
   * Add item to shopping cart
   */
  @PostMapping("/shopping-carts/{shoppingCartId}/addItem")
  public ResponseEntity<Void> addItem(
      @PathVariable Integer shoppingCartId,
      @Valid @RequestBody AddItemRequest request) {

    logger.info("POST /shopping-carts/{}/addItem - Product: {}, Quantity: {}",
        shoppingCartId, request.getProductId(), request.getQuantity());

    shoppingCartService.addItem(shoppingCartId, request.getProductId(), request.getQuantity());

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /**
   * POST /shopping-carts/{shoppingCartId}/checkout
   * Checkout shopping cart
   */
  @PostMapping("/shopping-carts/{shoppingCartId}/checkout")
  public ResponseEntity<CheckoutResponse> checkout(
      @PathVariable Integer shoppingCartId,
      @Valid @RequestBody CheckoutRequest request) {

    logger.info("POST /shopping-carts/{}/checkout", shoppingCartId);

    Integer orderId = shoppingCartService.checkout(shoppingCartId, request.getCreditCardNumber());
    CheckoutResponse response = new CheckoutResponse(orderId);

    return ResponseEntity.ok(response);
  }
}