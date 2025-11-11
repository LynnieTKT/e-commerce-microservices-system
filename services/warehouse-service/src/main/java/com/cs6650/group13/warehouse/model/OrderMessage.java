package com.cs6650.group13.warehouse.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OrderMessage {

  @JsonProperty("order_id")
  private Integer orderId;

  @JsonProperty("shopping_cart_id")
  private Integer shoppingCartId;

  @JsonProperty("customer_id")
  private Integer customerId;

  @JsonProperty("items")
  private List<CartItem> items;

  @JsonProperty("timestamp")
  private String timestamp;

  public OrderMessage() {
  }

  public OrderMessage(Integer orderId, Integer shoppingCartId, Integer customerId, List<CartItem> items) {
    this.orderId = orderId;
    this.shoppingCartId = shoppingCartId;
    this.customerId = customerId;
    this.items = items;
  }

  public Integer getOrderId() {
    return orderId;
  }

  public void setOrderId(Integer orderId) {
    this.orderId = orderId;
  }

  public Integer getShoppingCartId() {
    return shoppingCartId;
  }

  public void setShoppingCartId(Integer shoppingCartId) {
    this.shoppingCartId = shoppingCartId;
  }

  public Integer getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Integer customerId) {
    this.customerId = customerId;
  }

  public List<CartItem> getItems() {
    return items;
  }

  public void setItems(List<CartItem> items) {
    this.items = items;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return "OrderMessage{" +
        "orderId=" + orderId +
        ", shoppingCartId=" + shoppingCartId +
        ", customerId=" + customerId +
        ", itemCount=" + (items != null ? items.size() : 0) +
        ", timestamp='" + timestamp + '\'' +
        '}';
  }
}

