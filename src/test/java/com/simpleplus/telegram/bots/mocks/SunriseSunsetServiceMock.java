package com.simpleplus.telegram.bots.mocks;

import com.simpleplus.telegram.bots.components.BotBean;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;

import java.time.LocalDate;
import java.time.LocalTime;

public class SunriseSunsetServiceMock implements SunsetSunriseService, BotBean {
    @Override
    public SunsetSunriseTimes getSunsetSunriseTimes(Coordinates coordinates, LocalDate localDate) throws ServiceException {
        return new SunsetSunriseTimes(LocalTime.NOON, LocalTime.MIDNIGHT, null, null, null, null, null, null);
    }

    @Override
    public SunsetSunriseTimes getSunsetSunriseTimes(Coordinates coordinates) throws ServiceException {
        return new SunsetSunriseTimes(LocalTime.NOON, LocalTime.MIDNIGHT, null, null, null, null, null, null);
    }
}
