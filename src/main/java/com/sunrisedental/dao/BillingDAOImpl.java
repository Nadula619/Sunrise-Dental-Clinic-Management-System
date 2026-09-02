package com.sunrisedental.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.sunrisedental.config.DatabaseConnection;
import com.sunrisedental.model.Bill;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB implementation of IBillingDAO.
 */
public class BillingDAOImpl implements IBillingDAO {
    private static final Logger LOGGER = Logger.getLogger(BillingDAOImpl.class.getName());
    private static final String COLLECTION_NAME = "billing";

    private static final Map<String, Bill> MEMORY_STORE = new ConcurrentHashMap<>();
    private static final AtomicLong BILL_SEQUENCE = new AtomicLong(100);

    private MongoCollection<Document> getCollection() {
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            if (dbConn.isConnected()) {
                MongoDatabase db = dbConn.getDatabase();
                if (db != null) {
                    return db.getCollection(COLLECTION_NAME);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "MongoDB not accessible, using memory store", e);
        }
        return null;
    }

    @Override
    public synchronized String generateNextBillNumber() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        MongoCollection<Document> coll = getCollection();
        long seq = 1;
        if (coll != null) {
            try {
                long count = coll.countDocuments();
                seq = count + 1;
            } catch (Exception e) {
                seq = BILL_SEQUENCE.incrementAndGet();
            }
        } else {
            seq = MEMORY_STORE.size() + 1;
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
        bill.calculateTotals();

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Document doc = billToDoc(bill);
                coll.insertOne(doc);
                ObjectId id = doc.getObjectId("_id");
                if (id != null) {
                    bill.setId(id.toHexString());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error creating bill in MongoDB: " + e.getMessage());
            }
        }

        if (bill.getId() == null) {
            bill.setId(UUID.randomUUID().toString());
        }
        MEMORY_STORE.put(bill.getBillNumber(), bill);
        return true;
    }

    @Override
    public Bill findByBillNumber(String billNumber) {
        if (billNumber == null || billNumber.trim().isEmpty()) return null;

        String target = billNumber.trim();
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Document doc = coll.find(Filters.eq("billNumber", target)).first();
                if (doc != null) {
                    return docToBill(doc);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error finding bill by number: " + e.getMessage());
            }
        }

        return MEMORY_STORE.get(target);
    }

    @Override
    public Bill findByAppointmentNumber(String appointmentNumber) {
        if (appointmentNumber == null || appointmentNumber.trim().isEmpty()) return null;

        String target = appointmentNumber.trim();
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Document doc = coll.find(Filters.eq("appointmentNumber", target)).first();
                if (doc != null) {
                    return docToBill(doc);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error finding bill by appointment number: " + e.getMessage());
            }
        }

        for (Bill b : MEMORY_STORE.values()) {
            if (target.equalsIgnoreCase(b.getAppointmentNumber())) {
                return b;
            }
        }
        return null;
    }

    @Override
    public List<Bill> findAll() {
        List<Bill> list = new ArrayList<>();
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                for (Document doc : coll.find().sort(Sorts.descending("billedAt"))) {
                    list.add(docToBill(doc));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error finding all bills: " + e.getMessage());
            }
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
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                for (Document doc : coll.find(Filters.eq("patientName", patientName.trim()))) {
                    list.add(docToBill(doc));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error finding bills by patient: " + e.getMessage());
            }
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

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                coll.updateOne(Filters.eq("billNumber", billNumber.trim()),
                        Updates.combine(
                                Updates.set("paymentStatus", status.trim().toUpperCase()),
                                Updates.set("paymentMethod", method != null ? method.trim().toUpperCase() : "CASH")
                        ));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error updating bill status: " + e.getMessage());
            }
        }

        Bill b = MEMORY_STORE.get(billNumber.trim());
        if (b != null) {
            b.setPaymentStatus(status.trim().toUpperCase());
            if (method != null) b.setPaymentMethod(method.trim().toUpperCase());
            return true;
        }
        return coll != null;
    }

    @Override
    public long countBills() {
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                return coll.countDocuments();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error counting bills: " + e.getMessage());
            }
        }
        return MEMORY_STORE.size();
    }

    private Bill docToBill(Document doc) {
        Bill b = new Bill();
        if (doc.get("_id") != null) {
            b.setId(doc.get("_id").toString());
        }
        b.setBillNumber(doc.getString("billNumber"));
        b.setAppointmentNumber(doc.getString("appointmentNumber"));
        b.setPatientName(doc.getString("patientName"));
        b.setDentistName(doc.getString("dentistName"));
        b.setTreatmentType(doc.getString("treatmentType"));
        b.setConsultationFee(doc.getDouble("consultationFee") != null ? doc.getDouble("consultationFee") : 0.0);
        b.setTreatmentFee(doc.getDouble("treatmentFee") != null ? doc.getDouble("treatmentFee") : 0.0);
        b.setExtraCharges(doc.getDouble("extraCharges") != null ? doc.getDouble("extraCharges") : 0.0);
        b.setDiscountRate(doc.getDouble("discountRate") != null ? doc.getDouble("discountRate") : 0.0);
        b.setDiscountAmount(doc.getDouble("discountAmount") != null ? doc.getDouble("discountAmount") : 0.0);
        b.setDiscountType(doc.getString("discountType") != null ? doc.getString("discountType") : "NONE");
        b.setTotalAmount(doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0.0);
        b.setPaymentStatus(doc.getString("paymentStatus") != null ? doc.getString("paymentStatus") : "PAID");
        b.setPaymentMethod(doc.getString("paymentMethod") != null ? doc.getString("paymentMethod") : "CASH");
        b.setBilledAt(doc.getDate("billedAt") != null ? doc.getDate("billedAt") : new Date());
        b.setNotes(doc.getString("notes"));
        return b;
    }

    private Document billToDoc(Bill b) {
        Document doc = new Document();
        if (b.getId() != null && ObjectId.isValid(b.getId())) {
            doc.append("_id", new ObjectId(b.getId()));
        }
        doc.append("billNumber", b.getBillNumber())
           .append("appointmentNumber", b.getAppointmentNumber())
           .append("patientName", b.getPatientName())
           .append("dentistName", b.getDentistName())
           .append("treatmentType", b.getTreatmentType())
           .append("consultationFee", b.getConsultationFee())
           .append("treatmentFee", b.getTreatmentFee())
           .append("extraCharges", b.getExtraCharges())
           .append("discountRate", b.getDiscountRate())
           .append("discountAmount", b.getDiscountAmount())
           .append("discountType", b.getDiscountType())
           .append("totalAmount", b.getTotalAmount())
           .append("paymentStatus", b.getPaymentStatus())
           .append("paymentMethod", b.getPaymentMethod())
           .append("billedAt", b.getBilledAt() != null ? b.getBilledAt() : new Date())
           .append("notes", b.getNotes());
        return doc;
    }
}
