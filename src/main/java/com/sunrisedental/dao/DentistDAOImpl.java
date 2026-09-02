package com.sunrisedental.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.sunrisedental.config.DatabaseConnection;
import com.sunrisedental.model.Dentist;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB implementation of IDentistDAO with default dentist profiles.
 */
public class DentistDAOImpl implements IDentistDAO {
    private static final Logger LOGGER = Logger.getLogger(DentistDAOImpl.class.getName());
    private static final String COLLECTION_NAME = "dentists";

    private static final Map<String, Dentist> MEMORY_STORE = new ConcurrentHashMap<>();

    public DentistDAOImpl() {
        initDefaults();
    }

    private void initDefaults() {
        if (MEMORY_STORE.isEmpty()) {
            addDefault(new Dentist("Dr. Sarah Perera", "Consultant Dental Surgeon", "0771234501", "sarah.perera@sunrisedental.lk", 2500.00, Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")));
            addDefault(new Dentist("Dr. Roshan Fernando", "Orthodontist & Implantologist", "0771234502", "roshan.f@sunrisedental.lk", 3000.00, Arrays.asList("Monday", "Wednesday", "Saturday")));
            addDefault(new Dentist("Dr. Anusha Jayawardena", "Endodontist (Root Canal Specialist)", "0771234503", "anusha.j@sunrisedental.lk", 2800.00, Arrays.asList("Tuesday", "Thursday", "Sunday")));
            addDefault(new Dentist("Dr. Dinesh Wickramasinghe", "Pediatric & Cosmetic Dentist", "0771234504", "dinesh.w@sunrisedental.lk", 2500.00, Arrays.asList("Friday", "Saturday", "Sunday")));
        }
    }

    private void addDefault(Dentist d) {
        MEMORY_STORE.put(d.getName().toLowerCase(), d);
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
    public List<Dentist> findAll() {
        List<Dentist> list = new ArrayList<>();
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                for (Document doc : coll.find()) {
                    list.add(docToDentist(doc));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error reading dentists from MongoDB: " + e.getMessage());
            }
        }
        return new ArrayList<>(MEMORY_STORE.values());
    }

    @Override
    public Dentist findByName(String name) {
        if (name == null) return null;

        String target = name.trim().toLowerCase();
        for (Dentist d : findAll()) {
            if (d.getName() != null && d.getName().toLowerCase().equalsIgnoreCase(target)) {
                return d;
            }
        }
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

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Document doc = dentistToDoc(dentist);
                coll.replaceOne(Filters.eq("name", dentist.getName()), doc, new ReplaceOptions().upsert(true));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error saving dentist in MongoDB: " + e.getMessage());
            }
        }

        MEMORY_STORE.put(dentist.getName().toLowerCase(), dentist);
        return true;
    }

    private Dentist docToDentist(Document doc) {
        Dentist d = new Dentist();
        d.setName(doc.getString("name"));
        d.setSpecialization(doc.getString("specialization"));
        d.setContactNumber(doc.getString("contactNumber"));
        d.setEmail(doc.getString("email"));
        d.setConsultationFee(doc.getDouble("consultationFee") != null ? doc.getDouble("consultationFee") : 2500.00);
        List<String> days = doc.getList("availableDays", String.class);
        d.setAvailableDays(days != null ? days : new ArrayList<>());
        return d;
    }

    private Document dentistToDoc(Dentist d) {
        Document doc = new Document();
        doc.append("name", d.getName())
           .append("specialization", d.getSpecialization())
           .append("contactNumber", d.getContactNumber())
           .append("email", d.getEmail())
           .append("consultationFee", d.getConsultationFee())
           .append("availableDays", d.getAvailableDays());
        return doc;
    }
}
