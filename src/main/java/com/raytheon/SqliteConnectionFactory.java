package com.raytheon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqliteConnectionFactory {
    public static Connection getConnection(String path) throws SQLException, ClassNotFoundException {
        Connection c;
        Class.forName("org.sqlite.JDBC");
        c = DriverManager.getConnection("jdbc:sqlite:" + path);
        return c;
    }
}
