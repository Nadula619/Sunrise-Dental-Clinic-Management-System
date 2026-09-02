package com.sunrisedental.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton Pattern: Manages thread-safe database connections (MySQL for WAMP/phpMyAdmin
 * and MongoDB NoSQL).
 */
public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    private static volatile DatabaseConnection instance;
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private boolean mongoConnected = false;
    private boolean mysqlConnected = false;
    private String activeSqlUrl;
    private String activeSqlUser;
    private String activeSqlPass;

    private DatabaseConnection() {
        initMysql();
        initMongo();
    }

    private void initMysql() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Try primary config (Port 3306 / root / root)
            String[] urls = {
                AppConfig.getMysqlUrl(),
                "jdbc:mysql://localhost:3306/sunrise_dental_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true",
                "jdbc:mysql://localhost:3307/sunrise_dental_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
            };
            String[] users = {"root", "root"};
            String[] passes = {AppConfig.getMysqlPassword(), "root", ""};

            for (String url : urls) {
                for (String pass : passes) {
                    try (Connection conn = DriverManager.getConnection(url, "root", pass)) {
                        this.mysqlConnected = true;
                        this.activeSqlUrl = url;
                        this.activeSqlUser = "root";
                        this.activeSqlPass = pass;
                        LOGGER.info("Successfully connected to MySQL on WAMP (" + url + ")");
                        initMysqlTables(conn);
                        return;
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            this.mysqlConnected = false;
            LOGGER.log(Level.WARNING, "MySQL connection failed (" + e.getMessage() + "). In-memory cache will be active.", e);
        }
    }

    private void initMysqlTables(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // 1. Users table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id VARCHAR(64) PRIMARY KEY, " +
                    "username VARCHAR(100) UNIQUE NOT NULL, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(150), " +
                    "role VARCHAR(50), " +
                    "email VARCHAR(150), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            // 2. Treatments catalog table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS treatments (" +
                    "code VARCHAR(50) PRIMARY KEY, " +
                    "name VARCHAR(150) NOT NULL, " +
                    "category VARCHAR(100), " +
                    "base_price DECIMAL(10,2) NOT NULL, " +
                    "estimated_minutes INT DEFAULT 30, " +
                    "description TEXT" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            // 3. Dentists table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS dentists (" +
                    "id VARCHAR(64) PRIMARY KEY, " +
                    "name VARCHAR(150) UNIQUE NOT NULL, " +
                    "specialization VARCHAR(150), " +
                    "contact_number VARCHAR(50), " +
                    "email VARCHAR(150), " +
                    "consultation_fee DECIMAL(10,2) DEFAULT 2500.00, " +
                    "available_days VARCHAR(255)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            // 4. Appointments table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS appointments (" +
                    "id VARCHAR(64) PRIMARY KEY, " +
                    "appointment_number VARCHAR(50) UNIQUE NOT NULL, " +
                    "patient_name VARCHAR(150) NOT NULL, " +
                    "address TEXT, " +
                    "contact_number VARCHAR(50) NOT NULL, " +
                    "dentist_name VARCHAR(150) NOT NULL, " +
                    "treatment_type VARCHAR(150) NOT NULL, " +
                    "appointment_date DATE NOT NULL, " +
                    "appointment_time VARCHAR(20) NOT NULL, " +
                    "status VARCHAR(50) DEFAULT 'SCHEDULED', " +
                    "notes TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            // 5. Billing table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS billing (" +
                    "id VARCHAR(64) PRIMARY KEY, " +
                    "bill_number VARCHAR(50) UNIQUE NOT NULL, " +
                    "appointment_number VARCHAR(50), " +
                    "patient_name VARCHAR(150) NOT NULL, " +
                    "dentist_name VARCHAR(150), " +
                    "treatment_type VARCHAR(150), " +
                    "consultation_fee DECIMAL(10,2) DEFAULT 0.00, " +
                    "treatment_fee DECIMAL(10,2) DEFAULT 0.00, " +
                    "extra_charges DECIMAL(10,2) DEFAULT 0.00, " +
                    "discount_rate DECIMAL(5,4) DEFAULT 0.00, " +
                    "discount_amount DECIMAL(10,2) DEFAULT 0.00, " +
                    "discount_type VARCHAR(50) DEFAULT 'STANDARD', " +
                    "total_amount DECIMAL(10,2) NOT NULL, " +
                    "payment_status VARCHAR(50) DEFAULT 'PAID', " +
                    "payment_method VARCHAR(50) DEFAULT 'CASH', " +
                    "billed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "notes TEXT" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            LOGGER.info("MySQL tables verified and created in sunrise_dental_db successfully!");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error creating MySQL tables: " + e.getMessage(), e);
        }
    }

    private void initMongo() {
        try {
            String uri = AppConfig.getMongoUri();
            String dbName = AppConfig.getDatabaseName();

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .applyToSocketSettings(b -> b.connectTimeout(1500, TimeUnit.MILLISECONDS).readTimeout(1500, TimeUnit.MILLISECONDS))
                    .applyToClusterSettings(b -> b.serverSelectionTimeout(1500, TimeUnit.MILLISECONDS))
                    .build();

            this.mongoClient = MongoClients.create(settings);
            this.mongoDatabase = mongoClient.getDatabase(dbName);
            this.mongoDatabase.runCommand(new org.bson.Document("ping", 1));
            this.mongoConnected = true;
        } catch (Exception e) {
            this.mongoConnected = false;
        }
    }

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

    public Connection getSqlConnection() throws SQLException {
        if (activeSqlUrl != null) {
            return DriverManager.getConnection(activeSqlUrl, activeSqlUser, activeSqlPass);
        }
        return DriverManager.getConnection(AppConfig.getMysqlUrl(), AppConfig.getMysqlUser(), AppConfig.getMysqlPassword());
    }

    public boolean isMysqlConnected() {
        return mysqlConnected;
    }

    public MongoDatabase getDatabase() {
        return mongoDatabase;
    }

    public boolean isConnected() {
        return mysqlConnected || mongoConnected;
    }

    public void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
            } catch (Exception e) {}
        }
    }
}
