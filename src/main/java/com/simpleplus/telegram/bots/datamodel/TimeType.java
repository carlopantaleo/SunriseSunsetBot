package com.simpleplus.telegram.bots.datamodel;

public enum TimeType {
    SUNRISE_TIME("The sun is rising.", "Sunrise"),
    SUNRISE_TIME_ANTICIPATION("The sun is rising in %d minutes.", "Sunrise"),
    SUNSET_TIME("Sunset has begun.", "Sunset"),
    SUNSET_TIME_ANTICIPATION("Sunset has begun %d minutes ago.", "Sunset"),
    CIVIL_TWILIGHT_BEGIN_TIME("Civil twilight has begun.", "Civil twilight begin"),
    CIVIL_TWILIGHT_BEGIN_TIME_ANTICIPATION("Civil twilight begins in %d minutes.", "Civil twilight begin"),
    CIVIL_TWILIGHT_END_TIME("Civil twilight has ended.", "Civil twilight end"),
    CIVIL_TWILIGHT_END_TIME_ANTICIPATION("Civil twilight has ended %d minutes ago.", "Civil twilight end"),
    DEFAULT("Invalid.", "Invalid");

    private String message;
    private String readableName;

    TimeType(String message, String readableName) {
        this.message = message;
        this.readableName = readableName;
    }

    public String getMessage() {
        return message;
    }

    public String getReadableName() {
        return readableName;
    }
}
