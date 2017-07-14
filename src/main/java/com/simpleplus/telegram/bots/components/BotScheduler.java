package com.simpleplus.telegram.bots.components;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.simpleplus.telegram.bots.SunriseSunsetBot;
import com.simpleplus.telegram.bots.components.tasks.ScheduledMessage;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BotScheduler {
    private static final Logger LOG = Logger.getLogger(BotScheduler.class);

    private Timer schedule = new Timer();
    private SunriseSunsetBot bot;
    private ListMultimap<Long, Date> scheduledMessages = ArrayListMultimap.create();

    public BotScheduler(SunriseSunsetBot bot) {
        this.bot = bot;
    }

    public ScheduleResult scheduleMessage(long chatId, Date time, String message) {
        if (alreadyScheduled(chatId, time)) {
            LOG.info("A message for chatId[" + Long.toString(chatId) + "] is already " +
                    "scheduled at [" + time.toString() + "]");
            return ScheduleResult.NOT_TO_SCHEDULE;
        }

        try {
            // Schedule message only if time >= now
            if (time.after(Date.from(Instant.now().atZone(ZoneId.systemDefault()).toInstant()))) {
                schedule.schedule(new ScheduledMessage(chatId, message, bot), time);
                LOG.info("Message for chatId[" + Long.toString(chatId) + "] scheduled at [" + time.toString() + "]");
                scheduledMessages.put(chatId, time);
                return ScheduleResult.SCHEDULED;
            } else {
                LOG.info("Message for chatId[" + Long.toString(chatId) + "] " +
                        "NOT scheduled at [" + time.toString() + "] (date is before now)");
                return ScheduleResult.NOT_SCHEDULED;
            }
        } catch (IllegalStateException e) {
            LOG.error("IllegalStateException during scheduleMessage.", e);
            return ScheduleResult.NOT_SCHEDULED;
        }
    }

    //TODO: unit test!
    private boolean alreadyScheduled(long chatId, Date time) {
        List<Date> dates = scheduledMessages.get(chatId);
        return dates.contains(time);
    }

    public ScheduleResult schedule(TimerTask task, Date firstTime, long period) {
        // If firstTime is already passed, add period until firstTime gets in the future
        while (firstTime.before(Date.from(Instant.now().atZone(ZoneId.systemDefault()).toInstant()))) {
            firstTime = DateUtils.addMilliseconds(firstTime, (int) period);
        }

        try {
            schedule.scheduleAtFixedRate(task, firstTime, period);
            LOG.info("Task [" + task.toString() + "] scheduled at [" + firstTime.toString() + "] " +
                    "every [" + Long.toString(period / 1000) + "] seconds.");
        } catch (IllegalStateException e) {
            LOG.error("IllegalStateException during schedule.", e);
            return ScheduleResult.NOT_SCHEDULED;
        }

        return ScheduleResult.SCHEDULED;
    }

    public enum ScheduleResult {
        SCHEDULED,
        NOT_SCHEDULED,
        NOT_TO_SCHEDULE
    }

}
