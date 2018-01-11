package com.simpleplus.telegram.bots.datamodel;

public enum TimeType {
    SUNRISE_TIME("The sun is rising.", "Sunrise"),
    SUNRISE_TIME_ANTICIPATION("The sun is rising in %d minutes.", "Sunrise"),
    SUNSET_TIME("Sunset has begun.", "Sunset"),
    SUNSET_TIME_ANTICIPATION("Sunset begins in %d minutes.", "Sunset"),
    CIVIL_TWILIGHT_BEGIN_TIME("Civil twilight has begun.", "Begin of civil twilight"),
    CIVIL_TWILIGHT_BEGIN_TIME_ANTICIPATION("Civil twilight begins in %d minutes.", "Begin of civil twilight"),
    CIVIL_TWILIGHT_END_TIME("Civil twilight has ended.", "End of civil twilight"),
    CIVIL_TWILIGHT_END_TIME_ANTICIPATION("Civil twilight ends in %d minutes.", "End of civil twilight"),
    NAUTICAL_TWILIGHT_BEGIN_TIME("Nautical twilight has begun.", "Begin of nautical twilight"),
    NAUTICAL_TWILIGHT_BEGIN_TIME_ANTICIPATION("Nautical twilight begins in %d minutes.", "Begin of nautical twilight"),
    NAUTICAL_TWILIGHT_END_TIME("Nautical twilight has ended.", "End of nautical twilight"),
    NAUTICAL_TWILIGHT_END_TIME_ANTICIPATION("Nautical twilight ends in %d minutes.", "End of nautical twilight"),
    ASTRONOMICAL_TWILIGHT_BEGIN_TIME("Astronomical twilight has begun.", "Begin of astronomical twilight"),
    ASTRONOMICAL_TWILIGHT_BEGIN_TIME_ANTICIPATION("Astronomical twilight begins in %d minutes.",
            "Begin of astronomical twilight"),
    ASTRONOMICAL_TWILIGHT_END_TIME("Astronomical twilight has ended.", "End of astronomical twilight"),
    ASTRONOMICAL_TWILIGHT_END_TIME_ANTICIPATION("Astronomical twilight ends in %d minutes.",
            "End of astronomical twilight"),
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
