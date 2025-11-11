package com.cs6650.group13.shoppingcart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateCartResponse {

  @JsonProperty("shopping_cart_id")
  private Integer shoppingCartId;

  public CreateCartResponse() {
  }

  public CreateCartResponse(Integer shoppingCartId) {
    this.shoppingCartId = shoppingCartId;
  }

  public Integer getShoppingCartId() {
    return shoppingCartId;
  }

  public void setShoppingCartId(Integer shoppingCartId) {
    this.shoppingCartId = shoppingCartId;
  }

  @Override
  public String toString() {
    return "CreateCartResponse{" +
        "shoppingCartId=" + shoppingCartId +
        '}';
  }
}