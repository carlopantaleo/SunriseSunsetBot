package com.simpleplus.telegram.bots.datamodel;

public enum TimeType {
    SUNSET_TIME("The sun is rising."),
    SUNSET_TIME_ANTICIPATION("The sun is rising in %d minutes."),
    SUNRISE_TIME("Sunset has begun."),
    SUNRISE_TIME_ANTICIPATION("Sunset has begun %d minutes ago."),
    CIVIL_TWILIGHT_BEGIN_TIME("Civil twilight has begun."),
    CIVIL_TWILIGHT_BEGIN_TIME_ANTICIPATION("Civil twilight begins in %d minutes."),
    CIVIL_TWILIGHT_END_TIME("Civil twilight has ended."),
    CIVIL_TWILIGHT_END_TIME_ANTICIPATION("Civil twilight has ended %d minutes ago."),
    DEFAULT("Invalid.");

    private String message;

    TimeType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
