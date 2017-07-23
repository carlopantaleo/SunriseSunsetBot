package com.simpleplus.telegram.bots.components;

import org.junit.Test;

import static com.simpleplus.telegram.bots.components.BotScheduler.ScheduleResult.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScheduleResultTest {
    @Test
    public void inWorks() throws Exception {
        BotScheduler.ScheduleResult result = SCHEDULED;
        assertTrue(result.in(SCHEDULED));
        assertTrue(result.in(NOT_SCHEDULED, SCHEDULED));
        assertFalse(result.in(NOT_SCHEDULED));
        assertFalse(result.in(NOT_SCHEDULED, NOT_TO_SCHEDULE));
    }

}