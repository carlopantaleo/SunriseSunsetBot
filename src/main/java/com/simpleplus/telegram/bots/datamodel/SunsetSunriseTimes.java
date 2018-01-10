package com.simpleplus.telegram.bots.datamodel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;

public class SunsetSunriseTimes {
    private final LocalTime sunsetTime;
    private final LocalTime sunriseTime;
    private final LocalTime civilTwilightEndTime;
    private final LocalTime civilTwilightBeginTime;
    private final LocalTime nauticalTwilightEndTime;
    private final LocalTime nauticalTwilightBeginTime;
    private final LocalTime astronomicalTwilightEndTime;
    private final LocalTime astronomicalTwilightBeginTime;

    public SunsetSunriseTimes(LocalTime sunsetTime,
                              LocalTime sunriseTime,
                              LocalTime civilTwilightEndTime,
                              LocalTime civilTwilightBeginTime,
                              LocalTime nauticalTwilightEndTime,
                              LocalTime nauticalTwilightBeginTime,
                              LocalTime astronomicalTwilightEndTime,
                              LocalTime astronomicalTwilightBeginTime) {
        this.sunsetTime = sunsetTime;
        this.sunriseTime = sunriseTime;
        this.civilTwilightEndTime = civilTwilightEndTime;
        this.civilTwilightBeginTime = civilTwilightBeginTime;
        this.nauticalTwilightEndTime = nauticalTwilightEndTime;
        this.nauticalTwilightBeginTime = nauticalTwilightBeginTime;
        this.astronomicalTwilightEndTime = astronomicalTwilightEndTime;
        this.astronomicalTwilightBeginTime = astronomicalTwilightBeginTime;
    }

    public Date getSunsetTime() {
        return getZonedTime(sunsetTime);
    }

    public Date getSunriseTime() {
        return getZonedTime(sunriseTime);
    }

    public Date getCivilTwilightEndTime() {
        return getZonedTime(civilTwilightEndTime);
    }

    public Date getCivilTwilightBeginTime() {
        return getZonedTime(civilTwilightBeginTime);
    }

    public Date getNauticalTwilightEndTime() {
        return getZonedTime(nauticalTwilightEndTime);
    }

    public Date getNauticalTwilightBeginTime() {
        return getZonedTime(nauticalTwilightBeginTime);
    }

    public Date getAstronomicalTwilightEndTime() {
        return getZonedTime(astronomicalTwilightEndTime);
    }

    public Date getAstronomicalTwilightBeginTime() {
        return getZonedTime(astronomicalTwilightBeginTime);
    }

    /**
     * Returns a {@link Date} zoned date-time from a {@link LocalTime}.
     *
     * @param localTime the non-zoned time. It is implicitly assumed to be UTC.
     * @return
     */
    private Date getZonedTime(LocalTime localTime) {
        Instant instant = localTime.atDate(LocalDate.now()).
                atZone(ZoneOffset.UTC).toInstant();
        return Date.from(instant);
    }
}
