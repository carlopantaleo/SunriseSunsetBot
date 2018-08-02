package com.simpleplus.telegram.bots;

import com.simpleplus.telegram.bots.components.*;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.mocks.PersistenceManagerWithTestDB;
import com.simpleplus.telegram.bots.mocks.SunriseSunsetBotMock;
import com.simpleplus.telegram.bots.mocks.SunriseSunsetServiceMock;
import com.simpleplus.telegram.bots.services.NamedLocationToCoordinatesService;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import com.simpleplus.telegram.bots.services.impl.GooglePlacesAPI;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class MainTest {
    public static void initDefaultBotContext() {
        BotContext context = new BotContext();
        BotContext.setDefaultContext(context);
        context.addBean(SunriseSunsetBot.class, new SunriseSunsetBotMock());
        context.addBean(SunsetSunriseService.class, new SunriseSunsetServiceMock());
        context.addBean(NamedLocationToCoordinatesService.class, getNamedLocationToCoordinatesMock());
        context.addBean(BotScheduler.class);
        context.addBean(Notifier.class);
        context.addBean(PersistenceManager.class, new PersistenceManagerWithTestDB());
        context.addBean(MessageHandler.class);
        context.addBean(CommandHandler.class);
        context.addBean(AdminCommandHandler.class);
        context.addBean(PropertiesManager.class);
        context.addBean(UserAlertsManager.class);
        context.initContext();
    }

    private static BotBean getNamedLocationToCoordinatesMock() {
        GooglePlacesAPI mock = spy(GooglePlacesAPI.class);
        try {
            doReturn("{\n" +
                            "  \"candidates\": [\n" +
                            "    {\n" +
                            "      \"formatted_address\": \"Turin, Metropolitan City of Turin, Italy\",\n" +
                            "      \"geometry\": {\n" +
                            "        \"location\": {\n" +
                            "          \"lat\": 45.0703393,\n" +
                            "          \"lng\": 7.686864\n" +
                            "        },\n" +
                            "        \"viewport\": {\n" +
                            "          \"northeast\": {\n" +
                            "            \"lat\": 45.1402009,\n" +
                            "            \"lng\": 7.7733629\n" +
                            "          },\n" +
                            "          \"southwest\": {\n" +
                            "            \"lat\": 45.00677659999999,\n" +
                            "            \"lng\": 7.5778502\n" +
                            "          }\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }\n" +
                            "  ],\n" +
                            "  \"debug_log\": {\n" +
                            "    \"line\": []\n" +
                            "  },\n" +
                            "  \"status\": \"OK\"\n" +
                            "}")
            .when(mock).callRemoteService(anyString());
        } catch (ServiceException e) {
            // Not reached
        }
        return mock;
    }
}