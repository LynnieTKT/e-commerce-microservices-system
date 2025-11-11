package com.ecommerce.cca.service;

import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Service for credit card authorization
 * Simulates credit card processing with 90% approval rate
 */
@Service
public class AuthorizationService {

    private static final Logger logger = Logger.getLogger(AuthorizationService.class.getName());
    private final Random random = new Random();

    // Configuration constants
    private static final int AUTHORIZATION_RATE = 90; // 90% approval rate

    /**
     * Authorizes credit card transaction
     * Returns true 90% of the time, false 10% of the time (random)
     *
     * @param creditCardNumber The validated credit card number
     * @return true if authorized, false if declined
     */
    public boolean authorize(String creditCardNumber) {
        // Generate random number between 0-99
        // Values 0-89 = authorized (90%)
        // Values 90-99 = declined (10%)
        int randomValue = random.nextInt(100);
        boolean isAuthorized = randomValue < AUTHORIZATION_RATE;

        // Log authorization attempt (mask card number for security)
        String maskedCard = maskCardNumber(creditCardNumber);
        logger.info(String.format(
                "Authorization attempt for card %s: %s (random=%d)",
                maskedCard,
                isAuthorized ? "AUTHORIZED" : "DECLINED",
                randomValue
        ));

        return isAuthorized;
    }

    /**
     * Validates credit card format
     * Format must be: XXXX-XXXX-XXXX-XXXX (4 groups of 4 digits separated by dashes)
     *
     * @param creditCardNumber The card number to validate
     * @return true if valid format, false otherwise
     */
    public boolean validateFormat(String creditCardNumber) {
        if (creditCardNumber == null) {
            return false;
        }
        // Regex: exactly 4 groups of 4 digits, separated by dashes
        return creditCardNumber.matches("^[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}$");
    }

    /**
     * Mask card number for secure logging
     * Shows only last 4 digits: ****-****-****-1234
     *
     * @param creditCardNumber The card number to mask
     * @return Masked card number
     */
    private String maskCardNumber(String creditCardNumber) {
        if (creditCardNumber == null || creditCardNumber.length() < 4) {
            return "****-****-****-****";
        }
        return "****-****-****-" + creditCardNumber.substring(creditCardNumber.length() - 4);
    }
}