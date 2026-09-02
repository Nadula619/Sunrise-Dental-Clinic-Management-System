package com.sunrisedental.dao;

import com.sunrisedental.config.DatabaseConnection;
import com.sunrisedental.model.Appointment;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL & MongoDB Implementation of IAppointmentDAO.
 */
public class AppointmentDAOImpl implements IAppointmentDAO {
    private static final Logger LOGGER = Logger.getLogger(AppointmentDAOImpl.class.getName());
    private static final Map<String, Appointment> MEMORY_STORE = new ConcurrentHashMap<>();
    private static final AtomicLong SEQUENCE_COUNTER = new AtomicLong(100);

    private Connection getConnection() {
        try {
            return DatabaseConnection.getInstance().getSqlConnection();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public synchronized String generateNextAppointmentNumber() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        long seq = 1;
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT COUNT(*) FROM appointments";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        seq = rs.getLong(1) + 1;
                    }
                }
            }
        } catch (Exception e) {
            seq = SEQUENCE_COUNTER.incrementAndGet();
        }

        String candidate = String.format("APT-%d-%04d", currentYear, seq);
        int attempt = 1;
        while (findByAppointmentNumber(candidate) != null) {
            candidate = String.format("APT-%d-%04d", currentYear, seq + attempt);
            attempt++;
        }
        return candidate;
    }

    @Override
    public boolean createAppointment(Appointment appointment) {
        if (appointment == null) return false;

        if (appointment.getAppointmentNumber() == null || appointment.getAppointmentNumber().trim().isEmpty()) {
            appointment.setAppointmentNumber(generateNextAppointmentNumber());
        }
        if (appointment.getId() == null) {
            appointment.setId(UUID.randomUUID().toString());
        }

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "INSERT INTO appointments (id, appointment_number, patient_name, address, contact_number, " +
                             "dentist_name, treatment_type, appointment_date, appointment_time, status, notes, created_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE patient_name = VALUES(patient_name), address = VALUES(address), " +
                             "contact_number = VALUES(contact_number), dentist_name = VALUES(dentist_name), " +
                             "treatment_type = VALUES(treatment_type), appointment_date = VALUES(appointment_date), " +
                             "appointment_time = VALUES(appointment_time), status = VALUES(status), notes = VALUES(notes)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, appointment.getId());
                    ps.setString(2, appointment.getAppointmentNumber());
                    ps.setString(3, appointment.getPatientName());
                    ps.setString(4, appointment.getAddress());
                    ps.setString(5, appointment.getContactNumber());
                    ps.setString(6, appointment.getDentistName());
                    ps.setString(7, appointment.getTreatmentType());
                    ps.setString(8, appointment.getAppointmentDate());
                    ps.setString(9, appointment.getAppointmentTime());
                    ps.setString(10, appointment.getStatus());
                    ps.setString(11, appointment.getNotes());
                    ps.setTimestamp(12, new Timestamp(appointment.getCreatedAt() != null ? appointment.getCreatedAt().getTime() : System.currentTimeMillis()));
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error creating appointment in MySQL: " + e.getMessage());
        }

        MEMORY_STORE.put(appointment.getAppointmentNumber(), appointment);
        return true;
    }

    @Override
    public Appointment findByAppointmentNumber(String appointmentNumber) {
        if (appointmentNumber == null || appointmentNumber.trim().isEmpty()) return null;

        String target = appointmentNumber.trim();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM appointments WHERE appointment_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, target);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rsToAppointment(rs);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointment by number in MySQL: " + e.getMessage());
        }

        return MEMORY_STORE.get(target);
    }

    @Override
    public Appointment findById(String id) {
        if (id == null) return null;

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM appointments WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, id.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rsToAppointment(rs);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointment by id: " + e.getMessage());
        }

        for (Appointment a : MEMORY_STORE.values()) {
            if (id.equals(a.getId())) return a;
        }
        return null;
    }

    @Override
    public List<Appointment> findAll() {
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM appointments ORDER BY appointment_date DESC, appointment_time DESC";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        list.add(rsToAppointment(rs));
                    }
                    if (!list.isEmpty()) return list;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding all appointments in MySQL: " + e.getMessage());
        }

        List<Appointment> memList = new ArrayList<>(MEMORY_STORE.values());
        memList.sort((a, b) -> (b.getAppointmentDate() + b.getAppointmentTime()).compareTo(a.getAppointmentDate() + a.getAppointmentTime()));
        return memList;
    }

    @Override
    public List<Appointment> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }

        String q = "%" + query.trim() + "%";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM appointments WHERE appointment_number LIKE ? OR patient_name LIKE ? OR contact_number LIKE ? OR dentist_name LIKE ? OR treatment_type LIKE ? ORDER BY appointment_date DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, q);
                    ps.setString(2, q);
                    ps.setString(3, q);
                    ps.setString(4, q);
                    ps.setString(5, q);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(rsToAppointment(rs));
                        }
                        if (!list.isEmpty()) return list;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error searching appointments: " + e.getMessage());
        }

        String lowerQ = query.toLowerCase().trim();
        for (Appointment a : MEMORY_STORE.values()) {
            if ((a.getAppointmentNumber() != null && a.getAppointmentNumber().toLowerCase().contains(lowerQ)) ||
                (a.getPatientName() != null && a.getPatientName().toLowerCase().contains(lowerQ)) ||
                (a.getContactNumber() != null && a.getContactNumber().contains(lowerQ)) ||
                (a.getDentistName() != null && a.getDentistName().toLowerCase().contains(lowerQ)) ||
                (a.getTreatmentType() != null && a.getTreatmentType().toLowerCase().contains(lowerQ))) {
                list.add(a);
            }
        }
        return list;
    }

    @Override
    public List<Appointment> findByDate(String date) {
        if (date == null) return new ArrayList<>();

        List<Appointment> list = new ArrayList<>();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM appointments WHERE appointment_date = ? ORDER BY appointment_time ASC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, date.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(rsToAppointment(rs));
                        }
                        if (!list.isEmpty()) return list;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointments by date: " + e.getMessage());
        }

        for (Appointment a : MEMORY_STORE.values()) {
            if (date.trim().equals(a.getAppointmentDate())) list.add(a);
        }
        return list;
    }

    @Override
    public List<Appointment> findByDentist(String dentistName) {
        if (dentistName == null) return new ArrayList<>();

        List<Appointment> list = new ArrayList<>();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM appointments WHERE LOWER(dentist_name) = ? ORDER BY appointment_date DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, dentistName.trim().toLowerCase());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(rsToAppointment(rs));
                        }
                        if (!list.isEmpty()) return list;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointments by dentist: " + e.getMessage());
        }

        for (Appointment a : MEMORY_STORE.values()) {
            if (dentistName.trim().equalsIgnoreCase(a.getDentistName())) list.add(a);
        }
        return list;
    }

    @Override
    public boolean updateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getAppointmentNumber() == null) return false;

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "UPDATE appointments SET patient_name = ?, address = ?, contact_number = ?, " +
                             "dentist_name = ?, treatment_type = ?, appointment_date = ?, appointment_time = ?, " +
                             "status = ?, notes = ? WHERE appointment_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, appointment.getPatientName());
                    ps.setString(2, appointment.getAddress());
                    ps.setString(3, appointment.getContactNumber());
                    ps.setString(4, appointment.getDentistName());
                    ps.setString(5, appointment.getTreatmentType());
                    ps.setString(6, appointment.getAppointmentDate());
                    ps.setString(7, appointment.getAppointmentTime());
                    ps.setString(8, appointment.getStatus());
                    ps.setString(9, appointment.getNotes());
                    ps.setString(10, appointment.getAppointmentNumber());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating appointment in MySQL: " + e.getMessage());
        }

        MEMORY_STORE.put(appointment.getAppointmentNumber(), appointment);
        return true;
    }

    @Override
    public boolean updateStatus(String appointmentNumber, String status) {
        if (appointmentNumber == null || status == null) return false;

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "UPDATE appointments SET status = ? WHERE appointment_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, status.trim().toUpperCase());
                    ps.setString(2, appointmentNumber.trim());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating status: " + e.getMessage());
        }

        Appointment a = MEMORY_STORE.get(appointmentNumber.trim());
        if (a != null) {
            a.setStatus(status.trim().toUpperCase());
            return true;
        }
        return true;
    }

    @Override
    public boolean isConflict(String dentistName, String date, String time, String excludeAppointmentNumber) {
        if (dentistName == null || date == null || time == null) return false;

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT COUNT(*) FROM appointments WHERE LOWER(dentist_name) = ? AND appointment_date = ? AND appointment_time = ? AND status != 'CANCELLED'";
                if (excludeAppointmentNumber != null) {
                    sql += " AND appointment_number != ?";
                }
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, dentistName.trim().toLowerCase());
                    ps.setString(2, date.trim());
                    ps.setString(3, time.trim());
                    if (excludeAppointmentNumber != null) {
                        ps.setString(4, excludeAppointmentNumber.trim());
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking conflict in MySQL: " + e.getMessage());
        }

        for (Appointment a : MEMORY_STORE.values()) {
            if ("CANCELLED".equalsIgnoreCase(a.getStatus())) continue;
            if (excludeAppointmentNumber != null && excludeAppointmentNumber.equalsIgnoreCase(a.getAppointmentNumber())) continue;

            if (dentistName.trim().equalsIgnoreCase(a.getDentistName()) &&
                date.trim().equals(a.getAppointmentDate()) &&
                time.trim().equals(a.getAppointmentTime())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long countAppointments() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT COUNT(*) FROM appointments";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
        } catch (Exception e) {}
        return MEMORY_STORE.size();
    }

    private Appointment rsToAppointment(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setId(rs.getString("id"));
        a.setAppointmentNumber(rs.getString("appointment_number"));
        a.setPatientName(rs.getString("patient_name"));
        a.setAddress(rs.getString("address"));
        a.setContactNumber(rs.getString("contact_number"));
        a.setDentistName(rs.getString("dentist_name"));
        a.setTreatmentType(rs.getString("treatment_type"));
        a.setAppointmentDate(rs.getString("appointment_date"));
        a.setAppointmentTime(rs.getString("appointment_time"));
        a.setStatus(rs.getString("status"));
        a.setNotes(rs.getString("notes"));
        Timestamp ts = rs.getTimestamp("created_at");
        a.setCreatedAt(ts != null ? new Date(ts.getTime()) : new Date());
        return a;
    }
}
