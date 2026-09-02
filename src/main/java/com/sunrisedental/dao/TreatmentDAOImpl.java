package com.sunrisedental.dao;

import com.sunrisedental.config.DatabaseConnection;
import com.sunrisedental.model.Treatment;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL & MongoDB implementation of ITreatmentDAO with preset catalog.
 */
public class TreatmentDAOImpl implements ITreatmentDAO {
    private static final Logger LOGGER = Logger.getLogger(TreatmentDAOImpl.class.getName());
    private static final Map<String, Treatment> MEMORY_STORE = new ConcurrentHashMap<>();

    public TreatmentDAOImpl() {
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
            addDefault(new Treatment("TRT-001", "Routine Dental Checkup & Consultation", "Preventive", 2500.00, 30, "Comprehensive oral exam and diagnosis"));
            addDefault(new Treatment("TRT-002", "Teeth Cleaning & Polishing (Scaling)", "Hygiene", 4500.00, 45, "Ultrasonic scaling and enamel polishing"));
            addDefault(new Treatment("TRT-003", "Composite Dental Filling", "Restorative", 6000.00, 40, "Tooth-colored composite resin filling per tooth"));
            addDefault(new Treatment("TRT-004", "Root Canal Treatment (RCT)", "Endodontics", 18000.00, 75, "Single/multi-canal endodontic therapy"));
            addDefault(new Treatment("TRT-005", "Tooth Extraction (Simple)", "Surgery", 5000.00, 30, "Non-surgical extraction under local anesthesia"));
            addDefault(new Treatment("TRT-006", "Surgical Wisdom Tooth Removal", "Surgery", 22000.00, 90, "Impacted third molar surgical removal"));
            addDefault(new Treatment("TRT-007", "Teeth Whitening & Bleaching", "Cosmetic", 25000.00, 60, "In-office laser whitening session"));
            addDefault(new Treatment("TRT-008", "Porcelain Crown / Bridge", "Prosthodontics", 28000.00, 60, "High-grade ceramic crown placement"));
            addDefault(new Treatment("TRT-009", "Orthodontic Consultation & Braces", "Orthodontics", 45000.00, 60, "Orthodontic assessment and appliance fitting"));

            // Sync to MySQL
            for (Treatment t : MEMORY_STORE.values()) {
                save(t);
            }
        }
    }

    private void addDefault(Treatment t) {
        MEMORY_STORE.put(t.getCode(), t);
    }

    @Override
    public List<Treatment> findAll() {
        List<Treatment> list = new ArrayList<>();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM treatments ORDER BY code ASC";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        list.add(rsToTreatment(rs));
                    }
                    if (!list.isEmpty()) return list;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error reading treatments from MySQL: " + e.getMessage());
        }
        return new ArrayList<>(MEMORY_STORE.values());
    }

    @Override
    public Treatment findByCode(String code) {
        if (code == null) return null;

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM treatments WHERE code = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, code.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rsToTreatment(rs);
                    }
                }
            }
        } catch (Exception e) {}

        return MEMORY_STORE.get(code.trim());
    }

    @Override
    public Treatment findByName(String name) {
        if (name == null) return null;

        String target = name.trim().toLowerCase();
        for (Treatment t : findAll()) {
            if (t.getName() != null && t.getName().toLowerCase().equalsIgnoreCase(target)) {
                return t;
            }
        }
        for (Treatment t : findAll()) {
            if (t.getName() != null && t.getName().toLowerCase().contains(target)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public boolean save(Treatment treatment) {
        if (treatment == null || treatment.getCode() == null) return false;

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "INSERT INTO treatments (code, name, category, base_price, estimated_minutes, description) " +
                             "VALUES (?, ?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE name = VALUES(name), category = VALUES(category), " +
                             "base_price = VALUES(base_price), estimated_minutes = VALUES(estimated_minutes), description = VALUES(description)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, treatment.getCode());
                    ps.setString(2, treatment.getName());
                    ps.setString(3, treatment.getCategory());
                    ps.setDouble(4, treatment.getBasePrice());
                    ps.setInt(5, treatment.getEstimatedMinutes());
                    ps.setString(6, treatment.getDescription());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error saving treatment in MySQL: " + e.getMessage());
        }

        MEMORY_STORE.put(treatment.getCode(), treatment);
        return true;
    }

    private Treatment rsToTreatment(ResultSet rs) throws SQLException {
        Treatment t = new Treatment();
        t.setCode(rs.getString("code"));
        t.setName(rs.getString("name"));
        t.setCategory(rs.getString("category"));
        t.setBasePrice(rs.getDouble("base_price"));
        t.setEstimatedMinutes(rs.getInt("estimated_minutes"));
        t.setDescription(rs.getString("description"));
        return t;
    }
}
