package com.cs6650.group13.shoppingcart.exception;

public class CartNotFoundException extends RuntimeException {

  public CartNotFoundException(Integer cartId) {
    super("Shopping cart not found with ID: " + cartId);
  }
}