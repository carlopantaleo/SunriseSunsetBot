package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.Main;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

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

    @Test
    public void getSendMessageWorks() throws Exception {
        String broadcastMessage = adminCommandHandler.getSendMessage("send chatid=12345 send message.");
        assertEquals("send message.", broadcastMessage);

        broadcastMessage = adminCommandHandler.getSendMessage("send chatid=a12345 send message.");
        assertEquals("", broadcastMessage);
    }

    @Test
    public void getCommandOptionsWorks() throws Exception {
        String commandArgs = "chatid=12345 blabla broadcast=false test";

        Map<String, String> commandOptions = adminCommandHandler.getCommandOptions(commandArgs);
        assertEquals("12345", commandOptions.get("chatid"));
        assertEquals("false", commandOptions.get("broadcast"));
        assertEquals(2, commandOptions.size());
    }

}