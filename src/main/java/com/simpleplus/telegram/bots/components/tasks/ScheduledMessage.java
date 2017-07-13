package com.simpleplus.telegram.bots.components.tasks;

import com.simpleplus.telegram.bots.SunriseSunsetBot;

import java.util.TimerTask;

public class ScheduledMessage extends TimerTask {

    private final Long chatID;
    private final String message;
    private final SunriseSunsetBot bot;

    public ScheduledMessage(Long chatID, String message, SunriseSunsetBot bot) {
        this.chatID = chatID;
        this.message = message;
        this.bot = bot;
    }

    @Override
    public void run() {
        bot.reply(chatID, message);
    }

}
