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
    private static final String DEFAULT_DATABASE = "sunrise-sunset-bot.db";

    private PreparedStatement getUserStateStatement;
    private PreparedStatement insertUserStateStatement;
    private PreparedStatement updateUserStateStatement;
    private PreparedStatement getAllUserStatesStatement;
    private Connection connection;
    private String database;
    private PropertiesManager propertiesManager;

    public void init() {
        propertiesManager = (PropertiesManager) BotContext.getDefaultContext().getBean(PropertiesManager.class);

        database = propertiesManager.getBotDatabase();
        if (database == null) {
            database = DEFAULT_DATABASE;
        }

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + database);
            doStartup();
            prepareStatements();
        } catch (SQLException e) {
            LOG.fatal("SQLException during construction.", e);
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
                userState.setAdmin(rs.getBoolean("isadmin"));
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
                userState.setAdmin(rs.getBoolean("isadmin"));
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
                updateUserStateStatement.setBoolean(4, userState.isAdmin());
                updateUserStateStatement.setLong(5, chatId);
                updateUserStateStatement.executeUpdate();
            } else {
                insertUserStateStatement.setLong(1, chatId);
                insertUserStateStatement.setFloat(2, userState.getCoordinates().getLatitude());
                insertUserStateStatement.setFloat(3, userState.getCoordinates().getLongitude());
                insertUserStateStatement.setString(4, userState.getStep().toString());
                insertUserStateStatement.setBoolean(5, userState.isAdmin());
                insertUserStateStatement.execute();
            }
        } catch (SQLException e) {
            LOG.error("User state not set!.", e);
        }
    }

    public void setNextStep(long chatId) {
        UserState userState = getUserState(chatId);

        switch (userState.getStep()) {
            case NEW_CHAT:
                userState.setStep(Step.TO_ENTER_LOCATION);
                break;
            case TO_ENTER_LOCATION:
            case TO_ENTER_SUPPORT_MESSAGE:
            case STOPPED:
                userState.setStep(Step.RUNNING);
                break;
            case RUNNING:
                userState.setStep(Step.STOPPED);
                break;
        }

        setUserState(chatId, userState);
    }

    public void setStep(long chatId, Step step) {
        UserState userState = getUserState(chatId);
        userState.setStep(step);
        setUserState(chatId, userState);
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
                "SELECT chatid, latitude, longitude, step, isadmin " +
                        "FROM user_state " +
                        "WHERE chatid = ?");

        getAllUserStatesStatement = connection.prepareStatement(
                "SELECT chatid, latitude, longitude, step, isadmin " +
                        "FROM user_state ");

        insertUserStateStatement = connection.prepareStatement(
                "INSERT INTO user_state (chatid, latitude, longitude, step, isadmin) " +
                        "VALUES (?, ?, ?, ?, ?)");

        updateUserStateStatement = connection.prepareStatement(
                "UPDATE user_state SET " +
                        "latitude = ?, " +
                        "longitude = ?, " +
                        "step = ?, " +
                        "isadmin = ? " +
                        "WHERE chatid = ?");
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE user_state (" +
                            "chatid PRIMARY KEY," +
                            "latitude," +
                            "longitude," +
                            "step NOT NULL," +
                            "isadmin BOOLEAN NOT NULL DEFAULT ('false'))");
        }
    }
}
