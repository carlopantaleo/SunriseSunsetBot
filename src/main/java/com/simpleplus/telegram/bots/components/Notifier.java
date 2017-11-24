package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.components.tasks.ScheduledNotifiersInstaller;
import com.simpleplus.telegram.bots.datamodel.*;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

import static com.simpleplus.telegram.bots.components.BotScheduler.ScheduleResult.NOT_SCHEDULED;
import static com.simpleplus.telegram.bots.components.BotScheduler.ScheduleResult.NOT_TO_SCHEDULE;
import static com.simpleplus.telegram.bots.components.UserAlertsManager.DRAFT_DELAY;
import static com.simpleplus.telegram.bots.datamodel.Step.RUNNING;
import static com.simpleplus.telegram.bots.datamodel.Step.TO_ENTER_SUPPORT_MESSAGE;

public class Notifier implements BotBean {
    private static final Logger LOG = Logger.getLogger(Notifier.class);

    private SunriseSunsetBot bot;
    private SunsetSunriseService sunsetSunriseService;
    private BotScheduler scheduler;
    private PersistenceManager persistenceManager;
    private UserAlertsManager userAlertsManager;

    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        this.scheduler = (BotScheduler) BotContext.getDefaultContext().getBean(BotScheduler.class);
        this.sunsetSunriseService =
                (SunsetSunriseService) BotContext.getDefaultContext().getBean(SunsetSunriseService.class);
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        this.userAlertsManager =
                (UserAlertsManager) BotContext.getDefaultContext().getBean(UserAlertsManager.class);
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

                for (UserAlert alert : persistenceManager.getUserAlerts(chatId)) {
                    if (alert.getDelay() == DRAFT_DELAY) {
                        persistenceManager.deleteUserAlert(chatId, alert.getId());
                        LOG.info(String.format("Deleted draft alert #%d", alert.getId()));
                    }
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

        for (UserAlert alert : userAlertsManager.getUserAlerts(chatId)) {
            try {
                if (alert.getDelay() != DRAFT_DELAY) {
                    timesTomorrow = scheduleMessage(chatId, times, timesTomorrow, alert.getTimeType(), alert.getDelay());
                }
            } catch (IllegalStateException e) {
                bot.replyAndLogError(chatId, "IllegalStateException while scheduling message for " +
                        alert.getTimeType().name() + " .", e);
            }
        }
    }

    private @Nullable
    SunsetSunriseTimes scheduleMessage(long chatId,
                                       SunsetSunriseTimes times,
                                       @Nullable SunsetSunriseTimes timesTomorrow,
                                       TimeType timeType,
                                       long delay) throws ServiceException {
        Date datetime = DateUtils.addMinutes(getDateTimeFromTimeType(times, timeType), (int) delay);
        BotScheduler.ScheduleResult result =
                scheduler.scheduleMessage(chatId, datetime, formatMessage(timeType, delay));

        // If message is not scheduled, we try to calculate the sunrise time for the following day and re-schedule.
        if (result.in(NOT_SCHEDULED, NOT_TO_SCHEDULE)) {
            if (timesTomorrow == null) {
                timesTomorrow = calculateSunriseAndSunset(chatId, LocalDate.now().plusDays(1));
            }

            Date datetimeTomorrow = DateUtils.addDays(getDateTimeFromTimeType(timesTomorrow, timeType), 1);
            datetimeTomorrow = DateUtils.addMinutes(datetimeTomorrow, (int) delay);
            result = scheduler.scheduleMessage(chatId, datetimeTomorrow, formatMessage(timeType, delay));

            if (result.in(NOT_SCHEDULED, NOT_TO_SCHEDULE)) {
                LOG.warn(String.format("%s message not scheduled even for time [%s]",
                        timeType.name(), datetimeTomorrow.toString()));
            }
        }

        return timesTomorrow;
    }

    private String formatMessage(TimeType timeType, long delay) {
        if (delay == 0) {
            return timeType.getMessage();
        } else {
            return String.format(timeType.getMessage(), Math.abs(delay));
        }
    }

    private Date getDateTimeFromTimeType(SunsetSunriseTimes times, TimeType timeType) {
        switch (timeType) {
            case SUNRISE_TIME:
            case SUNRISE_TIME_ANTICIPATION:
                return times.getSunriseTime();
            case SUNSET_TIME_ANTICIPATION:
            case SUNSET_TIME:
                return times.getSunsetTime();
            case CIVIL_TWILIGHT_BEGIN_TIME:
            case CIVIL_TWILIGHT_BEGIN_TIME_ANTICIPATION:
                return times.getCivilTwilightBeginTime();
            case CIVIL_TWILIGHT_END_TIME:
            case CIVIL_TWILIGHT_END_TIME_ANTICIPATION:
                return times.getCivilTwilightEndTime();

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
