package com.sunrisedental.model;

import java.io.Serializable;

/**
 * Represents a dental treatment procedure and base pricing.
 */
public class Treatment implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code; // e.g. TRT-001
    private String name;
    private String category;
    private double basePrice;
    private int estimatedMinutes;
    private String description;

    public Treatment() {
    }

    public Treatment(String code, String name, String category, double basePrice, int estimatedMinutes, String description) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
        this.estimatedMinutes = estimatedMinutes;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
