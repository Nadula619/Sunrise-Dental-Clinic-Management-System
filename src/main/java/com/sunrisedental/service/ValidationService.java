package com.sunrisedental.service;

import com.sunrisedental.dao.IAppointmentDAO;
import com.sunrisedental.model.Appointment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for input validation, format integrity checks, and business constraint verification.
 */
public class ValidationService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+94|0)?[1-9]\\d{8}$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    static {
        DATE_FORMAT.setLenient(false);
    }

    private final IAppointmentDAO appointmentDAO;

    public ValidationService(IAppointmentDAO appointmentDAO) {
        this.appointmentDAO = appointmentDAO;
    }

    public List<String> validateAppointment(Appointment appointment, boolean isNew) {
        List<String> errors = new ArrayList<>();

        if (appointment == null) {
            errors.add("Appointment data cannot be null");
            return errors;
        }

        // 1. Patient Name
        if (appointment.getPatientName() == null || appointment.getPatientName().trim().isEmpty()) {
            errors.add("Patient name is required.");
        } else if (appointment.getPatientName().trim().length() < 2 || appointment.getPatientName().trim().length() > 100) {
            errors.add("Patient name must be between 2 and 100 characters.");
        }

        // 2. Contact Number
        if (appointment.getContactNumber() == null || appointment.getContactNumber().trim().isEmpty()) {
            errors.add("Contact number is required.");
        } else {
            String cleanPhone = appointment.getContactNumber().replaceAll("[\\s-]", "");
            if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
                errors.add("Invalid contact number format. Please provide a valid 10-digit number (e.g., 0771234567).");
            }
        }

        // 3. Dentist Name
        if (appointment.getDentistName() == null || appointment.getDentistName().trim().isEmpty()) {
            errors.add("Dentist selection is required.");
        }

        // 4. Treatment Type
        if (appointment.getTreatmentType() == null || appointment.getTreatmentType().trim().isEmpty()) {
            errors.add("Treatment type selection is required.");
        }

        // 5. Appointment Date
        if (appointment.getAppointmentDate() == null || appointment.getAppointmentDate().trim().isEmpty()) {
            errors.add("Appointment date is required.");
        } else {
            try {
                Date apptDate = DATE_FORMAT.parse(appointment.getAppointmentDate().trim());
                Date today = DATE_FORMAT.parse(DATE_FORMAT.format(new Date()));
                if (isNew && apptDate.before(today)) {
                    errors.add("Appointment date cannot be in the past.");
                }
            } catch (ParseException e) {
                errors.add("Invalid date format. Expected YYYY-MM-DD.");
            }
        }

        // 6. Appointment Time
        if (appointment.getAppointmentTime() == null || appointment.getAppointmentTime().trim().isEmpty()) {
            errors.add("Appointment time is required.");
        } else {
            String time = appointment.getAppointmentTime().trim();
            if (!TIME_PATTERN.matcher(time).matches()) {
                errors.add("Invalid time format. Expected HH:mm in 24-hour format (e.g., 14:30).");
            } else {
                // Clinic operating hours: 08:00 to 20:00
                String[] parts = time.split(":");
                int hour = Integer.parseInt(parts[0]);
                if (hour < 8 || hour >= 20) {
                    errors.add("Appointment time must be within clinic hours (08:00 - 20:00).");
                }
            }
        }

        // 7. Schedule Conflict Check
        if (errors.isEmpty() && appointmentDAO != null) {
            String excludeNum = isNew ? null : appointment.getAppointmentNumber();
            boolean conflict = appointmentDAO.isConflict(
                    appointment.getDentistName(),
                    appointment.getAppointmentDate(),
                    appointment.getAppointmentTime(),
                    excludeNum
            );
            if (conflict) {
                errors.add("Schedule Conflict: " + appointment.getDentistName() + 
                           " already has an appointment booked at " + appointment.getAppointmentTime() + 
                           " on " + appointment.getAppointmentDate() + ". Please choose another slot.");
            }
        }

        return errors;
    }

    public List<String> validateLogin(String username, String password) {
        List<String> errors = new ArrayList<>();
        if (username == null || username.trim().isEmpty()) {
            errors.add("Username is required.");
        }
        if (password == null || password.trim().isEmpty()) {
            errors.add("Password is required.");
        }
        return errors;
    }
}
