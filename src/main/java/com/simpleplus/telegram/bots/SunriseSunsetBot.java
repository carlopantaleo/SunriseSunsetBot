package com.simpleplus.telegram.bots;


import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.helpers.Coordinates;
import com.simpleplus.telegram.bots.helpers.Step;
import com.simpleplus.telegram.bots.helpers.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.helpers.UserState;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import com.simpleplus.telegram.bots.services.impl.SunsetSunriseRemoteAPI;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;


public class SunriseSunsetBot extends TelegramLongPollingBot {

    private static final String SUNRISE_MESSAGE = "The sun is rising!";
    private static final String SUNSET_MESSAGE = "Sunset has begun!";

    private Map<Long, UserState> userStateMap = new HashMap<Long, UserState>();
    private static final Coordinates DEFAULT_COORDINATE = new Coordinates();
    private final String savedStateFile = "filename.txt"; // TODO: move in properties
    private SunsetSunriseService sunsetSunriseService = new SunsetSunriseRemoteAPI();
    private Timer schedule = new Timer();

    public SunriseSunsetBot() {
        loadState();
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
                try {
                    if (update.getMessage().hasLocation()) {
                        setLocation(chatId, update.getMessage().getLocation());
                        try {
                            installNotifier(chatId);
                            setNextStep(chatId);
                        } catch (ServiceException e) {
                            e.printStackTrace();
                        }
                    } else {
                        reply(chatId, "You aren't sending me a location. Please try again!");
                    }
                } catch (NumberFormatException e) {
                    reply(chatId, "Invalid numeric format. A valid format is 12.4523556");
                }
            }
            break;

            // Solo debug!
            case RUNNING: {
                try {
                    installNotifier(chatId);
                    setNextStep(chatId);
                } catch (NumberFormatException e) {
                    reply(chatId, "Invalid numeric format. A valid format is 12.4523556");
                } catch (ServiceException e) {
                    reply(chatId, "Oops, something went wrong... " + e.getMessage());
                    e.printStackTrace();
                }
            }
            break;
        }
    }

    private void installNotifier(long chatId) throws ServiceException {
        SunsetSunriseTimes times = calculateSunriseAndSunset(chatId);
        reply(chatId,
                "Sunset at " + times.getUTCSunsetTime().toString() + ", " +
                        "sunrise at " + times.getUTCSunriseTime().toString());

        // TODO: schedulare solo se non è già passato
        schedule.schedule(new ScheduledMessage(chatId, SUNRISE_MESSAGE, this),
                times.getSunriseTime());
        schedule.schedule(new ScheduledMessage(chatId, SUNSET_MESSAGE, this),
                times.getSunsetTime());

        //Debug
        schedule.schedule(new ScheduledMessage(chatId, SUNRISE_MESSAGE, this),
                Date.from(LocalTime.now().plusSeconds(10)
                        .atDate(LocalDate.now())
                        .atZone(ZoneId.systemDefault()).toInstant()));

    }

    private void setLocation(long chatId, Location location) {
        UserState userState = userStateMap.get(chatId);
        userState.setCoordinates(new Coordinates(location.getLatitude(), location.getLongitude()));
        saveState();

        reply(chatId, "Perfect! I'm going to calculate today's sunset and sunrise time.");
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
            e.printStackTrace();
        }
    }

    private void saveState() {
        try {
            OutputStream os = new FileOutputStream(savedStateFile);
            ObjectOutput oo = new ObjectOutputStream(os);
            oo.writeObject(userStateMap);
            oo.close();
        } catch (IOException e) {
            System.out.printf("Unable to save to file [%s]\n", savedStateFile);
        }
    }

    private void loadState() {
        try {
            InputStream is = new FileInputStream(savedStateFile);
            ObjectInput oi = new ObjectInputStream(is);
            userStateMap = (Map<Long, UserState>) oi.readObject();
            oi.close();
        } catch (IOException e) {
            System.out.printf("Unable to load file [%s]\n", savedStateFile);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
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