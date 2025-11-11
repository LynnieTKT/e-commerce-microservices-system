package com.cs6650.group13.shoppingcart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CheckoutRequest {

  @NotNull(message = "Credit card number is required")
  @Pattern(
      regexp = "^[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}$",
      message = "Credit card number must be in format XXXX-XXXX-XXXX-XXXX"
  )
  @JsonProperty("credit_card_number")
  private String creditCardNumber;

  public CheckoutRequest() {
  }

  public CheckoutRequest(String creditCardNumber) {
    this.creditCardNumber = creditCardNumber;
  }

  public String getCreditCardNumber() {
    return creditCardNumber;
  }

  public void setCreditCardNumber(String creditCardNumber) {
    this.creditCardNumber = creditCardNumber;
  }

  @Override
  public String toString() {
    return "CheckoutRequest{" +
        "creditCardNumber=****-****-****-" +
        (creditCardNumber != null && creditCardNumber.length() > 4 ?
            creditCardNumber.substring(creditCardNumber.length() - 4) : "****") +
        '}';
  }
}