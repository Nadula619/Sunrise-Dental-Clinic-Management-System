package com.sunrisedental.servlet;

import com.sunrisedental.model.Appointment;
import com.sunrisedental.service.AppointmentService;
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
 * REST Web Service Servlet for Appointment Registration, Lookup, and Status Management.
 */
@WebServlet(name = "AppointmentServlet", urlPatterns = {"/api/appointments/*"})
public class AppointmentServlet extends HttpServlet {

    private AppointmentService appointmentService;

    @Override
    public void init() throws ServletException {
        this.appointmentService = new AppointmentService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || "/".equals(pathInfo)) {
            String search = req.getParameter("search");
            String date = req.getParameter("date");
            String dentist = req.getParameter("dentist");

            List<Appointment> list;
            if (search != null && !search.trim().isEmpty()) {
                list = appointmentService.searchAppointments(search.trim());
            } else if (date != null && !date.trim().isEmpty()) {
                list = appointmentService.getAppointmentsByDate(date.trim());
            } else if (dentist != null && !dentist.trim().isEmpty()) {
                list = appointmentService.getAppointmentsByDentist(dentist.trim());
            } else {
                list = appointmentService.getAllAppointments();
            }

            JsonUtil.sendSuccess(resp, "Appointments retrieved successfully", list);
        } else {
            String apptNumber = pathInfo.replaceFirst("/", "");
            Appointment appt = appointmentService.getByNumber(apptNumber);
            if (appt == null) {
                appt = appointmentService.getById(apptNumber);
            }

            if (appt != null) {
                JsonUtil.sendSuccess(resp, "Appointment details retrieved", appt);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Appointment not found: " + apptNumber);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.contains("/status")) {
            // Update status endpoint: POST /api/appointments/{id}/status
            String[] parts = pathInfo.split("/");
            if (parts.length >= 2) {
                String apptNumber = parts[1];
                StringBuilder sb = readBody(req);
                Map<?, ?> payload = JsonUtil.fromJson(sb.toString(), Map.class);
                String status = payload != null ? (String) payload.get("status") : null;

                if (status != null && appointmentService.updateStatus(apptNumber, status)) {
                    JsonUtil.sendSuccess(resp, "Appointment status updated to " + status, null);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Failed to update appointment status.");
                }
                return;
            }
        }

        // Create new appointment: POST /api/appointments
        StringBuilder sb = readBody(req);
        Appointment appt = JsonUtil.fromJson(sb.toString(), Appointment.class);

        AppointmentService.ServiceResult<Appointment> result = appointmentService.registerAppointment(appt);
        if (result.isSuccess()) {
            JsonUtil.sendSuccess(resp, result.getMessage(), result.getData());
        } else {
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("success", false);
            errorMap.put("error", result.getMessage());
            errorMap.put("details", result.getErrors());
            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, errorMap);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Appointment number required in URL path");
            return;
        }

        String apptNumber = pathInfo.replaceFirst("/", "");
        StringBuilder sb = readBody(req);
        Appointment appt = JsonUtil.fromJson(sb.toString(), Appointment.class);
        if (appt != null && (appt.getAppointmentNumber() == null || appt.getAppointmentNumber().isEmpty())) {
            appt.setAppointmentNumber(apptNumber);
        }

        AppointmentService.ServiceResult<Appointment> result = appointmentService.updateAppointment(appt);
        if (result.isSuccess()) {
            JsonUtil.sendSuccess(resp, result.getMessage(), result.getData());
        } else {
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("success", false);
            errorMap.put("error", result.getMessage());
            errorMap.put("details", result.getErrors());
            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, errorMap);
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
