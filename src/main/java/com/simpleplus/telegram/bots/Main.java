package com.simpleplus.telegram.bots;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.generics.BotSession;

public class Main {

    public static void main(String[] args) {

        ApiContextInitializer.init();

        TelegramBotsApi botsApi = new TelegramBotsApi();
        SunriseSunsetBot sunriseSunsetBot = new SunriseSunsetBot();

        try {
            BotSession session = botsApi.registerBot(sunriseSunsetBot);
            sunriseSunsetBot.setBotSession(session);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
