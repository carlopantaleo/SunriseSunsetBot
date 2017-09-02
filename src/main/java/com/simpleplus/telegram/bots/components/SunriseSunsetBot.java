package com.simpleplus.telegram.bots.components;


import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
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
    private MessageHandler messageHandler;
    private CommandHandler commandHandler;
    private PropertiesManager propertiesManager;

    public void init() {
        notifier = (Notifier) BotContext.getDefaultContext().getBean(Notifier.class);
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        messageHandler = (MessageHandler) BotContext.getDefaultContext().getBean(MessageHandler.class);
        commandHandler = (CommandHandler) BotContext.getDefaultContext().getBean(CommandHandler.class);
        propertiesManager = (PropertiesManager) BotContext.getDefaultContext().getBean(PropertiesManager.class);
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

        logMessage(update);

        try {
            if (commandHandler.isCommand(update)) {
                commandHandler.handleCommand(update);
            } else {
                messageHandler.handleMessage(update);
            }
        } catch (Exception e) {
            replyAndLogError(update.getMessage().getChatId(), "Exception while handling update.", e);
        }
    }

    private void logMessage(Update update) {
        String message = String.format("Incoming message from chatId %d: ", update.getMessage().getChatId());

        if (update.getMessage().hasText()) {
            LOG.info(message + update.getMessage().getText());
        } else if (update.getMessage().hasLocation()) {
            LOG.info(message + "(location message) " + update.getMessage().getLocation().toString());
        } else {
            LOG.info(message + "(other message type) " + update.getMessage().toString());
        }
    }

    public void reply(long chatId, String message) {
        SendMessage messageToSend = new SendMessage()
                .setChatId(chatId)
                .setText(message);
        reply(messageToSend);
    }

    public void reply(SendMessage messageToSend) {
        long chatId = Long.parseLong(messageToSend.getChatId());
        try {
            execute(messageToSend);
            LOG.info("Sent message to chatId[" + Long.toString(chatId) + "]. Message: " + messageToSend.getText());
        } catch (TelegramApiException e) {
            persistenceManager.setStep(chatId, Step.EXPIRED);
            LOG.warn("TelegramApiException during reply. Chat flagged as expired.", e);
        }
    }

    public void replyAndLogError(long chatId, String message, Throwable e) {
        String errorUUID = UUID.randomUUID().toString();
        LOG.error(message + " (" + errorUUID + ")", e);
        reply(chatId, "Oops, something went wrong. But don't worry, we are already working on it!\n" +
                "Problem ID: " + errorUUID);
    }

    public String getBotUsername() {
        return propertiesManager.getBotName();
    }

    @Override
    public String getBotToken() {
        return propertiesManager.getBotToken();
    }

    public BotSession getBotSession() {
        return botSession;
    }

    public void setBotSession(BotSession botSession) {
        this.botSession = botSession;
    }
}