package com.simpleplus.telegram.bots.datamodel;

public enum TimeType {
    SUNRISE_TIME("The sun is rising.", "Sunrise"),
    SUNRISE_TIME_ANTICIPATION("The sun is rising in %d minutes.", "Sunrise"),
    SUNSET_TIME("Sunset has begun.", "Sunset"),
    SUNSET_TIME_ANTICIPATION("Sunset begins in %d minutes.", "Sunset"),
    CIVIL_TWILIGHT_BEGIN_TIME("Civil twilight has begun.", "Civil twilight begin"),
    CIVIL_TWILIGHT_BEGIN_TIME_ANTICIPATION("Civil twilight begins in %d minutes.", "Civil twilight begin"),
    CIVIL_TWILIGHT_END_TIME("Civil twilight has ended.", "Civil twilight end"),
    CIVIL_TWILIGHT_END_TIME_ANTICIPATION("Civil twilight ends in %d minutes.", "Civil twilight end"),
    NAUTICAL_TWILIGHT_BEGIN_TIME("Nautical twilight has begun.", "Nautical twilight begin"),
    NAUTICAL_TWILIGHT_BEGIN_TIME_ANTICIPATION("Nautical twilight begins in %d minutes.", "Nautical twilight begin"),
    NAUTICAL_TWILIGHT_END_TIME("Nautical twilight has ended.", "Nautical twilight end"),
    NAUTICAL_TWILIGHT_END_TIME_ANTICIPATION("Nautical twilight ends in %d minutes.", "Nautical twilight end"),
    ASTRONOMICAL_TWILIGHT_BEGIN_TIME("Astronomical twilight has begun.", "Astronomical twilight begin"),
    ASTRONOMICAL_TWILIGHT_BEGIN_TIME_ANTICIPATION("Astronomical twilight begins in %d minutes.",
            "Astronomical twilight begin"),
    ASTRONOMICAL_TWILIGHT_END_TIME("Astronomical twilight has ended.", "Astronomical twilight end"),
    ASTRONOMICAL_TWILIGHT_END_TIME_ANTICIPATION("Astronomical twilight ends in %d minutes.",
            "Astronomical twilight end"),
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
