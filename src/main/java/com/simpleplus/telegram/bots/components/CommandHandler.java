package com.simpleplus.telegram.bots.components;

import org.telegram.telegrambots.api.objects.Update;

public class CommandHandler implements BotBean {
    private SunriseSunsetBot bot;
    private MessageHandler messageHandler;

    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean("SunriseSunsetBot");
        this.messageHandler = (MessageHandler) BotContext.getDefaultContext().getBean("MessageHandler");
    }

    public boolean isCommand(Update update) {
        return update.getMessage().getText().charAt(0) == '/';
    }

    public void handleCommand(Update update) {
        switch (getCommand(update)) {
            //TODO
        }
    }

    private Command getCommand(Update update) {
        String text = update.getMessage().getText();
        String command = text.substring(text.indexOf("/")).split(" ")[0];

        switch (command) {
            case "change-location":
                return Command.REENTER_LOCATION;
            default:
                return null;
        }
    }

    private enum Command {
        REENTER_LOCATION
    }
}
