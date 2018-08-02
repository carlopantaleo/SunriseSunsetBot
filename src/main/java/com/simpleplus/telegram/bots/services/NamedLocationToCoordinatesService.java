package com.simpleplus.telegram.bots.services;

import com.simpleplus.telegram.bots.components.BotBean;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.exceptions.ServiceException;

public interface NamedLocationToCoordinatesService extends BotBean {
    Coordinates findCoordinates(String namedLocation) throws ServiceException;
}
