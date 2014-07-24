package com.raytheon;

import org.apache.logging.log4j.LogManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PreloadDatabase {
    private Connection connection;
    private static org.apache.logging.log4j.Logger log = LogManager.getLogger();

    public PreloadDatabase(Connection conn) {
        this.connection = conn;
    }

    public void getParameter(String id) {
        log.debug("getParameters: {}", id);
        try (Statement stmt = connection.createStatement()) {
            String sql = String.format("select id from parameterdefs where id='%s';", id);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                log.debug(rs.getRow());
                log.debug(rs.getString(1));
            }
        } catch (SQLException e) {
            log.debug("exception: " + e);
        }
    }
}
