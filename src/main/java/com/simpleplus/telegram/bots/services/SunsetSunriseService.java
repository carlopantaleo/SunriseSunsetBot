package com.simpleplus.telegram.bots.services;

import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.exceptions.ServiceException;

import java.time.LocalDate;

public interface SunsetSunriseService {
    SunsetSunriseTimes getSunsetSunriseTimes(Coordinates coordinates, LocalDate localDate) throws ServiceException;
    SunsetSunriseTimes getSunsetSunriseTimes(Coordinates coordinates) throws ServiceException;
}
