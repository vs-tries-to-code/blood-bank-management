package com.bloodbank.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/blood_bank_db";
    private static final String USER = "root";
    private static final String PASSWORD = "duckBathwater4";


    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new SQLException("Driver not found", e);
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}