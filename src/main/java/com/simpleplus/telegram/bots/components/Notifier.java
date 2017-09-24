package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.components.tasks.ScheduledNotifiersInstaller;
import com.simpleplus.telegram.bots.datamodel.*;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import com.simpleplus.telegram.bots.services.impl.SunsetSunriseRemoteAPI;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.simpleplus.telegram.bots.components.BotScheduler.ScheduleResult.NOT_SCHEDULED;
import static com.simpleplus.telegram.bots.components.BotScheduler.ScheduleResult.NOT_TO_SCHEDULE;
import static com.simpleplus.telegram.bots.datamodel.Step.RUNNING;
import static com.simpleplus.telegram.bots.datamodel.Step.TO_ENTER_SUPPORT_MESSAGE;

public class Notifier implements BotBean {
    private static final Logger LOG = Logger.getLogger(Notifier.class);

    private SunriseSunsetBot bot;
    private SunsetSunriseService sunsetSunriseService;
    private BotScheduler scheduler;
    private PersistenceManager persistenceManager;

    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        this.scheduler = (BotScheduler) BotContext.getDefaultContext().getBean(BotScheduler.class);
        this.sunsetSunriseService =
                (SunsetSunriseService) BotContext.getDefaultContext().getBean(SunsetSunriseRemoteAPI.class);
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
    }

    public void installAllNotifiers() {
        for (Map.Entry<Long, UserState> userState : persistenceManager.getUserStatesMap().entrySet()) {
            if (userState.getValue().getStep().in(RUNNING, TO_ENTER_SUPPORT_MESSAGE)) {
                Long chatId = userState.getKey();
                try {
                    tryToInstallNotifier(chatId, 5);
                } catch (ServiceException e) {
                    bot.replyAndLogError(chatId, "ServiceException during installAllNotifiers", e);
                }
            }
        }
    }

    /**
     * Tries {@code numberOfTimes} times to install a notifier, otherwise throws ServiceException.
     *
     * @param chatId        the chat ID.
     * @param numberOfTimes number of retries.
     */
    public void tryToInstallNotifier(Long chatId, int numberOfTimes) throws ServiceException {
        for (int i = 0; i < numberOfTimes; i++) {
            try {
                installNotifier(chatId);
                return;
            } catch (ServiceException e) {
                LOG.warn("ServiceException during tryToInstallNotifier (attempt " +
                        Integer.toString(i) + ")... Sleeping 5 seconds.", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    LOG.error("InterruptedException while sleeping in tryToInstallNotifier.", e1);
                }
            }
        }
        throw new ServiceException("Cannot install notifier: service not available.");
    }

    private void installNotifier(long chatId) throws ServiceException {
        SunsetSunriseTimes times = calculateSunriseAndSunset(chatId);
        SunsetSunriseTimes timesTomorrow = null; // Deferred initialization: a call to a REST service is expensive

        for (UserAlert alert : getUserAlerts(chatId)) {
            try {
                timesTomorrow = scheduleMessage(chatId, times, timesTomorrow, alert.getTimeType());
            } catch (IllegalStateException e) {
                bot.replyAndLogError(chatId, "IllegalStateException while scheduling message for " +
                        alert.getTimeType().name() + " .", e);
            }
        }
    }

    private List<UserAlert> getUserAlerts(long chatId) {
        // At the moment it's a mock. Later it will retrieve alerts from database.
        ArrayList<UserAlert> alerts = new ArrayList<>();
        alerts.add(new UserAlert(chatId, TimeType.SUNRISE_TIME, 0));
        alerts.add(new UserAlert(chatId, TimeType.SUNSET_TIME, 0));
        return alerts;
    }

    private @Nullable
    SunsetSunriseTimes scheduleMessage(long chatId,
                                       SunsetSunriseTimes times,
                                       @Nullable SunsetSunriseTimes timesTomorrow,
                                       TimeType timeType) throws ServiceException {
        Date datetime = getDateTimeFromTimeType(times, timeType);
        BotScheduler.ScheduleResult result = scheduler.scheduleMessage(chatId, datetime, timeType.getMessage());

        // If message is not scheduled, we try to calculate the sunrise time for the following day and re-schedule.
        if (result.in(NOT_SCHEDULED, NOT_TO_SCHEDULE)) {
            if (timesTomorrow == null) {
                calculateSunriseAndSunset(chatId, LocalDate.now().plusDays(1));
            }

            Date datetimeTomorrow = DateUtils.addDays(getDateTimeFromTimeType(times, timeType), 1);
            result = scheduler.scheduleMessage(chatId, datetimeTomorrow, timeType.getMessage());

            if (result.in(NOT_SCHEDULED, NOT_TO_SCHEDULE)) {
                LOG.warn(String.format("%s message not scheduled even for time [%s]",
                        timeType.name(), datetimeTomorrow.toString()));
            }
        }

        return timesTomorrow;
    }

    private Date getDateTimeFromTimeType(SunsetSunriseTimes times, TimeType timeType) {
        switch (timeType) {
            case SUNRISE_TIME:
                return times.getSunriseTime();
            case SUNSET_TIME:
                return times.getSunsetTime();
            default:
                // Should never happen
                return Date.from(Instant.now());
        }
    }

    public void scheduleDailyAllNotifiersInstaller() {
        scheduler.schedule(new ScheduledNotifiersInstaller(),
                Date.from(LocalTime.of(0, 0) // Midnight
                        .atDate(LocalDate.now().plusDays(1)) // Tomorrow
                        .atZone(ZoneOffset.UTC) // At UTC
                        .toInstant()),
                60 * 60 * 24 * 1000); // Every 24 hours
    }

    private SunsetSunriseTimes calculateSunriseAndSunset(long chatId, LocalDate date) throws ServiceException {
        Coordinates coordinates = persistenceManager.getUserState(chatId).getCoordinates();
        return sunsetSunriseService.getSunsetSunriseTimes(coordinates, date);
    }

    private SunsetSunriseTimes calculateSunriseAndSunset(long chatId) throws ServiceException {
        return calculateSunriseAndSunset(chatId, LocalDate.now());
    }


}
