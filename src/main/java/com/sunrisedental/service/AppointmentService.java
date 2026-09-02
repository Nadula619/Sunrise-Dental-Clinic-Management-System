package com.sunrisedental.service;

import com.sunrisedental.dao.AppointmentDAOImpl;
import com.sunrisedental.dao.IAppointmentDAO;
import com.sunrisedental.model.Appointment;

import java.util.List;

/**
 * Service managing appointment registration, schedule lookups, status transitions, and queries.
 */
public class AppointmentService {

    private final IAppointmentDAO appointmentDAO;
    private final ValidationService validationService;

    public AppointmentService() {
        this.appointmentDAO = new AppointmentDAOImpl();
        this.validationService = new ValidationService(this.appointmentDAO);
    }

    public AppointmentService(IAppointmentDAO appointmentDAO) {
        this.appointmentDAO = appointmentDAO;
        this.validationService = new ValidationService(this.appointmentDAO);
    }

    public AppointmentService(IAppointmentDAO appointmentDAO, ValidationService validationService) {
        this.appointmentDAO = appointmentDAO;
        this.validationService = validationService;
    }

    public static class ServiceResult<T> {
        private final boolean success;
        private final String message;
        private final List<String> errors;
        private final T data;

        public ServiceResult(boolean success, String message, List<String> errors, T data) {
            this.success = success;
            this.message = message;
            this.errors = errors;
            this.data = data;
        }

        public static <T> ServiceResult<T> ok(String message, T data) {
            return new ServiceResult<>(true, message, null, data);
        }

        public static <T> ServiceResult<T> fail(String message, List<String> errors) {
            return new ServiceResult<>(false, message, errors, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getErrors() {
            return errors;
        }

        public T getData() {
            return data;
        }
    }

    public ServiceResult<Appointment> registerAppointment(Appointment appointment) {
        List<String> errors = validationService.validateAppointment(appointment, true);
        if (!errors.isEmpty()) {
            return ServiceResult.fail("Validation failed for appointment registration", errors);
        }

        boolean saved = appointmentDAO.createAppointment(appointment);
        if (saved) {
            return ServiceResult.ok("Appointment successfully registered with ID: " + appointment.getAppointmentNumber(), appointment);
        } else {
            return ServiceResult.fail("Failed to persist appointment in database.", null);
        }
    }

    public ServiceResult<Appointment> updateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getAppointmentNumber() == null) {
            return ServiceResult.fail("Invalid appointment identifier.", null);
        }

        Appointment existing = appointmentDAO.findByAppointmentNumber(appointment.getAppointmentNumber());
        if (existing == null) {
            return ServiceResult.fail("Appointment not found: " + appointment.getAppointmentNumber(), null);
        }

        List<String> errors = validationService.validateAppointment(appointment, false);
        if (!errors.isEmpty()) {
            return ServiceResult.fail("Validation failed for appointment update", errors);
        }

        boolean updated = appointmentDAO.updateAppointment(appointment);
        if (updated) {
            return ServiceResult.ok("Appointment updated successfully.", appointment);
        } else {
            return ServiceResult.fail("Failed to update appointment.", null);
        }
    }

    public Appointment getByNumber(String appointmentNumber) {
        return appointmentDAO.findByAppointmentNumber(appointmentNumber);
    }

    public Appointment getById(String id) {
        return appointmentDAO.findById(id);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentDAO.findAll();
    }

    public List<Appointment> searchAppointments(String query) {
        return appointmentDAO.search(query);
    }

    public List<Appointment> getAppointmentsByDate(String date) {
        return appointmentDAO.findByDate(date);
    }

    public List<Appointment> getAppointmentsByDentist(String dentistName) {
        return appointmentDAO.findByDentist(dentistName);
    }

    public boolean updateStatus(String appointmentNumber, String status) {
        return appointmentDAO.updateStatus(appointmentNumber, status);
    }

    public long getTotalAppointmentCount() {
        return appointmentDAO.countAppointments();
    }
}
