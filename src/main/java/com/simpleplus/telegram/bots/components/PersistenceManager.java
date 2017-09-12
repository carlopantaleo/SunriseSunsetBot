package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.datamodel.SavedChat;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.apache.log4j.Logger;
import org.h2.tools.Server;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistenceManager implements BotBean {
    private static final Logger LOG = Logger.getLogger(PersistenceManager.class);
    private EntityManagerFactory emFactory;
    private PropertiesManager propertiesManager;
    private Server webServer;

    public void init() {
        propertiesManager = (PropertiesManager) BotContext.getDefaultContext().getBean(PropertiesManager.class);

        String botDatabase = propertiesManager.getBotDatabase() != null ?
                propertiesManager.getBotDatabase() : "sunrise-sunset-bot";
        String botUser = propertiesManager.getProperty("bot-db-user") != null ?
                propertiesManager.getProperty("bot-db-user") : "sa";
        String botPassword = propertiesManager.getProperty("bot-db-password") != null ?
                propertiesManager.getProperty("bot-db-user") : "";


        Map<String, String> persistenceMap = new HashMap<>();
        persistenceMap.put("javax.persistence.jdbc.url", "jdbc:h2:./" + botDatabase);
        persistenceMap.put("javax.persistence.jdbc.user", botUser);
        persistenceMap.put("javax.persistence.jdbc.password", botPassword);
        emFactory = Persistence.createEntityManagerFactory("h2", persistenceMap);
        try {
            webServer = Server.createWebServer("-webAllowOthers", "-webPort", "8082").start();
        } catch (SQLException e) {
            LOG.error("Cannot create web server.", e);
        }
    }

    public void shutDown() {
        webServer.shutdown();
    }

    public UserState getUserState(long chatId) {
        EntityManager em = createEntityManager();
        SavedChat savedChat = em.find(SavedChat.class, chatId);
        UserState userState = savedChat != null ? savedChat.getUserState() : null;
        em.close();
        return userState;
    }

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

    public void setUserState(long chatId, UserState userState) {
        EntityManager em = createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        SavedChat savedChat = new SavedChat(chatId, userState);
        em.merge(savedChat);
        em.flush();
        transaction.commit();
        em.close();
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

    protected EntityManager createEntityManager() {
        return emFactory.createEntityManager();
    }
}
