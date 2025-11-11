package com.cs6650.group13.shoppingcart.model;

public class CartItem {
  private Integer productId;
  private Integer quantity;

  public CartItem() {
  }

  public CartItem(Integer productId, Integer quantity) {
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
    return "CartItem{" +
        "productId=" + productId +
        ", quantity=" + quantity +
        '}';
  }
}