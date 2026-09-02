package com.sunrisedental.config;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration loader for application settings and MongoDB connection details.
 */
public class AppConfig {
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final Properties properties = new Properties();

    static {
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                properties.load(is);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "application.properties not found, using environment variables and defaults", e);
        }
    }

    public static String getProperty(String key, String defaultValue) {
        String envValue = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            return sysProp;
        }
        return properties.getProperty(key, defaultValue);
    }

    public static String getMongoUri() {
        return getProperty("mongodb.uri", "mongodb://localhost:27017");
    }

    public static String getDatabaseName() {
        return getProperty("mongodb.database", "sunrise_dental_db");
    }

    public static int getServerPort() {
        return Integer.parseInt(getProperty("server.port", "8080"));
    }
}
