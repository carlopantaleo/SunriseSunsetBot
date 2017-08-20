package com.simpleplus.telegram.bots.datamodel;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum Step {
    NEW_CHAT,
    TO_ENTER_LOCATION,
    TO_REENTER_LOCATION,
    TO_ENTER_SUPPORT_MESSAGE,
    RUNNING,
    EXPIRED;

    public boolean in(Step... steps) {
        return !Arrays.stream(steps)
                .filter(this::equals)
                .collect(Collectors.toList())
                .isEmpty();
    }
}
