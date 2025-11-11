package com.ecommerce.cca.controller;

import com.ecommerce.cca.model.AuthorizationRequest;
import com.ecommerce.cca.model.ErrorResponse;
import com.ecommerce.cca.service.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

/**
 * REST Controller for Credit Card Authorization
 * Endpoints:
 * - POST /credit-card-authorizer/authorize - Authorize a credit card payment
 * - GET /credit-card-authorizer/health - Health check
 */
@RestController
@RequestMapping("/credit-card-authorizer")
public class CreditCardController {

    private static final Logger logger = Logger.getLogger(CreditCardController.class.getName());

    private final AuthorizationService authorizationService;

    @Autowired
    public CreditCardController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * Health check endpoint
     * Used by AWS ALB target group health checks
     *
     * @return 200 OK with status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CCA Service is running");
    }

    /**
     * Authorize credit card payment
     *
     * Returns:
     * - 200 OK: Payment authorized
     * - 400 BAD REQUEST: Invalid card format
     * - 402 PAYMENT REQUIRED: Payment declined
     *
     * @param request Authorization request with credit card number
     * @return ResponseEntity with appropriate status code
     */
    @PostMapping("/authorize")
    public ResponseEntity<?> authorize(@Valid @RequestBody AuthorizationRequest request) {

        String cardNumber = request.getCreditCardNumber();
        logger.info("Received authorization request");

        // Double-check format validation (should be caught by @Valid annotation)
        if (!authorizationService.validateFormat(cardNumber)) {
            logger.warning("Invalid card format received: " + cardNumber);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(
                            "INVALID_CARD_FORMAT",
                            "Credit card number must be in format XXXX-XXXX-XXXX-XXXX",
                            "Received format does not match required pattern"
                    ));
        }

        // Attempt authorization
        // 90% of requests will be authorized, 10% will be declined
        boolean isAuthorized = authorizationService.authorize(cardNumber);

        if (isAuthorized) {
            logger.info("Authorization APPROVED");
            // Return 200 OK with empty body for authorized transactions
            return ResponseEntity.ok().build();
        } else {
            logger.info("Authorization DECLINED");
            // Return 402 Payment Required for declined transactions
            return ResponseEntity
                    .status(HttpStatus.PAYMENT_REQUIRED) // 402
                    .body(new ErrorResponse(
                            "PAYMENT_DECLINED",
                            "Credit card authorization was declined",
                            "Please try a different payment method"
                    ));
        }
    }

    /**
     * Exception handler for validation errors
     * Catches @Valid annotation failures and returns 400 Bad Request
     *
     * @param ex The validation exception
     * @return 400 response with error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        // Extract the first validation error
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String errorMessage = fieldError != null
                ? fieldError.getDefaultMessage()
                : "Invalid input data";

        logger.warning("Validation error: " + errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "VALIDATION_ERROR",
                        errorMessage,
                        "Check credit card number format: XXXX-XXXX-XXXX-XXXX"
                ));
    }

    /**
     * Generic exception handler
     * Catches any unexpected errors and returns 500 Internal Server Error
     *
     * @param ex The exception
     * @return 500 response with error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.severe("Unexpected error: " + ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                        "Please try again later"
                ));
    }
}