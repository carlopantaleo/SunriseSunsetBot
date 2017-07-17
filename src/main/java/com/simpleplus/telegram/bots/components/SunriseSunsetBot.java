package com.simpleplus.telegram.bots.components;


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
import org.telegram.telegrambots.generics.BotSession;

import java.util.UUID;

/* Lista della spesa
    - Ci sono ancora eccezioni da gestire
    - Implementare un po' di comandi
    - Introdurre logiche di amministrazione
 */
public class SunriseSunsetBot extends TelegramLongPollingBot implements BotBean {
    private static final Coordinates DEFAULT_COORDINATE = new Coordinates();
    private static final Logger LOG = Logger.getLogger(SunriseSunsetBot.class);
    private Notifier notifier;
    private BotSession botSession;
    private PersistenceManager persistenceManager;

    public void init() {
        notifier = (Notifier) BotContext.getDefaultContext().getBean("Notifier");
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean("PersistenceManager");
    }

    public void start() {
        LOG.info("Starting up...");
        notifier.installAllNotifiers();
        notifier.scheduleDailyAllNotifiersInstaller();

        // Add shutdown hook to gracefully close db connection
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down...");
            persistenceManager.shutdown();
            botSession.stop();
        }));
    }

    private synchronized void restart() {
        botSession.stop();
        botSession.start();
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
        switch (persistenceManager.getUserState(chatId).getStep()) {
            case TO_REENTER_LOCATION: {
                gestToEnterCoordinates(chatId, false);
            }
            break;

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
        UserState userState = persistenceManager.getUserState(chatId);
        userState.setCoordinates(new Coordinates(location.getLatitude(), location.getLongitude()));
        persistenceManager.setUserState(chatId, userState);
    }

    private void setNextStep(long chatId) {
        UserState userState = persistenceManager.getUserState(chatId);

        switch (userState.getStep()) {
            case NEW_CHAT:
                userState.setStep(Step.TO_ENTER_LOCATION);
                break;
            case TO_ENTER_LOCATION:
                userState.setStep(Step.RUNNING);
                break;
        }

        persistenceManager.setUserState(chatId, userState);
    }

    private void setStep(long chatId, Step step) {
        UserState userState = persistenceManager.getUserState(chatId);
        userState.setStep(step);
    }

    private boolean isChatNew(long chatId) {
        return persistenceManager.getUserState(chatId) != null;
    }

    private void gestNewChat(long chatId) {
        gestToEnterCoordinates(chatId, true);
    }

    private void gestToEnterCoordinates(long chatId, boolean isChatNew) {
        String message = (isChatNew ? "Welcome! " : "") + "Please send me your location.";
        reply(chatId, message);

        UserState userState = new UserState(DEFAULT_COORDINATE, Step.TO_ENTER_LOCATION);
        persistenceManager.setUserState(chatId, userState);
    }

    public void reply(long chatId, String message) {
        SendMessage messageToSend = new SendMessage();
        messageToSend.setChatId(chatId);
        messageToSend.setText(message);

        try {
            sendMessage(messageToSend);
            LOG.info("Sent message to chatId[" + Long.toString(chatId) + "].");
        } catch (TelegramApiException e) {
            UserState userState = persistenceManager.getUserState(chatId);
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

    public String getBotUsername() {
        return "SunriseSunset_bot";
    }

    @Override
    public String getBotToken() {
        return "416777734:AAF5r6MdiQcsr1XKtiiUg69AGBp7JBG_IlQ";
    }

    public BotSession getBotSession() {
        return botSession;
    }

    public void setBotSession(BotSession botSession) {
        this.botSession = botSession;
    }
}