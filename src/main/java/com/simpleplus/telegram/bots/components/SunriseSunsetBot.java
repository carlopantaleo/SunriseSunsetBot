package com.simpleplus.telegram.bots.components;


import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.generics.BotSession;

import java.time.LocalTime;
import java.util.UUID;

import static com.simpleplus.telegram.bots.datamodel.Step.*;

public class SunriseSunsetBot extends TelegramLongPollingBot implements BotBean {
    private static final Logger LOG = Logger.getLogger(SunriseSunsetBot.class);
    private Notifier notifier;
    private BotSession botSession;
    private PersistenceManager persistenceManager;
    private MessageHandler messageHandler;
    private CommandHandler commandHandler;
    private PropertiesManager propertiesManager;
    private LocalTime lastGCTime = LocalTime.now();

    public static long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        } else {
            return 0L;
        }
    }

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
            botSession.stop();
            persistenceManager.shutDown();
        }));
    }

    private synchronized void restart() {
        botSession.stop();
        botSession.start();
    }

    public void onUpdateReceived(Update update) {
        if (!(update.hasCallbackQuery() ||
                (update.hasMessage() && (update.getMessage().hasText() || update.getMessage().hasLocation())))) {
            return;
        }

        logMessage(update);

        // If chat was expired, reactivate it
        UserState userState = persistenceManager.getUserState(getChatId(update));
        if (userState != null) {
            if (userState.getStep() == EXPIRED) {
                persistenceManager.setStep(getChatId(update), RUNNING);
            } else if (userState.getStep() == STOPPED) {
                commandHandler.handleResume(getChatId(update));
                return;
            }
        }

        try {
            if (commandHandler.isCommand(update)) {
                commandHandler.handleCommand(update);
            } else {
                messageHandler.handleMessage(update);
            }
        } catch (Exception e) {
            replyAndLogError(getChatId(update), "Exception while handling update.", e);
        }
    }

    private void logMessage(Update update) {
        String message = String.format("Incoming message from chatId %d: ", getChatId(update));

        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                LOG.info(message + update.getMessage().getText());
            } else if (update.getMessage().hasLocation()) {
                LOG.info(message + "(location message) " + update.getMessage().getLocation().toString());
            } else {
                LOG.info(message + "(other message type) " + update.getMessage().toString());
            }
        } else if (update.hasCallbackQuery()) {
            LOG.info("(callback query) " + update.getCallbackQuery().getData());
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
        reply(messageToSend, chatId, messageToSend.getText());

        // This is a good moment to call the garbage collector (experimental for limited hardware machines).
        gc();
    }

    public void reply(EditMessageText messageToSend) {
        long chatId = Long.parseLong(messageToSend.getChatId());
        reply(messageToSend, chatId, messageToSend.getText());

        // This is a good moment to call the garbage collector (experimental for limited hardware machines).
        gc();
    }

    private void reply(BotApiMethod messageToSend, long chatId, String text) {
        try {
            execute(messageToSend);
            LOG.info("Sent message to chatId[" + Long.toString(chatId) + "]. Message: " + text);
        } catch (TelegramApiException e) {
            persistenceManager.setStep(chatId, Step.EXPIRED);
            LOG.warn("TelegramApiException during reply. Chat flagged as expired.", e);
        }
    }

    private void gc() {
        if (propertiesManager.getProperty("force-gc") != null) {
            // ...but don't abuse of it.
            if (Math.abs(lastGCTime.getMinute() - LocalTime.now().getMinute()) > 30) {
                // Get current size of heap in bytes
                long heapSizeBefore = Runtime.getRuntime().totalMemory();
                long heapFreeBefore = Runtime.getRuntime().freeMemory();
                System.gc();
                lastGCTime = LocalTime.now();
                long heapSizeAfter = Runtime.getRuntime().totalMemory();
                long heapFreeAfter = Runtime.getRuntime().freeMemory();

                LOG.info(String.format("Full GC executed.\n" +
                                "\tHeap before: total = %d, free = %d.\n" +
                                "\tHeap after: total = %d, free = %d",
                        heapSizeBefore, heapFreeBefore, heapSizeAfter, heapFreeAfter));
            }
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