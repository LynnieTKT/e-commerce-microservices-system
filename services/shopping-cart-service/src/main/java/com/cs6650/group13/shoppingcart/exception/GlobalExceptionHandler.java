package com.cs6650.group13.shoppingcart.exception;

import com.cs6650.group13.shoppingcart.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(CartNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleCartNotFound(CartNotFoundException ex) {
    logger.warn("Cart not found: {}", ex.getMessage());
    ErrorResponse error = new ErrorResponse("CART_NOT_FOUND", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
    String details = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .reduce((a, b) -> a + "; " + b)
        .orElse("Validation failed");

    logger.warn("Validation error: {}", details);

    ErrorResponse error = new ErrorResponse(
        "INVALID_INPUT",
        "The provided input data is invalid",
        details
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
    logger.warn("Illegal state: {}", ex.getMessage());
    ErrorResponse error = new ErrorResponse("INVALID_STATE", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    logger.warn("Illegal argument: {}", ex.getMessage());
    ErrorResponse error = new ErrorResponse("INVALID_INPUT", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    logger.error("Internal server error", ex);
    ErrorResponse error = new ErrorResponse(
        "INTERNAL_ERROR",
        "An internal server error occurred",
        ex.getMessage()
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}