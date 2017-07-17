package com.simpleplus.telegram.bots.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simpleplus.telegram.bots.Main;
import org.junit.Before;
import org.junit.Test;
import org.telegram.telegrambots.api.objects.Update;

import static org.junit.Assert.assertEquals;

public class CommandHandlerTest {
    private CommandHandler commandHandler;

    @Before
    public void init() {
        Main.initDefaultBotContext();
        commandHandler = (CommandHandler) BotContext.getDefaultContext().getBean("CommandHandler");
    }

    @Test
    public void getCommandWorks() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "{\"message\" : {\"text\" : \"/change-location\"}}";

        Update update = mapper.readValue(jsonInString, Update.class);
        assertEquals(CommandHandler.Command.REENTER_LOCATION, commandHandler.getCommand(update));
    }
}