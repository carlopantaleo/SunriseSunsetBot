package com.simpleplus.telegram.bots;


import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.helpers.Coordinates;
import com.simpleplus.telegram.bots.helpers.Step;
import com.simpleplus.telegram.bots.helpers.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.helpers.UserState;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import com.simpleplus.telegram.bots.services.impl.SunsetSunriseRemoteAPI;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/* Lista della spesa
    - Gestire bene le eccezioni e avvisare l'utente.
 */
public class SunriseSunsetBot extends TelegramLongPollingBot {

    private static final String SUNRISE_MESSAGE = "The sun is rising!";
    private static final String SUNSET_MESSAGE = "Sunset has begun!";
    private static final Coordinates DEFAULT_COORDINATE = new Coordinates();
    private static final Logger LOG = Logger.getLogger(SunriseSunsetBot.class);

    /**
     * The map where states for each user are stored. Once the bot is constructed, it must contain all states which
     * were previously saved to disk.
     */
    private Map<Long, UserState> userStateMap = new HashMap<>();

    /**
     * The service which provides sunset and sunrise times.
     */
    private SunsetSunriseService sunsetSunriseService = new SunsetSunriseRemoteAPI();

    /**
     * The scheduler which can be used to schedule messages and generic events.
     */
    private BotScheduler scheduler = new BotScheduler(this);
    private PersistenceManager persistenceManager = new PersistenceManager("sunrise-sunset-bot.db");

    public SunriseSunsetBot() {
        loadState();
        installAllNotifiers();
        scheduler.schedule(new ScheduledNotifiersInstaller(this),
                Date.from(LocalTime.of(0, 0) // Midnight
                        .atDate(LocalDate.now().plusDays(1)) // Tomorrow
                        .atZone(ZoneOffset.UTC) // At UTC
                        .toInstant()),
                60 * 60 * 24 * 1000); // Every 24 hours

        // Add shutdown hook to gracefully close db connection
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down...");
            persistenceManager.shutdown();
        }));
    }

    void installAllNotifiers() {
        for (Map.Entry<Long, UserState> userState : userStateMap.entrySet()) {
            if (userState.getValue().getStep() == Step.RUNNING) {
                Long chatId = userState.getKey();
                try {
                    installNotifier(chatId);
                } catch (ServiceException e) {
                    replyAndLogError(chatId, "ServiceException during installAllNotifiers.", e);
                }
            }
        }
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
                        installNotifier(chatId);
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

    private void installNotifier(long chatId) throws ServiceException {
        SunsetSunriseTimes times = calculateSunriseAndSunset(chatId);
        SunsetSunriseTimes timesTomorrow = null; // Deferred initialisation: a call to a REST service is expensive

        try {
            BotScheduler.ScheduleResult result =
                    scheduler.scheduleMessage(chatId, times.getSunriseTime(), SUNRISE_MESSAGE);

            // If message is not scheduled, we try to calculate the sunrise time for the following day and re-schedule.
            if (result == BotScheduler.ScheduleResult.NOT_SCHEDULED) {
                timesTomorrow = calculateSunriseAndSunset(chatId, LocalDate.now().plusDays(1));
                result = scheduler.scheduleMessage(
                        chatId, DateUtils.addDays(timesTomorrow.getSunriseTime(), 1), SUNRISE_MESSAGE);

                if (result == BotScheduler.ScheduleResult.NOT_SCHEDULED) {
                    LOG.error("Sunrise message not scheduled even for time [" + timesTomorrow.getSunriseTime() + "]");
                }
            }
        } catch (IllegalStateException e) {
            replyAndLogError(chatId, "IllegalStateException while scheduling message for Sunrise Time.", e);
        }

        try {
            BotScheduler.ScheduleResult result =
                    scheduler.scheduleMessage(chatId, times.getSunsetTime(), SUNSET_MESSAGE);

            // If message is not scheduled, we try to calculate the sunrise time for the following day and re-schedule.
            if (result == BotScheduler.ScheduleResult.NOT_SCHEDULED) {
                timesTomorrow = timesTomorrow != null ?
                        calculateSunriseAndSunset(chatId, LocalDate.now().plusDays(1)) :
                        timesTomorrow;
                result = scheduler.scheduleMessage(
                        chatId, DateUtils.addDays(timesTomorrow.getSunsetTime(), 1), SUNSET_MESSAGE);
                if (result == BotScheduler.ScheduleResult.NOT_SCHEDULED) {
                    LOG.error("Sunset message not scheduled even for time [" + timesTomorrow.getSunriseTime() + "]");
                }
            }
        } catch (IllegalStateException e) {
            replyAndLogError(chatId, "IllegalStateException while scheduling message for Sunset Time.", e);
        }
    }


    private void setLocation(long chatId, Location location) {
        UserState userState = userStateMap.get(chatId);
        userState.setCoordinates(new Coordinates(location.getLatitude(), location.getLongitude()));
        saveGlobalState();
    }

    private SunsetSunriseTimes calculateSunriseAndSunset(long chatId, LocalDate date) throws ServiceException {
        Coordinates coordinates = userStateMap.get(chatId).getCoordinates();
        return sunsetSunriseService.getSunsetSunriseTimes(coordinates, date);
    }

    private SunsetSunriseTimes calculateSunriseAndSunset(long chatId) throws ServiceException {
        return calculateSunriseAndSunset(chatId, LocalDate.now());
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
        } catch (TelegramApiException e) {
            //TODO gestire la rimozione della chat
            LOG.error("TelegramApiException during reply.", e);
        }
    }

    private void replyAndLogError(long chatId, String message, Throwable e) {
        String errorUUID = UUID.randomUUID().toString();
        LOG.error(message + " (" + errorUUID + ")", e);
        reply(chatId, "Oops, something went wrong. Please report this ID to support: " + errorUUID);
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