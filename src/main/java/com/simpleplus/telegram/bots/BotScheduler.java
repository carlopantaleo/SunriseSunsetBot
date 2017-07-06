package com.simpleplus.telegram.bots;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class BotScheduler {
    private Timer schedule = new Timer();
    private TelegramLongPollingBot bot;
    private static final Logger LOG = Logger.getLogger(BotScheduler.class);

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
                LOG.info("Message scheduled at " + time.toString());
            } else {
                LOG.info("Message NOT scheduled at " + time.toString());
            }
        } catch (IllegalStateException e) {
            //TODO: handle this exception
            LOG.error("IllegalStateException during scheduleMessage.", e);
        }
    }

    public void schedule(TimerTask task, Date firstTime, long period) {
        schedule.schedule(task, firstTime, period);
        LOG.info("Task scheduled at " + firstTime.toString() + " every " + Long.toString(period) + " seconds.");
    }

}
