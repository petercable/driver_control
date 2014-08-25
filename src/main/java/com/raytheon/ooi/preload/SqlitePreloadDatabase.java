package com.raytheon.ooi.preload;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Concrete implementation of the PreloadDatabase using SQLite.
 * This class will clone the parse_preload repo and execute the parse_preload script
 * to generate a preload database if one does not already exist.
 */
public class SqlitePreloadDatabase extends PreloadDatabase {

    private final static SqlitePreloadDatabase INSTANCE = new SqlitePreloadDatabase();
    private SqlitePreloadDatabase() {}

    public static SqlitePreloadDatabase getInstance() {
        return INSTANCE;
    }

    public void connect() throws Exception {
        if (!Files.exists(Paths.get(model.getConfig().getWorkDir())))
            Files.createDirectory(Paths.get(model.getConfig().getWorkDir()));

        if (!Files.exists(Paths.get(model.getConfig().getDatabaseFile())))
            throw new SQLException("Database does not exist!");

        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + model.getConfig().getDatabaseFile());
    }
}
