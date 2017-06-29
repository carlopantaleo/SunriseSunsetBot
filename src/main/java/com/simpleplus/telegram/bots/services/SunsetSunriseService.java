package com.simpleplus.telegram.bots.services;

import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.helpers.Coordinates;
import com.simpleplus.telegram.bots.helpers.SunsetSunriseTimes;

public interface SunsetSunriseService {
    SunsetSunriseTimes getSunsetSunriseTimes(Coordinates coordinates) throws ServiceException;
}
