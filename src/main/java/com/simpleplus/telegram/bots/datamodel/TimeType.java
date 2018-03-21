package com.simpleplus.telegram.bots.datamodel;

public enum TimeType {
    SUNRISE_TIME(
            "The sun is rising.",
            "Sunrise",
            "sunrise"
    ),
    SUNRISE_TIME_ANTICIPATION(
            "The sun is rising in %d minutes.",
            "Sunrise",
            "sunrise"
    ),
    SUNSET_TIME(
            "Sunset has begun.",
            "Sunset",
            "sunset"
    ),
    SUNSET_TIME_ANTICIPATION(
            "Sunset begins in %d minutes.",
            "Sunset",
            "sunset"
    ),
    CIVIL_TWILIGHT_BEGIN_TIME(
            "Civil twilight has begun.",
            "Begin of civil twilight",
            "dawn"
    ),
    CIVIL_TWILIGHT_BEGIN_TIME_ANTICIPATION(
            "Civil twilight begins in %d minutes.",
            "Begin of civil twilight",
            "dawn"
    ),
    CIVIL_TWILIGHT_END_TIME(
            "Civil twilight has ended.",
            "End of civil twilight",
            "dusk"
    ),
    CIVIL_TWILIGHT_END_TIME_ANTICIPATION(
            "Civil twilight ends in %d minutes.",
            "End of civil twilight",
            "dusk"
    ),
    NAUTICAL_TWILIGHT_BEGIN_TIME(
            "Nautical twilight has begun.",
            "Begin of nautical twilight",
            "nauticalDawn"
    ),
    NAUTICAL_TWILIGHT_BEGIN_TIME_ANTICIPATION(
            "Nautical twilight begins in %d minutes.",
            "Begin of nautical twilight",
            "nauticalDawn"
    ),
    NAUTICAL_TWILIGHT_END_TIME(
            "Nautical twilight has ended.",
            "End of nautical twilight",
            "nauticalDusk"
    ),
    NAUTICAL_TWILIGHT_END_TIME_ANTICIPATION(
            "Nautical twilight ends in %d minutes.",
            "End of nautical twilight",
            "nauticalDusk"
    ),
    ASTRONOMICAL_TWILIGHT_BEGIN_TIME(
            "Astronomical twilight has begun.",
            "Begin of astronomical twilight",
            "night"
    ),
    ASTRONOMICAL_TWILIGHT_BEGIN_TIME_ANTICIPATION(
            "Astronomical twilight begins in %d minutes.",
            "Begin of astronomical twilight",
            "night"
    ),
    ASTRONOMICAL_TWILIGHT_END_TIME(
            "Astronomical twilight has ended.",
            "End of astronomical twilight",
            "nightEnd"
    ),
    ASTRONOMICAL_TWILIGHT_END_TIME_ANTICIPATION(
            "Astronomical twilight ends in %d minutes.",
            "End of astronomical twilight",
            "nightEnd"
    ),
    DEFAULT("Invalid.", "Invalid", "invalid");

    private String message;
    private String readableName;
    private String internalName;

    TimeType(String message, String readableName, String internalName) {
        this.message = message;
        this.readableName = readableName;
        this.internalName = internalName;
    }

    public String getMessage() {
        return message;
    }

    public String getReadableName() {
        return readableName;
    }

    public String getInternalName() {
        return internalName;
    }
}
