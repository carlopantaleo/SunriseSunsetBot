package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.components.tasks.ScheduledNotifiersInstaller;
import com.simpleplus.telegram.bots.datamodel.*;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.time.*;
import java.util.Date;
import java.util.Map;

import static com.simpleplus.telegram.bots.components.BotScheduler.ScheduleResult.NOT_SCHEDULED;
import static com.simpleplus.telegram.bots.components.BotScheduler.ScheduleResult.NOT_TO_SCHEDULE;
import static com.simpleplus.telegram.bots.components.UserAlertsManager.DRAFT_DELAY;
import static com.simpleplus.telegram.bots.datamodel.Step.RUNNING;
import static com.simpleplus.telegram.bots.datamodel.Step.TO_ENTER_SUPPORT_MESSAGE;

public class Notifier implements BotBean {
    private static final Logger LOG = LogManager.getLogger(Notifier.class);

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
                    tryToInstallNotifiers(chatId, 5);
                } catch (ServiceException e) {
                    bot.replyAndLogError(chatId, "ServiceException during installAllNotifiers", e);
                }

                for (UserAlert alert : persistenceManager.getUserAlerts(chatId)) {
                    if (alert.getDelay() == DRAFT_DELAY) {
                        persistenceManager.deleteUserAlert(chatId, alert.getId());
                        LOG.info("Deleted draft alert #{}", alert.getId());
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
    public void tryToInstallNotifiers(Long chatId, int numberOfTimes) throws ServiceException {
        for (int i = 0; i < numberOfTimes; i++) {
            try {
                installNotifiers(chatId);
                return;
            } catch (ServiceException e) {
                LOG.warn("ServiceException during tryToInstallNotifiers (attempt " + Integer.toString(i) +
                         ")... Sleeping 60 seconds.", e);
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e1) {
                    LOG.error("InterruptedException while sleeping in tryToInstallNotifiers.", e1);
                }
            }
        }
        throw new ServiceException("Cannot install notifier: service not available.");
    }

    private void installNotifiers(long chatId) throws ServiceException {
        SunsetSunriseTimes times = calculateSunriseAndSunset(chatId);
        SunsetSunriseTimes timesTomorrow = null; // Deferred initialization: a call to a REST service is expensive

        for (UserAlert alert : userAlertsManager.getUserAlerts(chatId)) {
            try {
                if (alert.getDelay() != DRAFT_DELAY) {
                    timesTomorrow = scheduleMessage(chatId, times, timesTomorrow, alert.getTimeType(),
                            alert.getDelay());
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

            Date datetimeTomorrow = getDateTimeFromTimeType(timesTomorrow, timeType);
            datetimeTomorrow = DateUtils.addMinutes(datetimeTomorrow, (int) delay);
            result = scheduler.scheduleMessage(chatId, datetimeTomorrow, formatMessage(timeType, delay));

            if (result.in(NOT_SCHEDULED, NOT_TO_SCHEDULE)) {
                LOG.warn("{} message not scheduled even for time {}", timeType.name(), datetimeTomorrow.toString());
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
        LocalDateTime dateTime = times.getTime(timeType.getInternalName());
        if (dateTime == null) {
            // Should never happen
            return Date.from(Instant.now());
        }

        return Date.from(dateTime.toInstant(ZoneOffset.UTC));
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
