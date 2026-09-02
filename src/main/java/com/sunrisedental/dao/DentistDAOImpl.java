package com.sunrisedental.dao;

import com.sunrisedental.config.DatabaseConnection;
import com.sunrisedental.model.Dentist;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL & MongoDB implementation of IDentistDAO.
 */
public class DentistDAOImpl implements IDentistDAO {
    private static final Logger LOGGER = Logger.getLogger(DentistDAOImpl.class.getName());
    private static final Map<String, Dentist> MEMORY_STORE = new ConcurrentHashMap<>();

    public DentistDAOImpl() {
        initDefaults();
    }

    private Connection getConnection() {
        try {
            return DatabaseConnection.getInstance().getSqlConnection();
        } catch (Exception e) {
            return null;
        }
    }

    private void initDefaults() {
        if (MEMORY_STORE.isEmpty()) {
            addDefault(new Dentist("Dr. Sarah Perera", "Consultant Dental Surgeon", "0771234501", "sarah.perera@sunrisedental.lk", 2500.00, Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")));
            addDefault(new Dentist("Dr. Roshan Fernando", "Orthodontist & Implantologist", "0771234502", "roshan.f@sunrisedental.lk", 3000.00, Arrays.asList("Monday", "Wednesday", "Saturday")));
            addDefault(new Dentist("Dr. Anusha Jayawardena", "Endodontist (Root Canal Specialist)", "0771234503", "anusha.j@sunrisedental.lk", 2800.00, Arrays.asList("Tuesday", "Thursday", "Sunday")));
            addDefault(new Dentist("Dr. Dinesh Wickramasinghe", "Pediatric & Cosmetic Dentist", "0771234504", "dinesh.w@sunrisedental.lk", 2500.00, Arrays.asList("Friday", "Saturday", "Sunday")));

            for (Dentist d : MEMORY_STORE.values()) {
                save(d);
            }
        }
    }

    private void addDefault(Dentist d) {
        if (d.getId() == null) d.setId(UUID.randomUUID().toString());
        MEMORY_STORE.put(d.getName().toLowerCase(), d);
    }

    @Override
    public List<Dentist> findAll() {
        List<Dentist> list = new ArrayList<>();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM dentists ORDER BY name ASC";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        list.add(rsToDentist(rs));
                    }
                    if (!list.isEmpty()) return list;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error reading dentists from MySQL: " + e.getMessage());
        }
        return new ArrayList<>(MEMORY_STORE.values());
    }

    @Override
    public Dentist findByName(String name) {
        if (name == null) return null;

        String target = name.trim().toLowerCase();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM dentists WHERE LOWER(name) LIKE ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, "%" + target + "%");
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rsToDentist(rs);
                    }
                }
            }
        } catch (Exception e) {}

        for (Dentist d : findAll()) {
            if (d.getName() != null && d.getName().toLowerCase().contains(target)) {
                return d;
            }
        }
        return null;
    }

    @Override
    public boolean save(Dentist dentist) {
        if (dentist == null || dentist.getName() == null) return false;

        if (dentist.getId() == null) dentist.setId(UUID.randomUUID().toString());

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "INSERT INTO dentists (id, name, specialization, contact_number, email, consultation_fee, available_days) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE specialization = VALUES(specialization), contact_number = VALUES(contact_number), " +
                             "email = VALUES(email), consultation_fee = VALUES(consultation_fee), available_days = VALUES(available_days)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, dentist.getId());
                    ps.setString(2, dentist.getName());
                    ps.setString(3, dentist.getSpecialization());
                    ps.setString(4, dentist.getContactNumber());
                    ps.setString(5, dentist.getEmail());
                    ps.setDouble(6, dentist.getConsultationFee());
                    ps.setString(7, dentist.getAvailableDays() != null ? String.join(",", dentist.getAvailableDays()) : "");
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error saving dentist in MySQL: " + e.getMessage());
        }

        MEMORY_STORE.put(dentist.getName().toLowerCase(), dentist);
        return true;
    }

    private Dentist rsToDentist(ResultSet rs) throws SQLException {
        Dentist d = new Dentist();
        d.setId(rs.getString("id"));
        d.setName(rs.getString("name"));
        d.setSpecialization(rs.getString("specialization"));
        d.setContactNumber(rs.getString("contact_number"));
        d.setEmail(rs.getString("email"));
        d.setConsultationFee(rs.getDouble("consultation_fee"));
        String days = rs.getString("available_days");
        if (days != null && !days.isEmpty()) {
            d.setAvailableDays(Arrays.asList(days.split(",")));
        } else {
            d.setAvailableDays(new ArrayList<>());
        }
        return d;
    }
}
