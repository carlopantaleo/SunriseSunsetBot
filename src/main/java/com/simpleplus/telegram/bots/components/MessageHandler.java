package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.Update;

public class MessageHandler implements BotBean {
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
        bot.reply(chatId, message);

        UserState userState = new UserState(DEFAULT_COORDINATE, Step.TO_ENTER_LOCATION);
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
