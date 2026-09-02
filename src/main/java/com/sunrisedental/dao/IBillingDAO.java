package com.sunrisedental.dao;

import com.sunrisedental.model.Bill;
import java.util.List;

/**
 * Data Access Object Interface for Billing & Receipts.
 */
public interface IBillingDAO {
    boolean createBill(Bill bill);
    Bill findByBillNumber(String billNumber);
    Bill findByAppointmentNumber(String appointmentNumber);
    List<Bill> findAll();
    List<Bill> findByPatientName(String patientName);
    boolean updatePaymentStatus(String billNumber, String status, String method);
    long countBills();
    String generateNextBillNumber();
}
