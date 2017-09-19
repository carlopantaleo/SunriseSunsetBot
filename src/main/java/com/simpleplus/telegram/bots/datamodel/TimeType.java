package com.simpleplus.telegram.bots.datamodel;

public enum TimeType {
    SUNSET_TIME("The sun is rising!"),
    SUNRISE_TIME("Sunset has begun.");

    private final String message;

    TimeType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
