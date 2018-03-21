package com.simpleplus.telegram.bots.datamodel;

import javax.annotation.Nullable;
import java.time.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SunsetSunriseTimes {
    private final Map<String, LocalDateTime> times = new HashMap<>();

    public @Nullable LocalDateTime getTime(String timeType) {
        return times.get(timeType);
    }

    public void putTime(String timeType, LocalDateTime time) {
        times.put(timeType, time);
    }
}
