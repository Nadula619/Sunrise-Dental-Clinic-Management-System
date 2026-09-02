package com.sunrisedental.dao;

import com.sunrisedental.model.Appointment;
import java.util.List;

/**
 * Data Access Object Interface for Appointments and Patient Bookings.
 */
public interface IAppointmentDAO {
    boolean createAppointment(Appointment appointment);
    Appointment findByAppointmentNumber(String appointmentNumber);
    Appointment findById(String id);
    List<Appointment> findAll();
    List<Appointment> search(String query);
    List<Appointment> findByDate(String date);
    List<Appointment> findByDentist(String dentistName);
    boolean updateAppointment(Appointment appointment);
    boolean updateStatus(String appointmentNumber, String status);
    boolean isConflict(String dentistName, String date, String time, String excludeAppointmentNumber);
    long countAppointments();
    String generateNextAppointmentNumber();
}
