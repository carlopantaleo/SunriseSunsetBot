package com.simpleplus.telegram.bots;


import com.simpleplus.telegram.bots.components.Notifier;
import com.simpleplus.telegram.bots.components.PersistenceManager;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/* Lista della spesa
    - Ci sono ancora eccezioni da gestire
    - Implementare un po' di comandi
    - Introdurre logiche di amministrazione
 */
public class SunriseSunsetBot extends TelegramLongPollingBot {
    private static final Coordinates DEFAULT_COORDINATE = new Coordinates();
    private static final Logger LOG = Logger.getLogger(SunriseSunsetBot.class);
    private Map<Long, UserState> userStateMap = new HashMap<>();
    private Notifier notifier = new Notifier(this);

    private PersistenceManager persistenceManager = new PersistenceManager("sunrise-sunset-bot.db");
    public SunriseSunsetBot() {
        LOG.info("Starting up...");
        loadState();
        notifier.installAllNotifiers();

        // Add shutdown hook to gracefully close db connection
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down...");
            persistenceManager.shutdown();
        }));
    }

    public Map<Long, UserState> getUserStateMap() {
        return userStateMap;
    }

    public void onUpdateReceived(Update update) {
        if (!(update.hasMessage() && (update.getMessage().hasText() || update.getMessage().hasLocation())))
            return;

        final long chatId = update.getMessage().getChatId();

        // Se la chat è nuova faccio varie inizializzazioni
        if (isChatNew(chatId)) {
            gestNewChat(chatId);
            return;
        }

        // Altrimenti procedo con gli step
        switch (userStateMap.get(chatId).getStep()) {
            case TO_ENTER_LOCATION: {
                if (update.getMessage().hasLocation()) {
                    setLocation(chatId, update.getMessage().getLocation());
                    try {
                        notifier.tryToInstallNotifier(chatId, 5);
                        setNextStep(chatId);
                        reply(chatId, "You will be notified at sunset and sunrise.");
                    } catch (ServiceException e) {
                        replyAndLogError(chatId, "ServiceException during onUpdateReceived.", e);
                    }
                } else {
                    reply(chatId, "You aren't sending me a location. Please try again!");
                }
            }
            break;
        }
    }

    private void setLocation(long chatId, Location location) {
        UserState userState = userStateMap.get(chatId);
        userState.setCoordinates(new Coordinates(location.getLatitude(), location.getLongitude()));
        saveGlobalState();
    }

    private void setNextStep(long chatId) {
        UserState userState = userStateMap.get(chatId);

        switch (userState.getStep()) {
            case NEW_CHAT:
                userState.setStep(Step.TO_ENTER_LOCATION);
                break;
            case TO_ENTER_LOCATION:
                userState.setStep(Step.RUNNING);
                break;
        }
        saveGlobalState();
    }

    private void setStep(long chatId, Step step) {
        UserState userState = userStateMap.get(chatId);
        userState.setStep(step);
    }

    private boolean isChatNew(long chatId) {
        return !userStateMap.containsKey(chatId);
    }

    private void gestNewChat(long chatId) {
        reply(chatId, "Welcome! Please send me your location.");

        userStateMap.put(chatId, new UserState(DEFAULT_COORDINATE, Step.TO_ENTER_LOCATION));
        saveGlobalState();
    }

    private void reply(long chatId, String message) {
        SendMessage messageToSend = new SendMessage();
        messageToSend.setChatId(chatId);
        messageToSend.setText(message);

        try {
            sendMessage(messageToSend);
            LOG.info("Sent message to chatId[" + Long.toString(chatId) + "].");
        } catch (TelegramApiException e) {
            UserState userState = userStateMap.get(chatId);
            userState.setStep(Step.EXPIRED);
            persistenceManager.setUserState(chatId, userState);
            LOG.error("TelegramApiException during reply. Chat flagged as expired.", e);
        }
    }

    public void replyAndLogError(long chatId, String message, Throwable e) {
        String errorUUID = UUID.randomUUID().toString();
        LOG.error(message + " (" + errorUUID + ")", e);
        reply(chatId, "Oops, something went wrong. You may not be notified at sunrise or sunset this time. " +
                "But don't worry, we are already working on it!\n" +
                "Problem ID: " + errorUUID);
    }

    private void saveGlobalState() {
        for (Map.Entry<Long, UserState> entry : userStateMap.entrySet()) {
            persistenceManager.setUserState(entry.getKey(), entry.getValue());
        }
    }

    private void loadState() {
        userStateMap = persistenceManager.getUserStatesMap();
    }

    public String getBotUsername() {
        return "SunriseSunset_bot";
    }

    @Override
    public String getBotToken() {
        return "416777734:AAF5r6MdiQcsr1XKtiiUg69AGBp7JBG_IlQ";
    }
}