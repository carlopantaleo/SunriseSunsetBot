package com.simpleplus.telegram.bots;


import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.helpers.Coordinates;
import com.simpleplus.telegram.bots.helpers.Step;
import com.simpleplus.telegram.bots.helpers.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.helpers.UserState;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import com.simpleplus.telegram.bots.services.impl.SunsetSunriseRemoteAPI;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/* Lista della spesa
    - Creare un task che ogni giorno installi tutti i notifier del giorno.
    - Lavorare sul PersistenceManager
    - Inserire un sistema di logging furbo
 */
public class SunriseSunsetBot extends TelegramLongPollingBot {

    private static final String SUNRISE_MESSAGE = "The sun is rising!";
    private static final String SUNSET_MESSAGE = "Sunset has begun!";
    private static final Coordinates DEFAULT_COORDINATE = new Coordinates();
    private static final Logger LOG = Logger.getLogger(SunriseSunsetBot.class);

    private final String savedStateFile = "filename.txt"; // TODO: dismiss in favour of PersistenceManager

    /**
     * The map where states for each user are stored. Once the bot is constructed, it must contain all states which
     * were previously saved to disk.
     */
    private Map<Long, UserState> userStateMap = new HashMap<Long, UserState>();

    /**
     * The service which provides sunset and sunrise times.
     */
    private SunsetSunriseService sunsetSunriseService = new SunsetSunriseRemoteAPI();

    /**
     * The scheduler which can be used to schedule messages and generic events.
     */
    private BotScheduler scheduler = new BotScheduler(this);

    public SunriseSunsetBot() {
        loadState();
        installAllNotifiers();
        scheduler.schedule(new ScheduledNotifiersInstaller(this),
                Date.from(LocalTime.of(0, 0) // Midnight
                        .atDate(LocalDate.now().plusDays(1)) // Tomorrow
                        .atZone(ZoneOffset.UTC) // At UTC
                        .toInstant()),
                60*60*24); // Every 24 hours
    }

    void installAllNotifiers() {
        for (Map.Entry<Long, UserState> userState : userStateMap.entrySet()) {
            if (userState.getValue().getStep() == Step.RUNNING) {
                try {
                    installNotifier(userState.getKey());
                } catch (ServiceException e) {
                    //TODO: handle this exception
                    LOG.error("ServiceException during installAllNotifiers.", e);
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
                        LOG.error("ServiceException during onUpdateReceived.", e);
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

        try {
            scheduler.scheduleMessage(chatId, times.getSunriseTime(), SUNRISE_MESSAGE);
        } catch (IllegalStateException e) {
            LOG.info("IllegalStateException while scheduling message for Sunrise Time.", e);
        }

        try {
            scheduler.scheduleMessage(chatId, times.getSunsetTime(), SUNSET_MESSAGE);
        } catch (IllegalStateException e) {
            LOG.info("IllegalStateException while scheduling message for Sunset Time.", e);
        }
    }


    private void setLocation(long chatId, Location location) {
        UserState userState = userStateMap.get(chatId);
        userState.setCoordinates(new Coordinates(location.getLatitude(), location.getLongitude()));
        saveState();
    }

    private SunsetSunriseTimes calculateSunriseAndSunset(long chatId) throws ServiceException {
        Coordinates coordinates = userStateMap.get(chatId).getCoordinates();
        return sunsetSunriseService.getSunsetSunriseTimes(coordinates);
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
        saveState();
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
        saveState();
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

    private void saveState() {
        try {
            OutputStream os = new FileOutputStream(savedStateFile);
            ObjectOutput oo = new ObjectOutputStream(os);
            oo.writeObject(userStateMap);
            oo.close();
        } catch (IOException e) {
            LOG.error("Unable to save to file [" + savedStateFile + "]", e);
        }
    }

    private void loadState() {
        try {
            InputStream is = new FileInputStream(savedStateFile);
            ObjectInput oi = new ObjectInputStream(is);
            userStateMap = (Map<Long, UserState>) oi.readObject();
            oi.close();
        } catch (IOException e) {
            LOG.error("Unable to load file [" + savedStateFile + "]", e);
        } catch (ClassNotFoundException e) {
            LOG.error("ClassNotFoundException - ", e);
        }
    }


    public String getBotUsername() {
        return "SunriseSunset_bot";
    }

    @Override
    public String getBotToken() {
        return "416777734:AAF5r6MdiQcsr1XKtiiUg69AGBp7JBG_IlQ";
    }

}