package com.sunrisedental.service;

import com.sunrisedental.dao.IAppointmentDAO;
import com.sunrisedental.model.Appointment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * TDD Test Suite for ValidationService (Input Validation & Schedule Conflict Verification).
 */
public class ValidationServiceTest {

    private IAppointmentDAO mockAppointmentDAO;
    private ValidationService validationService;
    private SimpleDateFormat dateFormat;

    @BeforeEach
    void setUp() {
        mockAppointmentDAO = Mockito.mock(IAppointmentDAO.class);
        validationService = new ValidationService(mockAppointmentDAO);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    private String getFutureDate(int daysInFuture) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysInFuture);
        return dateFormat.format(cal.getTime());
    }

    @Test
    @DisplayName("Should pass validation for a completely valid appointment")
    void testValidAppointmentPasses() {
        when(mockAppointmentDAO.isConflict(anyString(), anyString(), anyString(), any())).thenReturn(false);

        Appointment appt = new Appointment(
                "APT-2026-0001",
                "Kasun Jayasinghe",
                "No 10, Station Road, Colombo",
                "0771234567",
                "Dr. Sarah Perera",
                "Teeth Cleaning & Polishing (Scaling)",
                getFutureDate(2),
                "10:00",
                "Routine checkup"
        );

        List<String> errors = validationService.validateAppointment(appt, true);
        assertTrue(errors.isEmpty(), "Errors list should be empty for a valid appointment");
    }

    @Test
    @DisplayName("Should fail when appointment is null")
    void testNullAppointment() {
        List<String> errors = validationService.validateAppointment(null, true);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Appointment data cannot be null"));
    }

    @Test
    @DisplayName("Should fail when patient name is empty or too short")
    void testInvalidPatientName() {
        Appointment appt = new Appointment();
        appt.setPatientName("");
        appt.setContactNumber("0771234567");
        appt.setDentistName("Dr. Sarah Perera");
        appt.setTreatmentType("Scaling");
        appt.setAppointmentDate(getFutureDate(1));
        appt.setAppointmentTime("10:00");

        List<String> errors = validationService.validateAppointment(appt, true);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Patient name is required")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "abcd", "077123", "0077123456789", "phone"})
    @DisplayName("Should fail when contact number format is invalid")
    void testInvalidContactNumber(String invalidPhone) {
        Appointment appt = new Appointment();
        appt.setPatientName("Kasun Silva");
        appt.setContactNumber(invalidPhone);
        appt.setDentistName("Dr. Sarah Perera");
        appt.setTreatmentType("Scaling");
        appt.setAppointmentDate(getFutureDate(1));
        appt.setAppointmentTime("10:00");

        List<String> errors = validationService.validateAppointment(appt, true);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid contact number format")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0771234567", "0712345678", "0112345678", "+94771234567"})
    @DisplayName("Should accept valid Sri Lankan contact numbers")
    void testValidContactNumbers(String validPhone) {
        when(mockAppointmentDAO.isConflict(anyString(), anyString(), anyString(), any())).thenReturn(false);

        Appointment appt = new Appointment();
        appt.setPatientName("Kasun Silva");
        appt.setContactNumber(validPhone);
        appt.setDentistName("Dr. Sarah Perera");
        appt.setTreatmentType("Scaling");
        appt.setAppointmentDate(getFutureDate(1));
        appt.setAppointmentTime("10:00");

        List<String> errors = validationService.validateAppointment(appt, true);
        assertFalse(errors.stream().anyMatch(e -> e.contains("contact number")));
    }

    @Test
    @DisplayName("Should fail when appointment date is in the past for new booking")
    void testPastDateFails() {
        Appointment appt = new Appointment();
        appt.setPatientName("Kasun Silva");
        appt.setContactNumber("0771234567");
        appt.setDentistName("Dr. Sarah Perera");
        appt.setTreatmentType("Scaling");
        appt.setAppointmentDate("2020-01-01");
        appt.setAppointmentTime("10:00");

        List<String> errors = validationService.validateAppointment(appt, true);
        assertTrue(errors.stream().anyMatch(e -> e.contains("cannot be in the past")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"06:30", "07:45", "20:30", "22:00", "25:00", "invalid"})
    @DisplayName("Should fail when appointment time is outside operating hours (08:00 - 20:00)")
    void testInvalidOperatingHours(String invalidTime) {
        Appointment appt = new Appointment();
        appt.setPatientName("Kasun Silva");
        appt.setContactNumber("0771234567");
        appt.setDentistName("Dr. Sarah Perera");
        appt.setTreatmentType("Scaling");
        appt.setAppointmentDate(getFutureDate(1));
        appt.setAppointmentTime(invalidTime);

        List<String> errors = validationService.validateAppointment(appt, true);
        assertTrue(errors.stream().anyMatch(e -> e.contains("time")));
    }

    @Test
    @DisplayName("Should detect double-booking / schedule conflict for the same dentist at same time")
    void testScheduleConflictDetection() {
        when(mockAppointmentDAO.isConflict(eq("Dr. Sarah Perera"), anyString(), eq("14:30"), any()))
                .thenReturn(true);

        Appointment appt = new Appointment();
        appt.setPatientName("Kasun Silva");
        appt.setContactNumber("0771234567");
        appt.setDentistName("Dr. Sarah Perera");
        appt.setTreatmentType("Root Canal Treatment");
        appt.setAppointmentDate(getFutureDate(1));
        appt.setAppointmentTime("14:30");

        List<String> errors = validationService.validateAppointment(appt, true);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Schedule Conflict")));
    }
}
