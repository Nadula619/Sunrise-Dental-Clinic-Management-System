package com.sunrisedental.service.strategy;

/**
 * Health Insurance covered billing strategy with 15% partner coverage discount.
 */
public class InsuranceCoveredBillingStrategy implements BillingStrategy {
    private static final double DISCOUNT_RATE = 0.15; // 15%

    @Override
    public String getStrategyName() {
        return "INSURANCE";
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
