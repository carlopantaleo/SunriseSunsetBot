package com.simpleplus.telegram.bots;

import com.simpleplus.telegram.bots.components.*;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import com.simpleplus.telegram.bots.services.impl.SunsetSunriseRemoteAPI;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {

    public static void main(String[] args) {
        PropertiesManager.setArgv(args);
        initDefaultBotContext();

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            SunriseSunsetBot sunriseSunsetBot =
                    (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);

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
