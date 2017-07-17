package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class PersistenceManager implements BotBean {
    private static final Logger LOG = Logger.getLogger(PersistenceManager.class);

    private PreparedStatement getUserStateStatement;
    private PreparedStatement insertUserStateStatement;
    private PreparedStatement updateUserStateStatement;
    private PreparedStatement getAllUserStatesStatement;
    private Connection connection;
    private String database;

    public PersistenceManager(String database) {
        this.database = database;
    }

    public void init() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + database);
            doStartup();
            prepareStatements();
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

    public UserState getUserState(long chatId) {
        ResultSet rs;
        try {
            getUserStateStatement.setLong(1, chatId);
            rs = getUserStateStatement.executeQuery();

            UserState userState = new UserState();
            if (rs.next()) {
                userState.setCoordinates(new Coordinates(
                        rs.getFloat("latitude"),
                        rs.getFloat("longitude")));
                userState.setStep(Step.valueOf(rs.getString("step")));
                return userState;
            }
        } catch (SQLException | IllegalArgumentException e) {
            LOG.error("Exception while getting user state.", e);
        }

        return null;
    }

    public Map<Long, UserState> getUserStatesMap() {
        Map<Long, UserState> result = new HashMap<>();
        ResultSet rs;
        try {
            rs = getAllUserStatesStatement.executeQuery();

            while (rs.next()) {
                UserState userState = new UserState();
                userState.setCoordinates(new Coordinates(
                        rs.getFloat("latitude"),
                        rs.getFloat("longitude")));
                userState.setStep(Step.valueOf(rs.getString("step")));
                result.put(rs.getLong("chatid"), userState);
            }
        } catch (SQLException | IllegalArgumentException e) {
            LOG.error("Exception while fetching user states.", e);
        }

        return result;
    }

    public void setUserState(long chatId, UserState userState) {
        try {
            getUserStateStatement.setLong(1, chatId);
        } catch (SQLException e) {
            LOG.error("Unable to set chatId.", e);
        }

        try (ResultSet rs = getUserStateStatement.executeQuery()) {

            if (rs.next()) {
                updateUserStateStatement.setFloat(1, userState.getCoordinates().getLatitude());
                updateUserStateStatement.setFloat(2, userState.getCoordinates().getLongitude());
                updateUserStateStatement.setString(3, userState.getStep().toString());
                updateUserStateStatement.setLong(4, chatId);
                updateUserStateStatement.executeUpdate();
            } else {
                insertUserStateStatement.setLong(1, chatId);
                insertUserStateStatement.setFloat(2, userState.getCoordinates().getLatitude());
                insertUserStateStatement.setFloat(3, userState.getCoordinates().getLongitude());
                insertUserStateStatement.setString(4, userState.getStep().toString());
                insertUserStateStatement.execute();
            }
        } catch (SQLException e) {
            LOG.error("User state not set!.", e);
        }
    }

    private void doStartup() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM (SELECT name FROM sqlite_master " +
                             "WHERE type IN ('table','view') AND name = 'user_state')")
        ) {
            if (rs.next() && rs.getInt(1) != 1) {
                createTables();
            }

            // Vacuum SQLite
            statement.execute("VACUUM");
        } catch (SQLException e) {
            LOG.error("SQLException during init.", e);
        }
    }

    private void prepareStatements() throws SQLException {
        getUserStateStatement = connection.prepareStatement(
                "SELECT chatid, latitude, longitude, step " +
                        "FROM user_state " +
                        "WHERE chatid = ?");

        getAllUserStatesStatement = connection.prepareStatement(
                "SELECT chatid, latitude, longitude, step " +
                        "FROM user_state ");

        insertUserStateStatement = connection.prepareStatement(
                "INSERT INTO user_state (chatid, latitude, longitude, step) " +
                        "VALUES (?, ?, ?, ?)");

        updateUserStateStatement = connection.prepareStatement(
                "UPDATE user_state SET " +
                        "latitude = ?, " +
                        "longitude = ?, " +
                        "step = ? " +
                        "WHERE chatid = ?");
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE user_state (" +
                            "chatid PRIMARY KEY," +
                            "latitude," +
                            "longitude," +
                            "step NOT NULL)");
        }
    }
}
