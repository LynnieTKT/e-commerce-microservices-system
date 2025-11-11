package com.cs6650.group13.shoppingcart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AddItemRequest {

  @NotNull(message = "Product ID is required")
  @Positive(message = "Product ID must be positive")
  @JsonProperty("product_id")
  private Integer productId;

  @NotNull(message = "Quantity is required")
  @Positive(message = "Quantity must be positive")
  @JsonProperty("quantity")
  private Integer quantity;

  public AddItemRequest() {
  }

  public AddItemRequest(Integer productId, Integer quantity) {
    this.productId = productId;
    this.quantity = quantity;
  }

  public Integer getProductId() {
    return productId;
  }

  public void setProductId(Integer productId) {
    this.productId = productId;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  @Override
  public String toString() {
    return "AddItemRequest{" +
        "productId=" + productId +
        ", quantity=" + quantity +
        '}';
  }
}