
package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:teachersdb.db";

    private static boolean walInitialized = false;

    public static synchronized Connection getConnection()
            throws SQLException {

        Connection conn = DriverManager.getConnection(DB_URL);

        try (Statement stmt = conn.createStatement()) {

            // Wait up to 10 seconds if the database is busy
            stmt.execute("PRAGMA busy_timeout = 10000");

            // Enable foreign key constraints
            stmt.execute("PRAGMA foreign_keys = ON");

            // Enable WAL mode once
            if (!walInitialized) {
                stmt.execute("PRAGMA journal_mode = WAL");
                walInitialized = true;
            }

        } catch (SQLException e) {
            conn.close();
            throw e;
        }

        return conn;
    }
}
