package com.simpleplus.telegram.bots;

import com.simpleplus.telegram.bots.components.*;
import com.simpleplus.telegram.bots.services.impl.SunsetSunriseRemoteAPI;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.generics.BotSession;

public class Main {

    public static void main(String[] args) {

        ApiContextInitializer.init();
        initDefaultBotContext();

        TelegramBotsApi botsApi = new TelegramBotsApi();
        SunriseSunsetBot sunriseSunsetBot =
                (SunriseSunsetBot) BotContext.getDefaultContext().getBean("SunriseSunsetBot");

        try {
            BotSession session = botsApi.registerBot(sunriseSunsetBot);
            sunriseSunsetBot.setBotSession(session);
            sunriseSunsetBot.start();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private static void initDefaultBotContext() {
        BotContext context = new BotContext();
        BotContext.setDefaultContext(context);
        context.addBean("SunriseSunsetBot", new SunriseSunsetBot());
        context.addBean("SunsetSunriseService", new SunsetSunriseRemoteAPI());
        context.addBean("Scheduler", new BotScheduler());
        context.addBean("Notifier", new Notifier());
        context.addBean("PersistenceManager", new PersistenceManager("sunrise-sunset-bot.db"));
        context.addBean("MessageHandler", new MessageHandler());
        context.initContext();
    }
}
