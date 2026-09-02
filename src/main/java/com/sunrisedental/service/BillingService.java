package com.sunrisedental.service;

import com.sunrisedental.dao.BillingDAOImpl;
import com.sunrisedental.dao.IBillingDAO;
import com.sunrisedental.dao.ITreatmentDAO;
import com.sunrisedental.dao.TreatmentDAOImpl;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.Bill;
import com.sunrisedental.model.Treatment;
import com.sunrisedental.service.factory.BillingCalculatorFactory;
import com.sunrisedental.service.strategy.BillingStrategy;

import java.util.List;

/**
 * Service managing billing, cost calculations, discount strategies, and payment status.
 */
public class BillingService {

    private final IBillingDAO billingDAO;
    private final ITreatmentDAO treatmentDAO;

    public BillingService() {
        this.billingDAO = new BillingDAOImpl();
        this.treatmentDAO = new TreatmentDAOImpl();
    }

    public BillingService(IBillingDAO billingDAO, ITreatmentDAO treatmentDAO) {
        this.billingDAO = billingDAO;
        this.treatmentDAO = treatmentDAO;
    }

    /**
     * Calculates bill totals using the Factory and Strategy patterns.
     */
    public Bill calculateBill(String appointmentNumber, String patientName, String dentistName,
                              String treatmentType, double consultationFee, double extraCharges,
                              String discountType, String paymentMethod) {

        double treatmentFee = 0.0;
        if (treatmentType != null) {
            Treatment treatment = treatmentDAO.findByName(treatmentType);
            if (treatment != null) {
                treatmentFee = treatment.getBasePrice();
            }
        }

        BillingStrategy strategy = BillingCalculatorFactory.getStrategy(discountType);
        double subtotal = consultationFee + treatmentFee + extraCharges;
        double discountAmount = strategy.calculateDiscount(subtotal);
        double total = strategy.calculateTotal(subtotal);

        Bill bill = new Bill();
        bill.setAppointmentNumber(appointmentNumber);
        bill.setPatientName(patientName);
        bill.setDentistName(dentistName);
        bill.setTreatmentType(treatmentType);
        bill.setConsultationFee(consultationFee);
        bill.setTreatmentFee(treatmentFee);
        bill.setExtraCharges(extraCharges);
        bill.setDiscountType(strategy.getStrategyName());
        bill.setDiscountRate(strategy.getDiscountRate());
        bill.setDiscountAmount(discountAmount);
        bill.setTotalAmount(total);
        bill.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
        bill.setPaymentStatus("PAID");

        return bill;
    }

    public Bill generateAndSaveBill(Bill bill) {
        if (bill == null) return null;

        // Apply strategy calculation to ensure consistency
        BillingStrategy strategy = BillingCalculatorFactory.getStrategy(bill.getDiscountType());
        double subtotal = bill.getConsultationFee() + bill.getTreatmentFee() + bill.getExtraCharges();
        bill.setDiscountRate(strategy.getDiscountRate());
        bill.setDiscountAmount(strategy.calculateDiscount(subtotal));
        bill.setTotalAmount(strategy.calculateTotal(subtotal));

        boolean saved = billingDAO.createBill(bill);
        return saved ? bill : null;
    }

    public Bill getByBillNumber(String billNumber) {
        return billingDAO.findByBillNumber(billNumber);
    }

    public Bill getByAppointmentNumber(String appointmentNumber) {
        return billingDAO.findByAppointmentNumber(appointmentNumber);
    }

    public List<Bill> getAllBills() {
        return billingDAO.findAll();
    }

    public List<Bill> getBillsByPatient(String patientName) {
        return billingDAO.findByPatientName(patientName);
    }

    public boolean updatePayment(String billNumber, String status, String method) {
        return billingDAO.updatePaymentStatus(billNumber, status, method);
    }

    public long getTotalBillCount() {
        return billingDAO.countBills();
    }
}
