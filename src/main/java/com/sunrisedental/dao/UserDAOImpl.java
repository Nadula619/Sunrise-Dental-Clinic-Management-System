package com.sunrisedental.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.sunrisedental.config.DatabaseConnection;
import com.sunrisedental.model.User;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB Implementation of IUserDAO with fallback in-memory cache.
 */
public class UserDAOImpl implements IUserDAO {
    private static final Logger LOGGER = Logger.getLogger(UserDAOImpl.class.getName());
    private static final String COLLECTION_NAME = "users";

    // In-memory fallback repository
    private static final Map<String, User> MEMORY_STORE = new ConcurrentHashMap<>();

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
    public User findByUsername(String username) {
        if (username == null) return null;
        
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Document doc = coll.find(Filters.eq("username", username.trim())).first();
                if (doc != null) {
                    return docToUser(doc);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error querying user from MongoDB: " + e.getMessage());
            }
        }

        // Memory store lookup
        for (User u : MEMORY_STORE.values()) {
            if (u.getUsername() != null && u.getUsername().equalsIgnoreCase(username.trim())) {
                return u;
            }
        }
        return null;
    }

    @Override
    public User findById(String id) {
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
                    return docToUser(doc);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error querying user by ID: " + e.getMessage());
            }
        }

        return MEMORY_STORE.get(id);
    }

    @Override
    public boolean createUser(User user) {
        if (user == null || user.getUsername() == null) return false;

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                Document doc = userToDoc(user);
                coll.insertOne(doc);
                ObjectId insertedId = doc.getObjectId("_id");
                if (insertedId != null) {
                    user.setId(insertedId.toHexString());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error inserting user to MongoDB: " + e.getMessage());
            }
        }

        if (user.getId() == null) {
            user.setId(String.valueOf(System.currentTimeMillis()));
        }
        MEMORY_STORE.put(user.getId(), user);
        return true;
    }

    @Override
    public List<User> findAllUsers() {
        List<User> list = new ArrayList<>();
        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                for (Document doc : coll.find()) {
                    list.add(docToUser(doc));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error reading all users from MongoDB: " + e.getMessage());
            }
        }

        return new ArrayList<>(MEMORY_STORE.values());
    }

    @Override
    public boolean updateUser(User user) {
        if (user == null || user.getId() == null) return false;

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                if (ObjectId.isValid(user.getId())) {
                    coll.updateOne(Filters.eq("_id", new ObjectId(user.getId())),
                            Updates.combine(
                                    Updates.set("fullName", user.getFullName()),
                                    Updates.set("role", user.getRole()),
                                    Updates.set("email", user.getEmail())
                            ));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error updating user in MongoDB: " + e.getMessage());
            }
        }

        MEMORY_STORE.put(user.getId(), user);
        return true;
    }

    @Override
    public boolean deleteUser(String id) {
        if (id == null) return false;

        MongoCollection<Document> coll = getCollection();
        if (coll != null) {
            try {
                if (ObjectId.isValid(id)) {
                    coll.deleteOne(Filters.eq("_id", new ObjectId(id)));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error deleting user from MongoDB: " + e.getMessage());
            }
        }

        MEMORY_STORE.remove(id);
        return true;
    }

    private User docToUser(Document doc) {
        User u = new User();
        if (doc.get("_id") != null) {
            u.setId(doc.get("_id").toString());
        }
        u.setUsername(doc.getString("username"));
        u.setPasswordHash(doc.getString("passwordHash"));
        u.setFullName(doc.getString("fullName"));
        u.setRole(doc.getString("role"));
        u.setEmail(doc.getString("email"));
        u.setCreatedAt(doc.getDate("createdAt") != null ? doc.getDate("createdAt") : new Date());
        return u;
    }

    private Document userToDoc(User u) {
        Document doc = new Document();
        if (u.getId() != null && ObjectId.isValid(u.getId())) {
            doc.append("_id", new ObjectId(u.getId()));
        }
        doc.append("username", u.getUsername())
           .append("passwordHash", u.getPasswordHash())
           .append("fullName", u.getFullName())
           .append("role", u.getRole())
           .append("email", u.getEmail())
           .append("createdAt", u.getCreatedAt() != null ? u.getCreatedAt() : new Date());
        return doc;
    }
}
