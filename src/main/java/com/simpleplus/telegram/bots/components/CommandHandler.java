package com.simpleplus.telegram.bots.components;

import org.telegram.telegrambots.api.objects.Update;

public class CommandHandler implements BotBean {
    private SunriseSunsetBot bot;

    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean("SunriseSunsetBot");
    }

    public boolean isCommand(Update update) {
        return update.getMessage().getText().charAt(0) == '/';
    }
}
