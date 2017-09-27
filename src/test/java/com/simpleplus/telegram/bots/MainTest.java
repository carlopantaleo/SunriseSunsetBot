package com.simpleplus.telegram.bots;

import com.simpleplus.telegram.bots.components.*;
import com.simpleplus.telegram.bots.mocks.PersistenceManagerWithTestDB;
import com.simpleplus.telegram.bots.mocks.SunriseSunsetServiceMock;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;

public class MainTest {
    public static void initDefaultBotContext() {
        BotContext context = new BotContext();
        BotContext.setDefaultContext(context);
        context.addBean(SunriseSunsetBot.class);
        context.addBean(SunsetSunriseService.class, new SunriseSunsetServiceMock());
        context.addBean(BotScheduler.class);
        context.addBean(Notifier.class);
        context.addBean(PersistenceManager.class, new PersistenceManagerWithTestDB());
        context.addBean(MessageHandler.class);
        context.addBean(CommandHandler.class);
        context.addBean(AdminCommandHandler.class);
        context.addBean(PropertiesManager.class);
        context.addBean(UserAlertsManager.class);
        context.initContext();
    }
}