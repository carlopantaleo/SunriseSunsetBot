package com.simpleplus.telegram.bots;

import com.simpleplus.telegram.bots.components.*;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import com.simpleplus.telegram.bots.services.impl.SunsetSunriseRemoteAPI;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.generics.BotSession;

public class Main {

    public static void main(String[] args) {
        ApiContextInitializer.init();
        PropertiesManager.setArgv(args);
        initDefaultBotContext();

        TelegramBotsApi botsApi = new TelegramBotsApi();
        SunriseSunsetBot sunriseSunsetBot =
                (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);

        try {
            BotSession session = botsApi.registerBot(sunriseSunsetBot);
            sunriseSunsetBot.setBotSession(session);
            sunriseSunsetBot.start();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void initDefaultBotContext() {
        BotContext context = new BotContext();
        BotContext.setDefaultContext(context);
        context.addBean(SunriseSunsetBot.class);
        context.addBean(SunsetSunriseService.class, new SunsetSunriseRemoteAPI());
        context.addBean(BotScheduler.class);
        context.addBean(Notifier.class);
        context.addBean(PersistenceManager.class);
        context.addBean(MessageHandler.class);
        context.addBean(MessageHandler.class);
        context.addBean(CommandHandler.class);
        context.addBean(AdminCommandHandler.class);
        context.addBean(PropertiesManager.class);
        context.addBean(UserAlertsManager.class);
        context.initContext();
    }
}
