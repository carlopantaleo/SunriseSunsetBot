package com.simpleplus.telegram.bots.datamodel;

public enum TimeType {
    SUNRISE(
            "The sun is rising.",
            "Sunrise",
            "sunrise"
    ),
    SUNRISE_ANTICIPATION(
            "The sun is rising in %d minutes.",
            "Sunrise",
            "sunrise"
    ),
    SUNSET(
            "The sun is setting.",
            "Sunset",
            "sunset"
    ),
    SUNSET_ANTICIPATION(
            "The sun is setting in %d minutes.",
            "Sunset",
            "sunset"
    ),
    CIVIL_TWILIGHT_BEGIN(
            "Civil twilight has begun.",
            "Begin of civil twilight",
            "dawn"
    ),
    CIVIL_TWILIGHT_BEGIN_ANTICIPATION(
            "Civil twilight begins in %d minutes.",
            "Begin of civil twilight",
            "dawn"
    ),
    CIVIL_TWILIGHT_END(
            "Civil twilight has ended.",
            "End of civil twilight",
            "dusk"
    ),
    CIVIL_TWILIGHT_END_ANTICIPATION(
            "Civil twilight ends in %d minutes.",
            "End of civil twilight",
            "dusk"
    ),
    NAUTICAL_TWILIGHT_BEGIN(
            "Nautical twilight has begun.",
            "Begin of nautical twilight",
            "nauticalDawn"
    ),
    NAUTICAL_TWILIGHT_BEGIN_ANTICIPATION(
            "Nautical twilight begins in %d minutes.",
            "Begin of nautical twilight",
            "nauticalDawn"
    ),
    NAUTICAL_TWILIGHT_END(
            "Nautical twilight has ended.",
            "End of nautical twilight",
            "nauticalDusk"
    ),
    NAUTICAL_TWILIGHT_END_ANTICIPATION(
            "Nautical twilight ends in %d minutes.",
            "End of nautical twilight",
            "nauticalDusk"
    ),
    ASTRONOMICAL_TWILIGHT_BEGIN(
            "Astronomical twilight has begun.",
            "Begin of astronomical twilight",
            "nightEnd"
    ),
    ASTRONOMICAL_TWILIGHT_BEGIN_ANTICIPATION(
            "Astronomical twilight begins in %d minutes.",
            "Begin of astronomical twilight",
            "nightEnd"
    ),
    ASTRONOMICAL_TWILIGHT_END(
            "Astronomical twilight has ended.",
            "End of astronomical twilight",
            "night"
    ),
    ASTRONOMICAL_TWILIGHT_END_ANTICIPATION(
            "Astronomical twilight ends in %d minutes.",
            "End of astronomical twilight",
            "night"
    ),
    GOLDEN_HOUR_BEGIN(
            "Golden hour has begun.",
            "Begin of golden hour",
            "goldenHour"
    ),
    GOLDEN_HOUR_BEGIN_ANTICIPATION(
            "Golden hour begins in %d minutes.",
            "Begin of golden hour",
            "goldenHour"
    ),
    GOLDEN_HOUR_END(
            "Golden hour has ended.",
            "End of golden hour",
            "goldenHourEnd"
    ),
    GOLDEN_HOUR_END_ANTICIPATION(
            "Golden hour ends in %d minutes.",
            "End of golden hour",
            "goldenHourEnd"
    ),
    MOONRISE(
            "The moon is rising.",
            "Moonrise",
            "moonRise"
    ),
    MOONRISE_ANTICIPATION(
            "The moon is rising in %d minutes.",
            "Moonrise",
            "moonRise"
    ),
    MOONSET(
            "The moon is setting.",
            "Moonset",
            "moonSet"
    ),
    MOONSET_ANTICIPATION(
            "The moon is setting in %d minutes.",
            "Moonset",
            "moonSet"
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
