package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.components.tasks.ScheduledNotifiersInstaller;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.datamodel.UserState;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import com.simpleplus.telegram.bots.services.impl.SunsetSunriseRemoteAPI;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

import static com.simpleplus.telegram.bots.components.BotScheduler.ScheduleResult.NOT_SCHEDULED;
import static com.simpleplus.telegram.bots.components.BotScheduler.ScheduleResult.NOT_TO_SCHEDULE;

public class Notifier implements BotBean {
    private static final String SUNRISE_MESSAGE = "The sun is rising!";
    private static final String SUNSET_MESSAGE = "Sunset has begun!";
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
            if (userState.getValue().getStep() == Step.RUNNING) {
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

    public void installNotifier(long chatId) throws ServiceException {
        SunsetSunriseTimes times = calculateSunriseAndSunset(chatId);
        SunsetSunriseTimes timesTomorrow = null; // Deferred initialization: a call to a REST service is expensive

        try {
            timesTomorrow = scheduleSunriseSunsetMessage(chatId, times, null, SUNRISE_MESSAGE);
        } catch (IllegalStateException e) {
            bot.replyAndLogError(chatId, "IllegalStateException while scheduling message for Sunrise Time.", e);
        }

        try {
            scheduleSunriseSunsetMessage(chatId, times, timesTomorrow, SUNSET_MESSAGE);
        } catch (IllegalStateException e) {
            bot.replyAndLogError(chatId, "IllegalStateException while scheduling message for Sunset Time.", e);
        }
    }

    private @Nullable
    SunsetSunriseTimes scheduleSunriseSunsetMessage(long chatId,
                                                    SunsetSunriseTimes times,
                                                    @Nullable SunsetSunriseTimes timesTomorrow,
                                                    String message) throws ServiceException { // TODO: sostituire questo con qualcosa di più furbo
        BotScheduler.ScheduleResult result =
                scheduler.scheduleMessage(chatId, times.getSunriseTime(), message);

        // If message is not scheduled, we try to calculate the sunrise time for the following day and re-schedule.
        if (result.in(NOT_SCHEDULED, NOT_TO_SCHEDULE)) {
            timesTomorrow = timesTomorrow != null ?
                    timesTomorrow : calculateSunriseAndSunset(chatId, LocalDate.now().plusDays(1));
            Date datetimeTomorrow = DateUtils.addDays(
                    SUNRISE_MESSAGE.equals(message) ? timesTomorrow.getSunriseTime() : timesTomorrow.getSunsetTime(),
                    1);
            result = scheduler.scheduleMessage(
                    chatId, datetimeTomorrow, message);

            if (result.in(NOT_SCHEDULED, NOT_TO_SCHEDULE)) {
                LOG.warn(String.format("%s message not scheduled even for time [%s]",
                        SUNRISE_MESSAGE.equals(message) ? SUNRISE_MESSAGE : SUNSET_MESSAGE,
                        datetimeTomorrow.toString()));
            }
        }
        return timesTomorrow;
    }

    public void scheduleDailyAllNotifiersInstaller() {
        scheduler.schedule(new ScheduledNotifiersInstaller(this),
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
