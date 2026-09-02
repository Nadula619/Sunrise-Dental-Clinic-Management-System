package com.sunrisedental.model;

import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulates aggregate statistics and analytics for administrative decision-making reports.
 */
public class ReportSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private long totalAppointments;
    private long completedAppointments;
    private long scheduledAppointments;
    private long cancelledAppointments;

    private double totalRevenue;
    private double paidRevenue;
    private double pendingRevenue;

    private Map<String, Long> appointmentsByDentist;
    private Map<String, Long> appointmentsByTreatment;
    private Map<String, Double> revenueByTreatment;
    private Map<String, Double> revenueByDentist;

    public ReportSummary() {
    }

    public long getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(long totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public long getCompletedAppointments() {
        return completedAppointments;
    }

    public void setCompletedAppointments(long completedAppointments) {
        this.completedAppointments = completedAppointments;
    }

    public long getScheduledAppointments() {
        return scheduledAppointments;
    }

    public void setScheduledAppointments(long scheduledAppointments) {
        this.scheduledAppointments = scheduledAppointments;
    }

    public long getCancelledAppointments() {
        return cancelledAppointments;
    }

    public void setCancelledAppointments(long cancelledAppointments) {
        this.cancelledAppointments = cancelledAppointments;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public double getPaidRevenue() {
        return paidRevenue;
    }

    public void setPaidRevenue(double paidRevenue) {
        this.paidRevenue = paidRevenue;
    }

    public double getPendingRevenue() {
        return pendingRevenue;
    }

    public void setPendingRevenue(double pendingRevenue) {
        this.pendingRevenue = pendingRevenue;
    }

    public Map<String, Long> getAppointmentsByDentist() {
        return appointmentsByDentist;
    }

    public void setAppointmentsByDentist(Map<String, Long> appointmentsByDentist) {
        this.appointmentsByDentist = appointmentsByDentist;
    }

    public Map<String, Long> getAppointmentsByTreatment() {
        return appointmentsByTreatment;
    }

    public void setAppointmentsByTreatment(Map<String, Long> appointmentsByTreatment) {
        this.appointmentsByTreatment = appointmentsByTreatment;
    }

    public Map<String, Double> getRevenueByTreatment() {
        return revenueByTreatment;
    }

    public void setRevenueByTreatment(Map<String, Double> revenueByTreatment) {
        this.revenueByTreatment = revenueByTreatment;
    }

    public Map<String, Double> getRevenueByDentist() {
        return revenueByDentist;
    }

    public void setRevenueByDentist(Map<String, Double> revenueByDentist) {
        this.revenueByDentist = revenueByDentist;
    }
}
