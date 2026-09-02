package com.sunrisedental.dao;

import com.sunrisedental.config.DatabaseConnection;
import com.sunrisedental.model.Bill;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL & MongoDB Implementation of IBillingDAO.
 */
public class BillingDAOImpl implements IBillingDAO {
    private static final Logger LOGGER = Logger.getLogger(BillingDAOImpl.class.getName());
    private static final Map<String, Bill> MEMORY_STORE = new ConcurrentHashMap<>();
    private static final AtomicLong BILL_SEQUENCE = new AtomicLong(100);

    private Connection getConnection() {
        try {
            return DatabaseConnection.getInstance().getSqlConnection();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public synchronized String generateNextBillNumber() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        long seq = 1;
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT COUNT(*) FROM billing";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        seq = rs.getLong(1) + 1;
                    }
                }
            }
        } catch (Exception e) {
            seq = BILL_SEQUENCE.incrementAndGet();
        }

        String candidate = String.format("INV-%d-%04d", currentYear, seq);
        int attempt = 1;
        while (findByBillNumber(candidate) != null) {
            candidate = String.format("INV-%d-%04d", currentYear, seq + attempt);
            attempt++;
        }
        return candidate;
    }

    @Override
    public boolean createBill(Bill bill) {
        if (bill == null) return false;

        if (bill.getBillNumber() == null || bill.getBillNumber().trim().isEmpty()) {
            bill.setBillNumber(generateNextBillNumber());
        }
        if (bill.getId() == null) {
            bill.setId(UUID.randomUUID().toString());
        }
        bill.calculateTotals();

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "INSERT INTO billing (id, bill_number, appointment_number, patient_name, dentist_name, " +
                             "treatment_type, consultation_fee, treatment_fee, extra_charges, discount_rate, discount_amount, " +
                             "discount_type, total_amount, payment_status, payment_method, billed_at, notes) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE payment_status = VALUES(payment_status), payment_method = VALUES(payment_method)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, bill.getId());
                    ps.setString(2, bill.getBillNumber());
                    ps.setString(3, bill.getAppointmentNumber());
                    ps.setString(4, bill.getPatientName());
                    ps.setString(5, bill.getDentistName());
                    ps.setString(6, bill.getTreatmentType());
                    ps.setDouble(7, bill.getConsultationFee());
                    ps.setDouble(8, bill.getTreatmentFee());
                    ps.setDouble(9, bill.getExtraCharges());
                    ps.setDouble(10, bill.getDiscountRate());
                    ps.setDouble(11, bill.getDiscountAmount());
                    ps.setString(12, bill.getDiscountType());
                    ps.setDouble(13, bill.getTotalAmount());
                    ps.setString(14, bill.getPaymentStatus());
                    ps.setString(15, bill.getPaymentMethod());
                    ps.setTimestamp(16, new Timestamp(bill.getBilledAt() != null ? bill.getBilledAt().getTime() : System.currentTimeMillis()));
                    ps.setString(17, bill.getNotes());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error creating bill in MySQL: " + e.getMessage());
        }

        MEMORY_STORE.put(bill.getBillNumber(), bill);
        return true;
    }

    @Override
    public Bill findByBillNumber(String billNumber) {
        if (billNumber == null || billNumber.trim().isEmpty()) return null;

        String target = billNumber.trim();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM billing WHERE bill_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, target);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rsToBill(rs);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding bill by number in MySQL: " + e.getMessage());
        }

        return MEMORY_STORE.get(target);
    }

    @Override
    public Bill findByAppointmentNumber(String appointmentNumber) {
        if (appointmentNumber == null || appointmentNumber.trim().isEmpty()) return null;

        String target = appointmentNumber.trim();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM billing WHERE appointment_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, target);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rsToBill(rs);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding bill by appointment in MySQL: " + e.getMessage());
        }

        for (Bill b : MEMORY_STORE.values()) {
            if (target.equalsIgnoreCase(b.getAppointmentNumber())) return b;
        }
        return null;
    }

    @Override
    public List<Bill> findAll() {
        List<Bill> list = new ArrayList<>();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM billing ORDER BY billed_at DESC";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        list.add(rsToBill(rs));
                    }
                    if (!list.isEmpty()) return list;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding all bills in MySQL: " + e.getMessage());
        }

        List<Bill> memList = new ArrayList<>(MEMORY_STORE.values());
        memList.sort((a, b) -> {
            Date d1 = a.getBilledAt() != null ? a.getBilledAt() : new Date(0);
            Date d2 = b.getBilledAt() != null ? b.getBilledAt() : new Date(0);
            return d2.compareTo(d1);
        });
        return memList;
    }

    @Override
    public List<Bill> findByPatientName(String patientName) {
        if (patientName == null) return new ArrayList<>();

        List<Bill> list = new ArrayList<>();
        String q = "%" + patientName.trim() + "%";
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM billing WHERE patient_name LIKE ? ORDER BY billed_at DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, q);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(rsToBill(rs));
                        }
                        if (!list.isEmpty()) return list;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding bills by patient in MySQL: " + e.getMessage());
        }

        for (Bill b : MEMORY_STORE.values()) {
            if (b.getPatientName() != null && b.getPatientName().toLowerCase().contains(patientName.toLowerCase().trim())) {
                list.add(b);
            }
        }
        return list;
    }

    @Override
    public boolean updatePaymentStatus(String billNumber, String status, String method) {
        if (billNumber == null || status == null) return false;

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "UPDATE billing SET payment_status = ?, payment_method = ? WHERE bill_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, status.trim().toUpperCase());
                    ps.setString(2, method != null ? method.trim().toUpperCase() : "CASH");
                    ps.setString(3, billNumber.trim());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating payment status in MySQL: " + e.getMessage());
        }

        Bill b = MEMORY_STORE.get(billNumber.trim());
        if (b != null) {
            b.setPaymentStatus(status.trim().toUpperCase());
            if (method != null) b.setPaymentMethod(method.trim().toUpperCase());
            return true;
        }
        return true;
    }

    @Override
    public long countBills() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT COUNT(*) FROM billing";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
        } catch (Exception e) {}
        return MEMORY_STORE.size();
    }

    private Bill rsToBill(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setId(rs.getString("id"));
        b.setBillNumber(rs.getString("bill_number"));
        b.setAppointmentNumber(rs.getString("appointment_number"));
        b.setPatientName(rs.getString("patient_name"));
        b.setDentistName(rs.getString("dentist_name"));
        b.setTreatmentType(rs.getString("treatment_type"));
        b.setConsultationFee(rs.getDouble("consultation_fee"));
        b.setTreatmentFee(rs.getDouble("treatment_fee"));
        b.setExtraCharges(rs.getDouble("extra_charges"));
        b.setDiscountRate(rs.getDouble("discount_rate"));
        b.setDiscountAmount(rs.getDouble("discount_amount"));
        b.setDiscountType(rs.getString("discount_type"));
        b.setTotalAmount(rs.getDouble("total_amount"));
        b.setPaymentStatus(rs.getString("payment_status"));
        b.setPaymentMethod(rs.getString("payment_method"));
        Timestamp ts = rs.getTimestamp("billed_at");
        b.setBilledAt(ts != null ? new Date(ts.getTime()) : new Date());
        b.setNotes(rs.getString("notes"));
        return b;
    }
}
