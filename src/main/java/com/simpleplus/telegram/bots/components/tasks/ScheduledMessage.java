package com.simpleplus.telegram.bots.components.tasks;

import com.simpleplus.telegram.bots.components.BotContext;
import com.simpleplus.telegram.bots.components.SunriseSunsetBot;

import java.util.TimerTask;

public class ScheduledMessage extends TimerTask {

    private final Long chatID;
    private final String message;
    private final SunriseSunsetBot bot;

    public ScheduledMessage(Long chatID, String message) {
        this.chatID = chatID;
        this.message = message;
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
    }

    @Override
    public void run() {
        bot.reply(chatID, message);
    }

}
