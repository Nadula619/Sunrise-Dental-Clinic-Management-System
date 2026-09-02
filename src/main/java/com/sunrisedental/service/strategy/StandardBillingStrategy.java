package com.sunrisedental.service.strategy;

/**
 * Standard billing strategy with no discounts.
 */
public class StandardBillingStrategy implements BillingStrategy {
    @Override
    public String getStrategyName() {
        return "STANDARD";
    }

    @Override
    public double getDiscountRate() {
        return 0.0;
    }

    @Override
    public double calculateDiscount(double subtotal) {
        return 0.0;
    }

    @Override
    public double calculateTotal(double subtotal) {
        return Math.max(0.0, subtotal);
    }
}
