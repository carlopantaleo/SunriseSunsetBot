package com.simpleplus.telegram.bots.components;


import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.simpleplus.telegram.bots.datamodel.Step.*;

public class SunriseSunsetBot extends TelegramLongPollingBot implements BotBean {
    private static final Logger LOG = LogManager.getLogger(SunriseSunsetBot.class);
    private Notifier notifier;
    private BotSession botSession;
    private PersistenceManager persistenceManager;
    private MessageHandler messageHandler;
    private CommandHandler commandHandler;
    private PropertiesManager propertiesManager;
    private Map<Long, Integer> exceptionCountMap = new HashMap<>();

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

        initProxy();
    }

    private void initProxy() {
        String proxyHost = propertiesManager.getProperty("proxy-host");
        boolean useSocks = propertiesManager.getProperty("use-socks") != null;
        final int timeout = 75 * 1000;
        if (proxyHost != null) {
            if (useSocks) {
                System.getProperties().put("socksProxyHost", proxyHost);
                System.getProperties()
                        .put("socksProxyPort", propertiesManager.getPropertyOrDefault("proxy-port", "80"));
            } else {
                RequestConfig requestConfig = RequestConfig.custom().setProxy(new HttpHost(proxyHost,
                        Integer.parseInt(propertiesManager.getPropertyOrDefault("proxy-port", "80"))))
                        .setSocketTimeout(timeout).setConnectionRequestTimeout(timeout).setConnectTimeout(timeout)
                        .build();
                this.getOptions().setRequestConfig(requestConfig);
            }
        }
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
        String message = String.format("ChatId %d: Incoming message: ", getChatId(update));

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
        SendMessage messageToSend = new SendMessage().setChatId(chatId).setText(message);
        reply(messageToSend);
    }

    public void reply(SendMessage messageToSend) {
        reply(messageToSend, Long.parseLong(messageToSend.getChatId()), messageToSend.getText());
    }

    public void reply(EditMessageText messageToSend) {
        reply(messageToSend, Long.parseLong(messageToSend.getChatId()), messageToSend.getText());
    }

    private void reply(BotApiMethod messageToSend, long chatId, String text) {
        try {
            execute(messageToSend);
            LOG.info("ChatId {}: Outgoing message: {}", chatId, text);
        } catch (TelegramApiRequestException e) {
            LOG.warn("ChatId {}: TelegramApiRequestException during reply. " + "Error was {} - {}", chatId,
                    e.getErrorCode(), e.getApiResponse());

            if (reachedMaxExceptionCount(chatId)) {
                persistenceManager.setStep(chatId, Step.EXPIRED);
                LOG.warn("ChatId {}: Reached maximum number of exceptions. Chat flagged as expired.", chatId);
            } else {
                incrementExceptionCount(chatId);
            }
        } catch (TelegramApiException e) {
            LOG.warn("ChatId " + chatId + ": TelegramApiException during reply. Chat NOT flagged as expired.", e);
        }
    }

    private void incrementExceptionCount(long chatId) {
        Integer exceptionCount = exceptionCountMap.get(chatId);
        if (exceptionCount == null) {
            exceptionCount = 0;
        }
        exceptionCount++;

        exceptionCountMap.put(chatId, exceptionCount);
        LOG.info("ChatId {}: Incremented exception count to {}.", chatId, exceptionCount);
    }

    private boolean reachedMaxExceptionCount(long chatId) {
        Integer maxExceptions = Integer.valueOf(propertiesManager.getPropertyOrDefault("max-exceptions-for-chat", "3"));
        Integer exceptionCount = exceptionCountMap.get(chatId);

        return exceptionCount != null && exceptionCount >= maxExceptions;
    }

    public void replyAndLogError(long chatId, String message, Throwable e) {
        String errorUUID = UUID.randomUUID().toString();
        LOG.error(message + " (" + errorUUID + ")", e);
        reply(chatId, "Oops, something went wrong. But don't worry, we are already working on it!\n" + "Problem ID: " +
                      errorUUID);
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