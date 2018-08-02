package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.services.NamedLocationToCoordinatesService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.simpleplus.telegram.bots.components.SunriseSunsetBot.getChatId;
import static com.simpleplus.telegram.bots.datamodel.Step.EXPIRED;
import static com.simpleplus.telegram.bots.datamodel.Step.RUNNING;

public class MessageHandler implements BotBean {
    private static final Logger LOG = LogManager.getLogger(MessageHandler.class);
    private static final Coordinates DEFAULT_COORDINATE = new Coordinates();

    private SunriseSunsetBot bot;
    private PersistenceManager persistenceManager;
    private Notifier notifier;
    private BotScheduler scheduler;
    private AdminCommandHandler adminCommandHandler;
    private NamedLocationToCoordinatesService locationToCoordinatesService;

    @Override
    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        notifier = (Notifier) BotContext.getDefaultContext().getBean(Notifier.class);
        scheduler = (BotScheduler) BotContext.getDefaultContext().getBean(BotScheduler.class);
        adminCommandHandler = (AdminCommandHandler) BotContext.getDefaultContext().getBean(AdminCommandHandler.class);
        locationToCoordinatesService = (NamedLocationToCoordinatesService) BotContext.getDefaultContext()
                .getBean(NamedLocationToCoordinatesService.class);
    }

    public void handleMessage(Update update) {
        final long chatId = getChatId(update);

        // If chat is new, it has to be initialized
        if (isChatNew(chatId)) {
            handleNewChat(chatId);
            return;
        }

        // If it's a location message, handle it even though the state wasn't TO_ENTER_LOCATION
        if (update.getMessage().hasLocation()) {
            handleToEnterLocation(update);
            return;
        }

        // Otherwise go on with steps
        switch (persistenceManager.getUserState(chatId).getStep()) {
            case TO_REENTER_LOCATION: {
                handleStartRestartChat(chatId, false);
            }
            break;

            case TO_ENTER_LOCATION: {
                handleToEnterLocation(update);
            }
            break;

            case TO_ENTER_SUPPORT_MESSAGE: {
                sendToSupport(chatId, update.getMessage().getText());
                persistenceManager.setNextStep(chatId);
            }
            break;
        }
    }

    public void handleToEnterLocation(Update update) {
        long chatId = getChatId(update);
        Coordinates location = null;

        if (update.getMessage().hasLocation()) {
            location = new Coordinates(update.getMessage().getLocation().getLatitude(),
                    update.getMessage().getLocation().getLongitude());
        } else if (update.getMessage().hasText()) {
            location = parseLocation(update.getMessage().getText());
        }

        if (location != null) {
            setLocation(chatId, location);
            try {
                scheduler.cancelAllScheduledMessages(chatId);
                notifier.tryToInstallNotifiers(chatId, 5);
                persistenceManager.setStep(chatId, RUNNING);
                bot.reply(chatId, "Your location has been saved. " +
                        "You will be notified at sunset and sunrise.");
            } catch (ServiceException e) {
                bot.replyAndLogError(chatId, "ServiceException during onUpdateReceived.", e);
            }
        } else {
            bot.reply(chatId, "You aren't sending me a valid location. Please try again!");
        }
    }

    public void sendToSupport(long chatId, String message) {
        adminCommandHandler.broadcastToAdmins(String.format("Support request from chatId %d. Message: %s",
                chatId,
                message));
        bot.reply(chatId, "Message to support sent. We will get in touch with you shortly.");
    }

    private @Nullable
    Coordinates parseLocation(String text) {
        Coordinates coordinates = parseGpsLocation(text);

        if (coordinates == null) {
            coordinates = parseNamedLocation(text);
        }

        return coordinates;
    }

    private Coordinates parseGpsLocation(String text) {
        Pattern pattern = Pattern.compile("['\"]?(-?[0-9]*[.,][-0-9]*)[ .,;a-zA-Z]*(-?[0-9]*[.,][-0-9]*)['\"]?");
        Matcher matcher = pattern.matcher(text);

        Float latitude = null;
        Float longitude = null;

        try {
            if (matcher.find()) {
                latitude = Float.parseFloat(matcher.group(1).replace(',', '.'));
                longitude = Float.parseFloat(matcher.group(2).replace(',', '.'));
            }
        } catch (Exception e) {
            // Catching any type of exception leads in invalid parsing.
            LOG.error("Exception while parsing location.", e);
            return null;
        }

        if (latitude == null) { // If latitude is null, so is longitude: checking it is useless.
            return null;
        }

        return new Coordinates(latitude, longitude);
    }

    private Coordinates parseNamedLocation(String text) {
        try {
            return locationToCoordinatesService.findCoordinates(text);
        } catch (ServiceException e) {
            LOG.error("Could not parse named location: Service Exception.");
            return null;
        }
    }

    private boolean isChatNew(long chatId) {
        UserState userState = persistenceManager.getUserState(chatId);
        return userState == null || userState.getStep() == EXPIRED;
    }

    private void handleNewChat(long chatId) {
        handleStartRestartChat(chatId, true);
    }

    private void handleStartRestartChat(long chatId, boolean isChatNew) {
        String message = (isChatNew ? "Welcome! " : "") + "Please send me your location.\n" +
                "Tip: hit the 'Send Location' button below, or send me your coordinates " +
                "like '15.44286; -5.3362'.";
        SendMessage messageToSend = new SendMessage()
                .setChatId(chatId)
                .setText(message);
        addSendLocationInlineKeyboard(messageToSend);
        bot.reply(messageToSend);

        UserState userState = persistenceManager.getUserState(chatId);
        if (userState == null) { // Chat new
            userState = new UserState();
        }

        userState.setCoordinates(DEFAULT_COORDINATE);
        userState.setStep(Step.TO_ENTER_LOCATION);
        persistenceManager.setUserState(chatId, userState);
    }

    private void addSendLocationInlineKeyboard(SendMessage messageToSend) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup()
                .setOneTimeKeyboard(true)
                .setResizeKeyboard(true);
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton().setText("Send Location").setRequestLocation(true));
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row);
        keyboardMarkup.setKeyboard(rows);
        messageToSend.setReplyMarkup(keyboardMarkup);
    }

    private void setLocation(long chatId, Coordinates location) {
        UserState userState = persistenceManager.getUserState(chatId);
        userState.setCoordinates(location);
        persistenceManager.setUserState(chatId, userState);
    }
}
