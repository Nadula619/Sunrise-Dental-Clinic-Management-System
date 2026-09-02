package com.sunrisedental.service.strategy;

/**
 * Promotional / Clinic loyalty discount strategy with 5% discount.
 */
public class PromotionalBillingStrategy implements BillingStrategy {
    private static final double DISCOUNT_RATE = 0.05; // 5%

    @Override
    public String getStrategyName() {
        return "PROMOTIONAL";
    }

    @Override
    public double getDiscountRate() {
        return DISCOUNT_RATE;
    }

    @Override
    public double calculateDiscount(double subtotal) {
        return subtotal > 0 ? subtotal * DISCOUNT_RATE : 0.0;
    }

    @Override
    public double calculateTotal(double subtotal) {
        return Math.max(0.0, subtotal - calculateDiscount(subtotal));
    }
}
