package com.simpleplus.telegram.bots;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Timer;

public class BotScheduler {
    private Timer schedule = new Timer();
    private TelegramLongPollingBot bot;

    public BotScheduler(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    public void scheduleMessage(long chatId, Date time, String message) {
        try {
            // Schedule message only if time >= now
            if (time.compareTo(Date.from(LocalTime.now()
                    .atDate(LocalDate.now())
                    .atZone(ZoneOffset.systemDefault())
                    .toInstant())) >= 0) {
                schedule.schedule(new ScheduledMessage(chatId, message, bot), time);
            }
            System.out.println("Message scheduled at " + time.toString());
        } catch (IllegalStateException e) {
            //TODO: handle this exception
            e.printStackTrace();
        }

    }

}