package com.simpleplus.telegram.bots;

import org.apache.log4j.Logger;

import java.sql.*;

public class PersistenceManager {
    private static final Logger LOG = Logger.getLogger(PersistenceManager.class);

    private Connection connection;

    public PersistenceManager(String database) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + database);
            init();
        } catch (SQLException e) {
            LOG.error("SQLException during construction.", e);
            throw new Error("Critical error: unable to init database.");
        }
    }

    public void shutdown() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            LOG.error("SQLException during shutdown.", e);
        }
    }

    private void init() {
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(
                    "SELECT COUNT(*) FROM (SELECT name FROM sqlite_master \n" +
                            "WHERE type IN ('table','view') AND name = 'USER_STATE')\n");
            while (rs.next()) {
                if (rs.getInt(1) != 1) {
                    createTables();
                }
            }
        } catch (SQLException e) {
            LOG.error("SQLException during init.", e);
        }
    }

    private void createTables() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(
                "CREATE TABLE user_state (" +
                        "chatid PRIMARY KEY," +
                        "latitude," +
                        "longitude," +
                        "step)");
    }

}
