package com.cs6650.group13.shoppingcart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateCartRequest {

  @NotNull(message = "Customer ID is required")
  @Positive(message = "Customer ID must be positive")
  @JsonProperty("customer_id")
  private Integer customerId;

  public CreateCartRequest() {
  }

  public CreateCartRequest(Integer customerId) {
    this.customerId = customerId;
  }

  public Integer getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Integer customerId) {
    this.customerId = customerId;
  }

  @Override
  public String toString() {
    return "CreateCartRequest{" +
        "customerId=" + customerId +
        '}';
  }
}