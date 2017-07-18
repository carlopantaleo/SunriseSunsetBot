package com.simpleplus.telegram.bots.components;

import org.telegram.telegrambots.api.objects.Update;

public class AdminCommandHandler extends CommandHandler implements BotBean {
    private SunriseSunsetBot bot;
    private MessageHandler messageHandler;
    private PersistenceManager persistenceManager;

    @Override
    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean("SunriseSunsetBot");
        this.messageHandler = (MessageHandler) BotContext.getDefaultContext().getBean("MessageHandler");
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean("PersistenceManager");
    }

    public boolean verifyAdminToken(Update update) {
        return getCommand(update) == Command.SET_ADMINISTRATOR &&
                getCommandArguments(update).equals(bot.getBotToken());
    }

    @Override
    public void handleCommand(Update update) {
        super.handleCommand(update);
    }
}
