package com.sunrisedental.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents an invoice / billing record for patient treatments.
 */
public class Bill implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String billNumber; // e.g., INV-2026-0001
    private String appointmentNumber;
    private String patientName;
    private String dentistName;
    private String treatmentType;
    
    private double consultationFee;
    private double treatmentFee;
    private double extraCharges; // Medicine, x-ray, consumables
    private double discountRate; // e.g., 0.10 for 10%
    private double discountAmount;
    private String discountType; // NONE, SENIOR_CITIZEN (10%), INSURANCE (15%), PROMOTIONAL (5%)
    private double totalAmount;
    
    private String paymentStatus; // PAID, PENDING, CANCELLED
    private String paymentMethod; // CASH, CARD, ONLINE_TRANSFER
    private Date billedAt;
    private String notes;

    public Bill() {
        this.paymentStatus = "PAID";
        this.paymentMethod = "CASH";
        this.discountType = "NONE";
        this.billedAt = new Date();
    }

    public Bill(String billNumber, String appointmentNumber, String patientName, 
                String dentistName, String treatmentType, double consultationFee, 
                double treatmentFee, double extraCharges, double discountRate, 
                String discountType, String paymentMethod) {
        this.billNumber = billNumber;
        this.appointmentNumber = appointmentNumber;
        this.patientName = patientName;
        this.dentistName = dentistName;
        this.treatmentType = treatmentType;
        this.consultationFee = consultationFee;
        this.treatmentFee = treatmentFee;
        this.extraCharges = extraCharges;
        this.discountRate = discountRate;
        this.discountType = discountType;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = "PAID";
        this.billedAt = new Date();
        calculateTotals();
    }

    public void calculateTotals() {
        double subtotal = this.consultationFee + this.treatmentFee + this.extraCharges;
        this.discountAmount = subtotal * (this.discountRate > 0 ? this.discountRate : 0.0);
        this.totalAmount = Math.max(0.0, subtotal - this.discountAmount);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDentistName() {
        return dentistName;
    }

    public void setDentistName(String dentistName) {
        this.dentistName = dentistName;
    }

    public String getTreatmentType() {
        return treatmentType;
    }

    public void setTreatmentType(String treatmentType) {
        this.treatmentType = treatmentType;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
        calculateTotals();
    }

    public double getTreatmentFee() {
        return treatmentFee;
    }

    public void setTreatmentFee(double treatmentFee) {
        this.treatmentFee = treatmentFee;
        calculateTotals();
    }

    public double getExtraCharges() {
        return extraCharges;
    }

    public void setExtraCharges(double extraCharges) {
        this.extraCharges = extraCharges;
        calculateTotals();
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(double discountRate) {
        this.discountRate = discountRate;
        calculateTotals();
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Date getBilledAt() {
        return billedAt;
    }

    public void setBilledAt(Date billedAt) {
        this.billedAt = billedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
