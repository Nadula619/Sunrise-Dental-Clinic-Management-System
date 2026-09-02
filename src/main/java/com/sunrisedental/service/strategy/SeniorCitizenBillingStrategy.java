package com.sunrisedental.service.strategy;

/**
 * Senior Citizen billing strategy with a 10% discount.
 */
public class SeniorCitizenBillingStrategy implements BillingStrategy {
    private static final double DISCOUNT_RATE = 0.10; // 10%

    @Override
    public String getStrategyName() {
        return "SENIOR_CITIZEN";
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
