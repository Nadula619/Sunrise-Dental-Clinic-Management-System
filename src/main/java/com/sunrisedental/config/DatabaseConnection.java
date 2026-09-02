package com.sunrisedental.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton Pattern: Manages a single, thread-safe MongoDB client connection instance
 * throughout the application lifecycle.
 */
public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    private static volatile DatabaseConnection instance;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private boolean connected = false;

    private DatabaseConnection() {
        initConnection();
    }

    private void initConnection() {
        try {
            String uri = AppConfig.getMongoUri();
            String dbName = AppConfig.getDatabaseName();

            LOGGER.info("Connecting to MongoDB at: " + uri);

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .applyToSocketSettings(builder -> 
                            builder.connectTimeout(3000, TimeUnit.MILLISECONDS)
                                   .readTimeout(3000, TimeUnit.MILLISECONDS))
                    .applyToClusterSettings(builder ->
                            builder.serverSelectionTimeout(3000, TimeUnit.MILLISECONDS))
                    .build();

            this.mongoClient = MongoClients.create(settings);
            this.database = mongoClient.getDatabase(dbName);
            
            // Quick ping / verification
            this.database.runCommand(new org.bson.Document("ping", 1));
            this.connected = true;
            LOGGER.info("Successfully connected to MongoDB database: " + dbName);
        } catch (Exception e) {
            this.connected = false;
            LOGGER.log(Level.WARNING, "MongoDB connection failed or server is offline (" + e.getMessage() + "). In-memory fallback will be active if needed.", e);
        }
    }

    /**
     * Thread-safe Singleton access method with Double-Checked Locking.
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public boolean isConnected() {
        return connected;
    }

    public void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                connected = false;
                LOGGER.info("MongoDB connection closed.");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing MongoDB connection", e);
            }
        }
    }
}
