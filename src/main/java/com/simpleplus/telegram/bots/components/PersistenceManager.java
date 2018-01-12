package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.datamodel.SavedChat;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserAlert;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.Server;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersistenceManager implements BotBean {
    private static final Logger LOG = LogManager.getLogger(PersistenceManager.class);
    private EntityManagerFactory emFactory;
    private PropertiesManager propertiesManager;
    private Server webServer;

    // TODO: use a TCP database (and not an embedded one) for better manageability.

    public void init() {
        propertiesManager = (PropertiesManager) BotContext.getDefaultContext().getBean(PropertiesManager.class);
        createEMFactory();
        startEmbeddedWebServer();
    }

    private void startEmbeddedWebServer() {
        if (propertiesManager.getProperty("embed-web-server") != null) {
            try {
                webServer = Server.createWebServer(
                        "-webAllowOthers",
                        "-webPort",
                        propertiesManager.getPropertyOrDefault("bot-db-port", "8082")
                ).start();
                LOG.info("H2 web server started on port {}.", webServer.getPort());
            } catch (SQLException e) {
                LOG.error("Cannot create web server.", e);
            }
        }
    }

    private void createEMFactory() {
        Map<String, String> persistenceMap = new HashMap<>();
        persistenceMap.put("javax.persistence.jdbc.url", "jdbc:h2:./" +
                propertiesManager.getPropertyOrDefault("bot-database", "db"));
        persistenceMap.put("javax.persistence.jdbc.user",
                propertiesManager.getPropertyOrDefault("bot-db-user", "sa"));
        persistenceMap.put("javax.persistence.jdbc.password",
                propertiesManager.getPropertyOrDefault("bot-db-password", ""));
        emFactory = Persistence.createEntityManagerFactory("h2", persistenceMap);
    }

    public void shutDown() {
        if (webServer != null) {
            webServer.shutdown();
        }
    }

    protected EntityManager createEntityManager() {
        return emFactory.createEntityManager();
    }

    private SavedChat getSavedChat(long chatId) {
        EntityManager em = createEntityManager();
        SavedChat savedChat = em.find(SavedChat.class, chatId);
        em.close();
        return savedChat;
    }

    // The following methods act on the UserState part of a SavedChat

    /**
     * Gets the {@link UserState} associated with a {@code chatId}.
     *
     * @param chatId the {@code chatId} to look for
     * @return the {@link UserState} associated
     */
    public UserState getUserState(long chatId) {
        SavedChat savedChat = getSavedChat(chatId);
        return savedChat != null ? savedChat.getUserState() : null;
    }

    /**
     * Gets a map of all the {@link UserState}s in the database.
     *
     * @return a map of {@link UserState}s
     */
    public Map<Long, UserState> getUserStatesMap() {
        Map<Long, UserState> result = new HashMap<>();

        EntityManager em = createEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SavedChat> q = cb.createQuery(SavedChat.class);
        Root<SavedChat> c = q.from(SavedChat.class);
        q.select(c);
        TypedQuery<SavedChat> query = em.createQuery(q);
        List<SavedChat> results = query.getResultList();

        for (SavedChat savedChat : results) {
            result.put(savedChat.getChatId(), savedChat.getUserState());
        }

        em.close();

        return result;
    }

    /**
     * Sets a {@link UserState} for a certain {@code chatId}.
     *
     * @param chatId    the {@code chatId} for which the {@link UserState} has to be set
     * @param userState the {@link UserState}
     */
    public void setUserState(long chatId, UserState userState) {
        LOG.debug("ChatId {}: Setting UserState: {}", chatId, userState);

        EntityManager em = createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();

        SavedChat savedChat = getSavedChat(chatId);
        if (savedChat == null) {
            savedChat = new SavedChat();
            savedChat.setChatId(chatId);
        }

        savedChat.setUserState(userState);
        em.merge(savedChat);
        em.flush();
        transaction.commit();
        em.close();
    }

    /**
     * Sets the logically following {@link Step} to a certain {@code chatId}.
     * If there's not a logically following {@link Step}, it will leave the current {@link Step} as is.
     *
     * @param chatId the {@code chatId} for which the next step has to be set.
     */
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

    /**
     * Sets a given {@link Step} to a certain {@code chatId}.
     *
     * @param chatId the {@code chatId} to be updated
     * @param step   the {@link Step} to set
     */
    public void setStep(long chatId, Step step) {
        UserState userState = getUserState(chatId);
        userState.setStep(step);
        setUserState(chatId, userState);
    }


    // The following methods act on the UserAlert part of a SavedChat

    /**
     * Gets the {@link UserAlert}s associated with the given {@code chatId}.
     *
     * @param chatId the {@code chatId} to which the {@link UserAlert}s are associated
     * @return the {@link UserAlert}s associated
     */
    public Set<UserAlert> getUserAlerts(long chatId) {
        SavedChat savedChat = getSavedChat(chatId);
        return savedChat.getUserAlerts();
    }

    /**
     * Adds a {@link UserAlert} to the {@link SavedChat} to which it's associated.
     *
     * @param userAlert the {@link UserAlert} to be added
     */
    public boolean addUserAlert(UserAlert userAlert) {
        EntityManager em = createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        SavedChat savedChat = getSavedChat(userAlert.getChatId());
        boolean res = savedChat.addUserAlert(userAlert);
        em.merge(savedChat);
        em.flush();
        transaction.commit();
        em.close();

        return res;
    }

    /**
     * Deletes a {@link UserAlert} from the {@link SavedChat} to which it's associated.
     */
    public void deleteUserAlert(long chatId, long alertId) {
        EntityManager em = createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        SavedChat savedChat = getSavedChat(chatId);
        savedChat.deleteUserAlert(alertId);
        em.merge(savedChat);
        em.flush();
        transaction.commit();
        em.close();
    }

    /**
     * Edits a {@link UserAlert} from the {@link SavedChat} to which it's associated.
     */
    public boolean editUserAlert(UserAlert userAlert) {
        EntityManager em = createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        SavedChat savedChat = getSavedChat(userAlert.getChatId());
        boolean res = savedChat.editUserAlert(userAlert);
        em.merge(savedChat);
        em.flush();
        transaction.commit();
        em.close();

        return res;
    }
}
