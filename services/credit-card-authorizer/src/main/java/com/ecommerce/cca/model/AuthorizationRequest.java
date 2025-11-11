package com.ecommerce.cca.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for credit card authorization
 * Validates credit card number format: XXXX-XXXX-XXXX-XXXX
 */
public class AuthorizationRequest {

    @NotBlank(message = "Credit card number is required")
    @Pattern(
            regexp = "^[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}$",
            message = "Credit card must be in format: 1234-5678-9012-3456"
    )
    @JsonProperty("credit_card_number")
    private String creditCardNumber;

    // Default constructor
    public AuthorizationRequest() {}

    public AuthorizationRequest(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }
}