package com.simpleplus.telegram.bots.helpers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

public class SunsetSunriseTimes {
    private final LocalTime sunsetTime;
    private final LocalTime sunriseTime;
    ZoneId timeZone = ZoneId.systemDefault(); // TODO: internazionalizzare

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
        return getGlobalTime(sunsetTime);
    }

    public Date getSunriseTime() {
        return getGlobalTime(sunriseTime);
    }

    private Date getGlobalTime(LocalTime localTime) {
        Instant instant = localTime.atDate(LocalDate.now()).
                atZone(timeZone).toInstant();
        return Date.from(instant);
    }
}
