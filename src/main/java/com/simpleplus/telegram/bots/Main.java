package com.simpleplus.telegram.bots;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class Main {

    public static void main(String[] args) {

        ApiContextInitializer.init();

        TelegramBotsApi botsApi = new TelegramBotsApi();
        SunriseSunsetBot alertBot = new SunriseSunsetBot();

        try {
            botsApi.registerBot(alertBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
