package com.simpleplus.telegram.bots.helpers;

import java.time.LocalTime;

public class SunsetSunriseTimes {
    private final LocalTime sunsetTime;
    private final LocalTime sunriseTime;

    public LocalTime getSunsetTime() {
        return sunsetTime;
    }

    public LocalTime getSunriseTime() {
        return sunriseTime;
    }

    public SunsetSunriseTimes(LocalTime sunsetTime, LocalTime sunriseTime) {
        this.sunsetTime = sunsetTime;
        this.sunriseTime = sunriseTime;
    }
}
