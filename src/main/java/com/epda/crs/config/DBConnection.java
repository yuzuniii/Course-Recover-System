package com.epda.crs.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/crs_system?useSSL=false&serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "admin";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            ensureSchemaCompatibility();
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            throw new ExceptionInInitializerError("Database schema check failed: " + e.getMessage());
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    private static void ensureSchemaCompatibility() throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            DatabaseMetaData metaData = connection.getMetaData();
            ensureColumn(connection, metaData, "users", "last_login",
                    "ALTER TABLE users ADD COLUMN last_login TIMESTAMP NULL DEFAULT NULL AFTER status");
            ensureColumn(connection, metaData, "students", "email",
                    "ALTER TABLE students ADD COLUMN email VARCHAR(100) NULL AFTER programme");
            ensureColumn(connection, metaData, "courses", "instructor",
                    "ALTER TABLE courses ADD COLUMN instructor VARCHAR(100) NULL AFTER credit_hours");
            ensureUniqueIndex(connection, metaData, "courses", "course_code", "uk_courses_course_code");
        }
    }

    private static void ensureColumn(Connection connection, DatabaseMetaData metaData, String tableName,
            String columnName, String alterSql) throws SQLException {
        if (hasColumn(metaData, tableName, columnName)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(alterSql);
        }
    }

    private static void ensureUniqueIndex(Connection connection, DatabaseMetaData metaData, String tableName,
            String columnName, String indexName) throws SQLException {
        try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName, true, false)) {
            while (indexes.next()) {
                String existingIndex = indexes.getString("INDEX_NAME");
                String existingColumn = indexes.getString("COLUMN_NAME");
                if (indexName.equalsIgnoreCase(existingIndex) || columnName.equalsIgnoreCase(existingColumn)) {
                    return;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD CONSTRAINT " + indexName
                    + " UNIQUE (" + columnName + ")");
        }
    }

    private static boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName)
            throws SQLException {
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }
}
