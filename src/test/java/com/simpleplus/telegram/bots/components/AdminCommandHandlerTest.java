package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.Main;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AdminCommandHandlerTest {
    private AdminCommandHandler adminCommandHandler;

    @Before
    public void init() {
        Main.initDefaultBotContext();
        adminCommandHandler = (AdminCommandHandler) BotContext.getDefaultContext().getBean(AdminCommandHandler.class);
    }

    @Test
    public void getBroadcastMessageWorks() throws Exception {
        String broadcastMessage = adminCommandHandler.getBroadcastMessage("broadcast bla bla bla.");
        assertEquals("bla bla bla.", broadcastMessage);

        broadcastMessage = adminCommandHandler.getBroadcastMessage("podcast bla bla bla.");
        assertEquals("", broadcastMessage);

    }

}