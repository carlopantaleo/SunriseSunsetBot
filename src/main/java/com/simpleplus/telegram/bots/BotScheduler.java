package com.simpleplus.telegram.bots;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.time.Instant;
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
            if (time.after(Date.from(Instant.now()))) {
                schedule.schedule(new ScheduledMessage(chatId, message, bot), time);
                LOG.info("Message for chatId[" + Long.toString(chatId) + "] scheduled at [" + time.toString() + "]");
            } else {
                LOG.info("Message for chatId[" + Long.toString(chatId) + "] NOT scheduled at [" + time.toString() + "]");
            }
        } catch (IllegalStateException e) {
            //TODO: handle this exception
            LOG.error("IllegalStateException during scheduleMessage.", e);
        }
    }

    public void schedule(TimerTask task, Date firstTime, long period) {
        // If firstTime is already passed, add period until firstTime gets in the future
        while (firstTime.before(Date.from(Instant.now()))) {
            firstTime = DateUtils.addMilliseconds(firstTime, (int) period);
        }

        schedule.schedule(task, firstTime, period);
        LOG.info("Task [" + task.toString() + "] scheduled at [" + firstTime.toString() + "] " +
                "every [" + Long.toString(period / 1000) + "] seconds.");
    }



}
