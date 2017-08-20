package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.objects.Update;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.simpleplus.telegram.bots.datamodel.Step.EXPIRED;

public class MessageHandler implements BotBean {
    private static final Logger LOG = Logger.getLogger(MessageHandler.class);
    private static final Coordinates DEFAULT_COORDINATE = new Coordinates();

    private SunriseSunsetBot bot;
    private PersistenceManager persistenceManager;
    private Notifier notifier;
    private BotScheduler scheduler;
    private AdminCommandHandler adminCommandHandler;

    @Override
    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        notifier = (Notifier) BotContext.getDefaultContext().getBean(Notifier.class);
        scheduler = (BotScheduler) BotContext.getDefaultContext().getBean(BotScheduler.class);
        adminCommandHandler = (AdminCommandHandler) BotContext.getDefaultContext().getBean(AdminCommandHandler.class);
    }

    public void handleMessage(Update update) {
        final long chatId = update.getMessage().getChatId();

        // If chat is new, it has to be initialized
        if (isChatNew(chatId)) {
            gestNewChat(chatId);
            return;
        }

        // Otherwise go on with steps
        switch (persistenceManager.getUserState(chatId).getStep()) {
            case TO_REENTER_LOCATION: {
                gestStartRestartChat(chatId, false);
            }
            break;

            case TO_ENTER_LOCATION: {
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
                        notifier.tryToInstallNotifier(chatId, 5);
                        setNextStep(chatId);
                        bot.reply(chatId, "You will be notified at sunset and sunrise.");
                    } catch (ServiceException e) {
                        bot.replyAndLogError(chatId, "ServiceException during onUpdateReceived.", e);
                    }
                } else {
                    bot.reply(chatId, "You aren't sending me a valid location. Please try again!");
                }
            }
            break;

            case TO_ENTER_SUPPORT_MESSAGE: {
                sendToSupport(chatId, update.getMessage().getText());
                setNextStep(chatId);
            }
            break;
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

    private void setStep(long chatId, Step step) {
        UserState userState = persistenceManager.getUserState(chatId);
        userState.setStep(step);
    }

    private boolean isChatNew(long chatId) {
        UserState userState = persistenceManager.getUserState(chatId);
        return userState == null || userState.getStep() == EXPIRED;
    }

    private void gestNewChat(long chatId) {
        gestStartRestartChat(chatId, true);
    }

    private void gestStartRestartChat(long chatId, boolean isChatNew) {
        String message = (isChatNew ? "Welcome! " : "") + "Please send me your location.\n" +
                "Tip: use the 'send -> location' in your app, or send me a message like '15.44286; -5.3362'.";
        bot.reply(chatId, message);

        UserState userState = new UserState(DEFAULT_COORDINATE, Step.TO_ENTER_LOCATION, false);
        persistenceManager.setUserState(chatId, userState);
    }

    private void setLocation(long chatId, Coordinates location) {
        UserState userState = persistenceManager.getUserState(chatId);
        userState.setCoordinates(location);
        persistenceManager.setUserState(chatId, userState);
    }

    private void setNextStep(long chatId) {
        UserState userState = persistenceManager.getUserState(chatId);

        switch (userState.getStep()) {
            case NEW_CHAT:
                userState.setStep(Step.TO_ENTER_LOCATION);
                break;
            case TO_ENTER_LOCATION:
            case TO_ENTER_SUPPORT_MESSAGE:
                userState.setStep(Step.RUNNING);
                break;
        }

        persistenceManager.setUserState(chatId, userState);
    }
}
