package com.sunrisedental.service;

import com.sunrisedental.dao.AppointmentDAOImpl;
import com.sunrisedental.dao.BillingDAOImpl;
import com.sunrisedental.dao.IAppointmentDAO;
import com.sunrisedental.dao.IBillingDAO;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.Bill;
import com.sunrisedental.model.ReportSummary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service generating aggregate management metrics and decision-making reports.
 */
public class ReportService {

    private final IAppointmentDAO appointmentDAO;
    private final IBillingDAO billingDAO;

    public ReportService() {
        this.appointmentDAO = new AppointmentDAOImpl();
        this.billingDAO = new BillingDAOImpl();
    }

    public ReportService(IAppointmentDAO appointmentDAO, IBillingDAO billingDAO) {
        this.appointmentDAO = appointmentDAO;
        this.billingDAO = billingDAO;
    }

    public ReportSummary generateSummary() {
        ReportSummary summary = new ReportSummary();

        List<Appointment> appointments = appointmentDAO.findAll();
        List<Bill> bills = billingDAO.findAll();

        long totalAppt = appointments.size();
        long completed = 0;
        long scheduled = 0;
        long cancelled = 0;

        Map<String, Long> byDentist = new HashMap<>();
        Map<String, Long> byTreatment = new HashMap<>();

        for (Appointment a : appointments) {
            String status = a.getStatus() != null ? a.getStatus().toUpperCase() : "SCHEDULED";
            switch (status) {
                case "COMPLETED":
                    completed++;
                    break;
                case "CANCELLED":
                    cancelled++;
                    break;
                case "SCHEDULED":
                case "IN_PROGRESS":
                default:
                    scheduled++;
                    break;
            }

            if (a.getDentistName() != null && !a.getDentistName().trim().isEmpty()) {
                byDentist.put(a.getDentistName(), byDentist.getOrDefault(a.getDentistName(), 0L) + 1);
            }
            if (a.getTreatmentType() != null && !a.getTreatmentType().trim().isEmpty()) {
                byTreatment.put(a.getTreatmentType(), byTreatment.getOrDefault(a.getTreatmentType(), 0L) + 1);
            }
        }

        summary.setTotalAppointments(totalAppt);
        summary.setCompletedAppointments(completed);
        summary.setScheduledAppointments(scheduled);
        summary.setCancelledAppointments(cancelled);
        summary.setAppointmentsByDentist(byDentist);
        summary.setAppointmentsByTreatment(byTreatment);

        double totalRev = 0.0;
        double paidRev = 0.0;
        double pendingRev = 0.0;

        Map<String, Double> revByTreatment = new HashMap<>();
        Map<String, Double> revByDentist = new HashMap<>();

        for (Bill b : bills) {
            totalRev += b.getTotalAmount();
            if ("PAID".equalsIgnoreCase(b.getPaymentStatus())) {
                paidRev += b.getTotalAmount();
            } else {
                pendingRev += b.getTotalAmount();
            }

            if (b.getTreatmentType() != null) {
                revByTreatment.put(b.getTreatmentType(), revByTreatment.getOrDefault(b.getTreatmentType(), 0.0) + b.getTotalAmount());
            }
            if (b.getDentistName() != null) {
                revByDentist.put(b.getDentistName(), revByDentist.getOrDefault(b.getDentistName(), 0.0) + b.getTotalAmount());
            }
        }

        summary.setTotalRevenue(totalRev);
        summary.setPaidRevenue(paidRev);
        summary.setPendingRevenue(pendingRev);
        summary.setRevenueByTreatment(revByTreatment);
        summary.setRevenueByDentist(revByDentist);

        return summary;
    }
}
