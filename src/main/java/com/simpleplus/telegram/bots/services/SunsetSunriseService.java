package com.simpleplus.telegram.bots.services;

import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.helpers.Coordinates;
import com.simpleplus.telegram.bots.helpers.SunsetSunriseTimes;

import java.time.LocalDate;

public interface SunsetSunriseService {
    SunsetSunriseTimes getSunsetSunriseTimes(Coordinates coordinates, LocalDate localDate) throws ServiceException;
    SunsetSunriseTimes getSunsetSunriseTimes(Coordinates coordinates) throws ServiceException;
}
