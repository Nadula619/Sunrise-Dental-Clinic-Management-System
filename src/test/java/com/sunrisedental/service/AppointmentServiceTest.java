package com.sunrisedental.service;

import com.sunrisedental.dao.IAppointmentDAO;
import com.sunrisedental.model.Appointment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for AppointmentService operations.
 */
public class AppointmentServiceTest {

    private IAppointmentDAO mockDAO;
    private ValidationService mockValidationService;
    private AppointmentService appointmentService;
    private SimpleDateFormat dateFormat;

    @BeforeEach
    void setUp() {
        mockDAO = Mockito.mock(IAppointmentDAO.class);
        mockValidationService = Mockito.mock(ValidationService.class);
        appointmentService = new AppointmentService(mockDAO, mockValidationService);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    private String getFutureDate(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, days);
        return dateFormat.format(cal.getTime());
    }

    @Test
    @DisplayName("Should successfully register appointment when validation passes")
    void testRegisterAppointmentSuccess() {
        Appointment appt = new Appointment(
                "APT-2026-0001", "Kamal Silva", "Colombo",
                "0771234567", "Dr. Sarah Perera", "Scaling",
                getFutureDate(2), "10:00", "Notes"
        );

        when(mockValidationService.validateAppointment(appt, true)).thenReturn(List.of());
        when(mockDAO.createAppointment(appt)).thenReturn(true);

        AppointmentService.ServiceResult<Appointment> result = appointmentService.registerAppointment(appt);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        verify(mockDAO, times(1)).createAppointment(appt);
    }

    @Test
    @DisplayName("Should reject appointment registration when validation fails")
    void testRegisterAppointmentValidationFailure() {
        Appointment appt = new Appointment();
        when(mockValidationService.validateAppointment(appt, true))
                .thenReturn(List.of("Patient name is required", "Contact number is required"));

        AppointmentService.ServiceResult<Appointment> result = appointmentService.registerAppointment(appt);
        assertFalse(result.isSuccess());
        assertEquals(2, result.getErrors().size());
        verify(mockDAO, never()).createAppointment(any());
    }

    @Test
    @DisplayName("Should search appointments by query string")
    void testSearchAppointments() {
        Appointment a1 = new Appointment("APT-2026-0001", "Kamal Silva", "Colombo", "0771234567", "Dr. Sarah", "Cleaning", getFutureDate(1), "10:00", "");
        when(mockDAO.search("Kamal")).thenReturn(List.of(a1));

        List<Appointment> results = appointmentService.searchAppointments("Kamal");
        assertEquals(1, results.size());
        assertEquals("Kamal Silva", results.get(0).getPatientName());
    }

    @Test
    @DisplayName("Should update appointment status")
    void testUpdateStatus() {
        when(mockDAO.updateStatus("APT-2026-0001", "COMPLETED")).thenReturn(true);
        boolean ok = appointmentService.updateStatus("APT-2026-0001", "COMPLETED");
        assertTrue(ok);
        verify(mockDAO, times(1)).updateStatus("APT-2026-0001", "COMPLETED");
    }
}
