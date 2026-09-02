package com.sunrisedental.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.sunrisedental.config.DatabaseConnection;
import com.sunrisedental.model.Appointment;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * MongoDB implementation of IAppointmentDAO.
 */
public class AppointmentDAOImpl implements IAppointmentDAO {
    private static final Logger LOGGER = Logger.getLogger(AppointmentDAOImpl.class.getName());
    private static final String COLLECTION_NAME = "appointments";

    private static final Map<String, Appointment> MEMORY_STORE = new ConcurrentHashMap<>();
    private static final AtomicLong SEQUENCE_COUNTER = new AtomicLong(100);

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
    public synchronized String generateNextAppointmentNumber() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        MongoCollection<Document> coll = getCollection();
        long seq = 1;
        if (coll != null) {
            try {
                long count = coll.countDocuments();
                seq = count + 1;
            } catch (Exception e) {
                seq = SEQUENCE_COUNTER.incrementAndGet();
            }
        } else {
            seq = MEMORY_STORE.size() + 1;
        }

        String candidate = String.format("APT-%d-%04d", currentYear, seq);
        // Ensure candidate is unique
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

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Document doc = appointmentToDoc(appointment);
                coll.insertOne(doc);
                ObjectId id = doc.getObjectId("_id");
                if (id != null) {
                    appointment.setId(id.toHexString());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error creating appointment in MongoDB: " + e.getMessage());
            }
        }

        if (appointment.getId() == null) {
            appointment.setId(UUID.randomUUID().toString());
        }
        MEMORY_STORE.put(appointment.getAppointmentNumber(), appointment);
        return true;
    }

    @Override
    public Appointment findByAppointmentNumber(String appointmentNumber) {
        if (appointmentNumber == null || appointmentNumber.trim().isEmpty()) return null;

        String target = appointmentNumber.trim();
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Document doc = coll.find(Filters.eq("appointmentNumber", target)).first();
                if (doc != null) {
                    return docToAppointment(doc);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error finding appointment by number: " + e.getMessage());
            }
        }

        return MEMORY_STORE.get(target);
    }

    @Override
    public Appointment findById(String id) {
        if (id == null) return null;

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Document doc;
                if (ObjectId.isValid(id)) {
                    doc = coll.find(Filters.eq("_id", new ObjectId(id))).first();
                } else {
                    doc = coll.find(Filters.eq("id", id)).first();
                }
                if (doc != null) {
                    return docToAppointment(doc);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error finding appointment by id: " + e.getMessage());
            }
        }

        for (Appointment appt : MEMORY_STORE.values()) {
            if (id.equals(appt.getId())) {
                return appt;
            }
        }
        return null;
    }

    @Override
    public List<Appointment> findAll() {
        List<Appointment> list = new ArrayList<>();
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                for (Document doc : coll.find().sort(Sorts.descending("createdAt"))) {
                    list.add(docToAppointment(doc));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error finding all appointments: " + e.getMessage());
            }
        }

        List<Appointment> memList = new ArrayList<>(MEMORY_STORE.values());
        memList.sort((a, b) -> {
            Date d1 = a.getCreatedAt() != null ? a.getCreatedAt() : new Date(0);
            Date d2 = b.getCreatedAt() != null ? b.getCreatedAt() : new Date(0);
            return d2.compareTo(d1);
        });
        return memList;
    }

    @Override
    public List<Appointment> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }

        String q = query.trim();
        List<Appointment> list = new ArrayList<>();
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Pattern regex = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
                Bson filter = Filters.or(
                        Filters.regex("appointmentNumber", regex),
                        Filters.regex("patientName", regex),
                        Filters.regex("contactNumber", regex),
                        Filters.regex("dentistName", regex),
                        Filters.regex("treatmentType", regex)
                );
                for (Document doc : coll.find(filter)) {
                    list.add(docToAppointment(doc));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error searching appointments: " + e.getMessage());
            }
        }

        String lowerQ = q.toLowerCase();
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
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                for (Document doc : coll.find(Filters.eq("appointmentDate", date.trim()))) {
                    list.add(docToAppointment(doc));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error finding appointments by date: " + e.getMessage());
            }
        }

        for (Appointment a : MEMORY_STORE.values()) {
            if (date.trim().equals(a.getAppointmentDate())) {
                list.add(a);
            }
        }
        return list;
    }

    @Override
    public List<Appointment> findByDentist(String dentistName) {
        if (dentistName == null) return new ArrayList<>();

        List<Appointment> list = new ArrayList<>();
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                for (Document doc : coll.find(Filters.eq("dentistName", dentistName.trim()))) {
                    list.add(docToAppointment(doc));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error finding appointments by dentist: " + e.getMessage());
            }
        }

        for (Appointment a : MEMORY_STORE.values()) {
            if (dentistName.trim().equalsIgnoreCase(a.getDentistName())) {
                list.add(a);
            }
        }
        return list;
    }

    @Override
    public boolean updateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getAppointmentNumber() == null) return false;

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                coll.updateOne(Filters.eq("appointmentNumber", appointment.getAppointmentNumber()),
                        Updates.combine(
                                Updates.set("patientName", appointment.getPatientName()),
                                Updates.set("address", appointment.getAddress()),
                                Updates.set("contactNumber", appointment.getContactNumber()),
                                Updates.set("dentistName", appointment.getDentistName()),
                                Updates.set("treatmentType", appointment.getTreatmentType()),
                                Updates.set("appointmentDate", appointment.getAppointmentDate()),
                                Updates.set("appointmentTime", appointment.getAppointmentTime()),
                                Updates.set("status", appointment.getStatus()),
                                Updates.set("notes", appointment.getNotes())
                        ));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error updating appointment: " + e.getMessage());
            }
        }

        MEMORY_STORE.put(appointment.getAppointmentNumber(), appointment);
        return true;
    }

    @Override
    public boolean updateStatus(String appointmentNumber, String status) {
        if (appointmentNumber == null || status == null) return false;

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                coll.updateOne(Filters.eq("appointmentNumber", appointmentNumber.trim()),
                        Updates.set("status", status.trim().toUpperCase()));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error updating appointment status: " + e.getMessage());
            }
        }

        Appointment a = MEMORY_STORE.get(appointmentNumber.trim());
        if (a != null) {
            a.setStatus(status.trim().toUpperCase());
            return true;
        }
        return coll != null;
    }

    @Override
    public boolean isConflict(String dentistName, String date, String time, String excludeAppointmentNumber) {
        if (dentistName == null || date == null || time == null) return false;

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                List<Bson> filters = new ArrayList<>();
                filters.add(Filters.eq("dentistName", dentistName.trim()));
                filters.add(Filters.eq("appointmentDate", date.trim()));
                filters.add(Filters.eq("appointmentTime", time.trim()));
                filters.add(Filters.ne("status", "CANCELLED"));
                if (excludeAppointmentNumber != null) {
                    filters.add(Filters.ne("appointmentNumber", excludeAppointmentNumber.trim()));
                }

                long count = coll.countDocuments(Filters.and(filters));
                if (count > 0) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error checking appointment conflict: " + e.getMessage());
            }
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
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                return coll.countDocuments();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error counting appointments: " + e.getMessage());
            }
        }
        return MEMORY_STORE.size();
    }

    private Appointment docToAppointment(Document doc) {
        Appointment a = new Appointment();
        if (doc.get("_id") != null) {
            a.setId(doc.get("_id").toString());
        }
        a.setAppointmentNumber(doc.getString("appointmentNumber"));
        a.setPatientName(doc.getString("patientName"));
        a.setAddress(doc.getString("address"));
        a.setContactNumber(doc.getString("contactNumber"));
        a.setDentistName(doc.getString("dentistName"));
        a.setTreatmentType(doc.getString("treatmentType"));
        a.setAppointmentDate(doc.getString("appointmentDate"));
        a.setAppointmentTime(doc.getString("appointmentTime"));
        a.setStatus(doc.getString("status") != null ? doc.getString("status") : "SCHEDULED");
        a.setNotes(doc.getString("notes"));
        a.setCreatedAt(doc.getDate("createdAt") != null ? doc.getDate("createdAt") : new Date());
        return a;
    }

    private Document appointmentToDoc(Appointment a) {
        Document doc = new Document();
        if (a.getId() != null && ObjectId.isValid(a.getId())) {
            doc.append("_id", new ObjectId(a.getId()));
        }
        doc.append("appointmentNumber", a.getAppointmentNumber())
           .append("patientName", a.getPatientName())
           .append("address", a.getAddress())
           .append("contactNumber", a.getContactNumber())
           .append("dentistName", a.getDentistName())
           .append("treatmentType", a.getTreatmentType())
           .append("appointmentDate", a.getAppointmentDate())
           .append("appointmentTime", a.getAppointmentTime())
           .append("status", a.getStatus())
           .append("notes", a.getNotes())
           .append("createdAt", a.getCreatedAt() != null ? a.getCreatedAt() : new Date());
        return doc;
    }
}
