package com.simpleplus.telegram.bots.datamodel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;

public class SunsetSunriseTimes {
    private final LocalTime sunsetTime;
    private final LocalTime sunriseTime;

    public SunsetSunriseTimes(LocalTime sunsetTime, LocalTime sunriseTime) {
        this.sunsetTime = sunsetTime;
        this.sunriseTime = sunriseTime;
    }

    public LocalTime getUTCSunsetTime() {
        return sunsetTime;
    }

    public LocalTime getUTCSunriseTime() {
        return sunriseTime;
    }

    public Date getSunsetTime() {
        return getZonedTime(sunsetTime);
    }

    public Date getSunriseTime() {
        return getZonedTime(sunriseTime);
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
