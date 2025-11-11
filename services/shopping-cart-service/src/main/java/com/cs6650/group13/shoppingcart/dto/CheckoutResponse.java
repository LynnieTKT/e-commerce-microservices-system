package com.cs6650.group13.shoppingcart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckoutResponse {

  @JsonProperty("order_id")
  private Integer orderId;

  public CheckoutResponse() {
  }

  public CheckoutResponse(Integer orderId) {
    this.orderId = orderId;
  }

  public Integer getOrderId() {
    return orderId;
  }

  public void setOrderId(Integer orderId) {
    this.orderId = orderId;
  }

  @Override
  public String toString() {
    return "CheckoutResponse{" +
        "orderId=" + orderId +
        '}';
  }
}