package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.components.tasks.ScheduledNotifiersInstaller;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.datamodel.UserState;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import com.simpleplus.telegram.bots.services.impl.SunsetSunriseRemoteAPI;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

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

    //TODO: unit test! Questo metodo è troppo spaghettoso...
    public void installNotifier(long chatId) throws ServiceException {
        SunsetSunriseTimes times = calculateSunriseAndSunset(chatId);
        SunsetSunriseTimes timesTomorrow = null; // Deferred initialisation: a call to a REST service is expensive

        try {
            BotScheduler.ScheduleResult result =
                    scheduler.scheduleMessage(chatId, times.getSunriseTime(), SUNRISE_MESSAGE);

            // If message is not scheduled, we try to calculate the sunrise time for the following day and re-schedule.
            if (result == BotScheduler.ScheduleResult.NOT_SCHEDULED) {
                timesTomorrow = calculateSunriseAndSunset(chatId, LocalDate.now().plusDays(1));
                result = scheduler.scheduleMessage(
                        chatId, DateUtils.addDays(timesTomorrow.getSunriseTime(), 1), SUNRISE_MESSAGE);

                if (result == BotScheduler.ScheduleResult.NOT_SCHEDULED) {
                    LOG.warn("Sunrise message not scheduled even for time [" + timesTomorrow.getSunriseTime() + "]");
                }
            }
        } catch (IllegalStateException e) {
            bot.replyAndLogError(chatId, "IllegalStateException while scheduling message for Sunrise Time.", e);
        }

        try {
            BotScheduler.ScheduleResult result =
                    scheduler.scheduleMessage(chatId, times.getSunsetTime(), SUNSET_MESSAGE);

            // If message is not scheduled, we try to calculate the sunrise time for the following day and re-schedule.
            if (result == BotScheduler.ScheduleResult.NOT_SCHEDULED) {
                timesTomorrow = timesTomorrow != null ?
                        calculateSunriseAndSunset(chatId, LocalDate.now().plusDays(1)) :
                        timesTomorrow;
                result = scheduler.scheduleMessage(
                        chatId, DateUtils.addDays(timesTomorrow.getSunsetTime(), 1), SUNSET_MESSAGE);
                if (result == BotScheduler.ScheduleResult.NOT_SCHEDULED) {
                    LOG.warn("Sunset message not scheduled even for time [" + timesTomorrow.getSunriseTime() + "]");
                }
            }
        } catch (IllegalStateException e) {
            bot.replyAndLogError(chatId, "IllegalStateException while scheduling message for Sunset Time.", e);
        }
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
