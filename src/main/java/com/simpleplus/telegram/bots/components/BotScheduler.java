package com.simpleplus.telegram.bots.components;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.simpleplus.telegram.bots.components.tasks.ScheduledMessage;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class BotScheduler implements BotBean {
    private static final Logger LOG = Logger.getLogger(BotScheduler.class);

    private Timer schedule = new Timer();
    private SunriseSunsetBot bot;
    private ListMultimap<Long, Task> scheduledMessages = ArrayListMultimap.create();

    @Override
    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
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
                TimerTask task = new ScheduledMessage(chatId, message, bot);
                schedule.schedule(task, time);
                LOG.info("Message for chatId[" + Long.toString(chatId) + "] scheduled at [" + time.toString() + "]");
                scheduledMessages.put(chatId, new Task(time, task));
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
        List<Task> tasks = scheduledMessages.get(chatId);
        return !tasks.stream()
                .filter(s -> s.datetimeScheduled.equals(time))
                .collect(Collectors.toList())
                .isEmpty();
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

    public void cancelAllScheduledMessages(long chatId) {
        List<Task> tasksToStop = scheduledMessages.removeAll(chatId);
        tasksToStop.forEach(s -> s.task.cancel());
        LOG.debug(String.format("Deleted these scheduled messages for chatId %d: %s", chatId, tasksToStop.toString()));
        LOG.info(String.format("Deleted %d scheduled messages for chatId %d", tasksToStop.size(), chatId));
        schedule.purge();
    }

    public enum ScheduleResult {
        SCHEDULED,
        NOT_SCHEDULED,
        NOT_TO_SCHEDULE;

        public boolean in(ScheduleResult... result) {
            return !Arrays.stream(result)
                    .filter(this::equals)
                    .collect(Collectors.toList())
                    .isEmpty();
        }
    }

    private class Task {
        Date datetimeScheduled;
        TimerTask task;

        public Task(Date datetimeScheduled, TimerTask task) {
            this.datetimeScheduled = datetimeScheduled;
            this.task = task;
        }

        @Override
        public String toString() {
            return "Task{" +
                    "datetimeScheduled=" + datetimeScheduled +
                    ", task=" + task +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Task task1 = (Task) o;

            if (datetimeScheduled != null ? !datetimeScheduled.equals(task1.datetimeScheduled) : task1.datetimeScheduled != null)
                return false;
            return task != null ? task.equals(task1.task) : task1.task == null;
        }
    }

}
