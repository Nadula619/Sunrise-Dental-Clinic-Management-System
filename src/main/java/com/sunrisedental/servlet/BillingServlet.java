package com.sunrisedental.servlet;

import com.sunrisedental.model.Bill;
import com.sunrisedental.service.BillingService;
import com.sunrisedental.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST Web Service Servlet for Billing, Invoices, and Payment Processing.
 */
@WebServlet(name = "BillingServlet", urlPatterns = {"/api/billing/*"})
public class BillingServlet extends HttpServlet {

    private BillingService billingService;

    @Override
    public void init() throws ServletException {
        this.billingService = new BillingService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || "/".equals(pathInfo)) {
            String patient = req.getParameter("patient");
            List<Bill> bills;
            if (patient != null && !patient.trim().isEmpty()) {
                bills = billingService.getBillsByPatient(patient.trim());
            } else {
                bills = billingService.getAllBills();
            }
            JsonUtil.sendSuccess(resp, "Bills retrieved successfully", bills);
        } else if (pathInfo.startsWith("/appointment/")) {
            String apptNumber = pathInfo.replaceFirst("/appointment/", "");
            Bill bill = billingService.getByAppointmentNumber(apptNumber);
            if (bill != null) {
                JsonUtil.sendSuccess(resp, "Bill for appointment found", bill);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "No bill found for appointment: " + apptNumber);
            }
        } else {
            String billNumber = pathInfo.replaceFirst("/", "");
            Bill bill = billingService.getByBillNumber(billNumber);
            if (bill != null) {
                JsonUtil.sendSuccess(resp, "Bill found", bill);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Bill not found: " + billNumber);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        StringBuilder sb = readBody(req);

        if ("/calculate".equals(pathInfo)) {
            // Dry run calculation endpoint
            Map<?, ?> reqData = JsonUtil.fromJson(sb.toString(), Map.class);
            if (reqData == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid calculation payload");
                return;
            }

            String apptNo = (String) reqData.get("appointmentNumber");
            String patient = (String) reqData.get("patientName");
            String dentist = (String) reqData.get("dentistName");
            String treatment = (String) reqData.get("treatmentType");
            double consultation = reqData.get("consultationFee") != null ? Double.parseDouble(reqData.get("consultationFee").toString()) : 0.0;
            double extra = reqData.get("extraCharges") != null ? Double.parseDouble(reqData.get("extraCharges").toString()) : 0.0;
            String discountType = (String) reqData.get("discountType");
            String payMethod = (String) reqData.get("paymentMethod");

            Bill calculated = billingService.calculateBill(apptNo, patient, dentist, treatment, consultation, extra, discountType, payMethod);
            JsonUtil.sendSuccess(resp, "Bill calculated successfully", calculated);
        } else if (pathInfo != null && pathInfo.contains("/payment")) {
            // Update payment status
            String billNumber = pathInfo.split("/")[1];
            Map<?, ?> payload = JsonUtil.fromJson(sb.toString(), Map.class);
            String status = payload != null ? (String) payload.get("status") : "PAID";
            String method = payload != null ? (String) payload.get("method") : "CASH";

            if (billingService.updatePayment(billNumber, status, method)) {
                JsonUtil.sendSuccess(resp, "Payment status updated", null);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Failed to update payment status");
            }
        } else {
            // Create and persist new bill
            Bill bill = JsonUtil.fromJson(sb.toString(), Bill.class);
            if (bill == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid bill payload");
                return;
            }

            Bill saved = billingService.generateAndSaveBill(bill);
            if (saved != null) {
                JsonUtil.sendSuccess(resp, "Bill successfully generated with number: " + saved.getBillNumber(), saved);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate bill.");
            }
        }
    }

    private StringBuilder readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb;
    }
}
