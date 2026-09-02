package com.sunrisedental.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.sunrisedental.config.DatabaseConnection;
import com.sunrisedental.model.Treatment;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB implementation of ITreatmentDAO with pre-seeded standard dental procedures.
 */
public class TreatmentDAOImpl implements ITreatmentDAO {
    private static final Logger LOGGER = Logger.getLogger(TreatmentDAOImpl.class.getName());
    private static final String COLLECTION_NAME = "treatments";

    private static final Map<String, Treatment> MEMORY_STORE = new ConcurrentHashMap<>();

    public TreatmentDAOImpl() {
        // Ensure default treatments are available
        initDefaults();
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
        }
    }

    private void addDefault(Treatment t) {
        MEMORY_STORE.put(t.getCode(), t);
    }

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
    public List<Treatment> findAll() {
        List<Treatment> list = new ArrayList<>();
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                for (Document doc : coll.find()) {
                    list.add(docToTreatment(doc));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error reading treatments from MongoDB: " + e.getMessage());
            }
        }
        return new ArrayList<>(MEMORY_STORE.values());
    }

    @Override
    public Treatment findByCode(String code) {
        if (code == null) return null;

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Document doc = coll.find(Filters.eq("code", code.trim())).first();
                if (doc != null) {
                    return docToTreatment(doc);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error reading treatment by code: " + e.getMessage());
            }
        }

        return MEMORY_STORE.get(code.trim());
    }

    @Override
    public Treatment findByName(String name) {
        if (name == null) return null;

        String target = name.trim().toLowerCase();
        for (Treatment t : findAll()) {
            if (t.getName() != null && t.getName().toLowerCase().equals(target)) {
                return t;
            }
        }
        // Partial match
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

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Document doc = treatmentToDoc(treatment);
                coll.replaceOne(Filters.eq("code", treatment.getCode()), doc, new ReplaceOptions().upsert(true));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error saving treatment in MongoDB: " + e.getMessage());
            }
        }

        MEMORY_STORE.put(treatment.getCode(), treatment);
        return true;
    }

    private Treatment docToTreatment(Document doc) {
        Treatment t = new Treatment();
        t.setCode(doc.getString("code"));
        t.setName(doc.getString("name"));
        t.setCategory(doc.getString("category"));
        t.setBasePrice(doc.getDouble("basePrice") != null ? doc.getDouble("basePrice") : 0.0);
        t.setEstimatedMinutes(doc.getInteger("estimatedMinutes") != null ? doc.getInteger("estimatedMinutes") : 30);
        t.setDescription(doc.getString("description"));
        return t;
    }

    private Document treatmentToDoc(Treatment t) {
        Document doc = new Document();
        doc.append("code", t.getCode())
           .append("name", t.getName())
           .append("category", t.getCategory())
           .append("basePrice", t.getBasePrice())
           .append("estimatedMinutes", t.getEstimatedMinutes())
           .append("description", t.getDescription());
        return doc;
    }
}
