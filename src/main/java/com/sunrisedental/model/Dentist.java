package com.sunrisedental.model;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a dentist / dental surgeon at the clinic.
 */
public class Dentist implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String specialization;
    private String contactNumber;
    private String email;
    private double consultationFee;
    private List<String> availableDays;

    public Dentist() {
    }

    public Dentist(String name, String specialization, String contactNumber, String email, double consultationFee, List<String> availableDays) {
        this.name = name;
        this.specialization = specialization;
        this.contactNumber = contactNumber;
        this.email = email;
        this.consultationFee = consultationFee;
        this.availableDays = availableDays;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public List<String> getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(List<String> availableDays) {
        this.availableDays = availableDays;
    }
}
