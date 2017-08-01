package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.objects.Location;
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

    @Override
    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        notifier = (Notifier) BotContext.getDefaultContext().getBean(Notifier.class);
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
                Location location = null;

                if (update.getMessage().hasLocation()) {
                    location = update.getMessage().getLocation();
                } else if (update.getMessage().hasText()) {
                    location = parseLocation(update.getMessage().getText());
                }

                if (location != null) {
                    setLocation(chatId, location);
                    try {
                        notifier.tryToInstallNotifier(chatId, 5);
                        setNextStep(chatId);
                        bot.reply(chatId, "You will be notified at sunset and sunrise.");
                    } catch (ServiceException e) {
                        bot.replyAndLogError(chatId, "ServiceException during onUpdateReceived.", e);
                    }
                } else {
                    bot.reply(chatId, "You aren't sending me a location. Please try again!");
                }
            }
            break;
        }
    }

    private @Nullable Location parseLocation(String text) {
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

        // Must overload Location, since it was thought to be created only by the bot
        // TODO: maybe use Coordinates class instead?
        Float finalLongitude = longitude;
        Float finalLatitude = latitude;
        return new Location() {
            public Float getLongitude() {
                return finalLongitude;
            }

            public Float getLatitude() {
                return finalLatitude;
            }
        };
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
}
