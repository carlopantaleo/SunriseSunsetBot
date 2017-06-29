package com.simpleplus.telegram.bots;


import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.helpers.Coordinates;
import com.simpleplus.telegram.bots.helpers.Step;
import com.simpleplus.telegram.bots.helpers.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.helpers.UserState;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import com.simpleplus.telegram.bots.services.impl.SunsetSunriseRemoteAPI;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;


public class SunriseSunsetBot extends TelegramLongPollingBot {

    private Timer botTimer = new Timer();
    private Map<Long, UserState> userStateMap = new HashMap<Long, UserState>();
    private static final Coordinates DEFAULT_COORDINATE = new Coordinates();
    private final String savedStateFile = "filename.txt"; // TODO: move in properties
    private SunsetSunriseService sunsetSunriseService = new SunsetSunriseRemoteAPI();

    public SunriseSunsetBot() {
        loadState();
    }

    public void onUpdateReceived(Update update) {
        if (!(update.hasMessage() && update.getMessage().hasText()))
            return;

        final long chatId = update.getMessage().getChatId();

        // Se la chat è nuova faccio varie inizializzazioni
        if (!isChatNew(chatId)) {
            gestNewChat(chatId);
            return;
        }

        // Altrimenti procedo con gli step
        switch (userStateMap.get(chatId).getStep()) {
            case TO_ENTER_LATITUDE: {
                try {
                    setLatitude(chatId, update.getMessage().getText());
                    setNextStep(chatId);
                } catch (NumberFormatException e) {
                    reply(chatId, "Invalid numeric format. A valid format is 12.4523556");
                }
            }
            break;

            case TO_ENTER_LONGITUDE: {
                try {
                    setLongitude(chatId, update.getMessage().getText());
                    SunsetSunriseTimes times = calculateSunriseAndSunset(chatId);
                    reply(chatId,
                            "Sunset at " + times.getSunsetTime().toString() + ", " +
                                    "sunrise at " + times.getSunriseTime().toString());
                    setNextStep(chatId);
                } catch (NumberFormatException e) {
                    reply(chatId, "Invalid numeric format. A valid format is 12.4523556");
                } catch (ServiceException e) {
                    reply(chatId, "Oops, something went wrong... " + e.getMessage());
                    e.printStackTrace();
                }
            }
            break;

            // Solo debug!
            case RUNNING: {
                try {
                    SunsetSunriseTimes times = calculateSunriseAndSunset(chatId);
                    reply(chatId,
                            "Sunset at " + times.getSunsetTime().toString() + ", " +
                                    "sunrise at " + times.getSunriseTime().toString());
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

    private SunsetSunriseTimes calculateSunriseAndSunset(long chatId) throws ServiceException {
        Coordinates coordinates = userStateMap.get(chatId).getCoordinates();
        return sunsetSunriseService.getSunsetSunriseTimes(coordinates);
    }

    private void setNextStep(long chatId) {
        UserState userState = userStateMap.get(chatId);

        switch (userState.getStep()) {
            case NEW_CHAT:
                userState.setStep(Step.TO_ENTER_LATITUDE);
                break;
            case TO_ENTER_LATITUDE:
                userState.setStep(Step.TO_ENTER_LONGITUDE);
                break;
            case TO_ENTER_LONGITUDE:
                userState.setStep(Step.RUNNING);
                break;
        }
        saveState();
    }

    private void setLongitude(long chatId, String text) {
        UserState userState = userStateMap.get(chatId);
        userState.setCoordinates(new Coordinates(userState.getCoordinates().getLatitude(), new BigDecimal(text)));
        saveState();

        reply(chatId, "Perfect! I'm going to calculate today's sunset and sunrise time.");
    }

    private void setLatitude(long chatId, String text) {
        UserState userState = userStateMap.get(chatId);
        userState.setCoordinates(new Coordinates(new BigDecimal(text), BigDecimal.ZERO));
        saveState();

        reply(chatId, "Good! Now enter your longitude.");
    }

    private boolean isChatNew(long chatId) {
        return userStateMap.containsKey(chatId);
    }

    private void gestNewChat(long chatId) {
        reply(chatId, "Welcome! Please enter your latitude (eg. 12.4523556)");

        userStateMap.put(chatId, new UserState(DEFAULT_COORDINATE, Step.TO_ENTER_LATITUDE));
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