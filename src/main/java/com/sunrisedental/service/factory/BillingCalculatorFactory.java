package com.sunrisedental.service.factory;

import com.sunrisedental.service.strategy.*;

/**
 * Factory Pattern: Creates the appropriate BillingStrategy instance based on
 * discount category or patient tier.
 */
public class BillingCalculatorFactory {

    public static BillingStrategy getStrategy(String discountType) {
        if (discountType == null || discountType.trim().isEmpty()) {
            return new StandardBillingStrategy();
        }

        switch (discountType.trim().toUpperCase()) {
            case "SENIOR_CITIZEN":
            case "SENIOR":
                return new SeniorCitizenBillingStrategy();
            case "INSURANCE":
            case "CORPORATE":
                return new InsuranceCoveredBillingStrategy();
            case "PROMOTIONAL":
            case "PROMO":
            case "LOYALTY":
                return new PromotionalBillingStrategy();
            case "STANDARD":
            case "NONE":
            default:
                return new StandardBillingStrategy();
        }
    }
}
