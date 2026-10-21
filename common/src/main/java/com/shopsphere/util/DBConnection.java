package com.shopsphere.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC connection factory (DriverManager based). Settings are read from
 * db.properties on the classpath and can be overridden by system properties
 * or environment variables.
 */
public final class DBConnection {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) PROPS.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        // Register the driver explicitly: inside Tomcat, drivers in WEB-INF/lib are not always
        // discovered by DriverManager ("No suitable driver"). Override with -Ddb.driver=... if needed.
        String driver = System.getProperty("db.driver", PROPS.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException ignored) {
            // DriverManager will report a clear error when connecting
        }
    }

    private DBConnection() {}

    private static String setting(String key, String envName) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) v = System.getenv(envName);
        if (v == null || v.isBlank()) v = PROPS.getProperty(key);
        return v;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                setting("db.url", "DB_URL"),
                setting("db.user", "DB_USER"),
                setting("db.password", "DB_PASSWORD"));
    }
}
