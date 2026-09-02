package com.sunrisedental.service.strategy;

/**
 * Strategy Pattern Interface for applying discounts and pricing policies.
 */
public interface BillingStrategy {
    String getStrategyName();
    double getDiscountRate();
    double calculateDiscount(double subtotal);
    double calculateTotal(double subtotal);
}
