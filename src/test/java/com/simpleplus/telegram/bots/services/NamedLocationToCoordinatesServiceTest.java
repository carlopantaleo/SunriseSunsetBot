package com.simpleplus.telegram.bots.services;

import com.google.common.collect.Range;
import com.simpleplus.telegram.bots.MainTest;
import com.simpleplus.telegram.bots.components.BotContext;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NamedLocationToCoordinatesServiceTest {
    private NamedLocationToCoordinatesService locationToCoordinatesService;

    @Before
    public void init() {
        MainTest.initDefaultBotContext();
        locationToCoordinatesService = (NamedLocationToCoordinatesService) BotContext.getDefaultContext()
                .getBean(NamedLocationToCoordinatesService.class);
    }

    @Test
    public void responseIsParsedCorrectly() throws ServiceException {
        Coordinates coords = locationToCoordinatesService.findCoordinates("dummy");
        assertNotNull(coords);
        assertTrue(Range.closed(45.0703F, 45.0704F).contains(coords.getLatitude()));
        assertTrue(Range.closed(7.6868F, 7.6869F).contains(coords.getLongitude()));
        assertEquals("Turin, Metropolitan City of Turin, Italy", coords.getDescription());
    }
}