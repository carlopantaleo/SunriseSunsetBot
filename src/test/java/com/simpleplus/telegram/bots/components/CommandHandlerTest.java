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
        commandHandler = (CommandHandler) BotContext.getDefaultContext().getBean(CommandHandler.class);
    }

    @Test
    public void getCommandWorks() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String jsonInString = "{\"message\" : {\"text\" : \"/change_location\"}}";
        Update update = mapper.readValue(jsonInString, Update.class);
        assertEquals(CommandHandler.Command.REENTER_LOCATION, commandHandler.getCommand(update));

        jsonInString = "{\"message\" : {\"text\" : \"/change_location args\"}}";
        update = mapper.readValue(jsonInString, Update.class);
        assertEquals(CommandHandler.Command.REENTER_LOCATION, commandHandler.getCommand(update));
    }

    @Test
    public void getCommandArgumentsWorks() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String jsonInString = "{\"message\" : {\"text\" : \"/change_location arg1\"}}";
        Update update = mapper.readValue(jsonInString, Update.class);
        assertEquals("arg1", commandHandler.getCommandArguments(update));

        jsonInString = "{\"message\" : {\"text\" : \"/change_location\"}}";
        update = mapper.readValue(jsonInString, Update.class);
        assertEquals("", commandHandler.getCommandArguments(update));

        jsonInString = "{\"message\" : {\"text\" : \"/change_location \"}}";
        update = mapper.readValue(jsonInString, Update.class);
        assertEquals("", commandHandler.getCommandArguments(update));

        jsonInString = "{\"message\" : {\"text\" : \"/change_location arg1 arg2\"}}";
        update = mapper.readValue(jsonInString, Update.class);
        assertEquals("arg1 arg2", commandHandler.getCommandArguments(update));
    }

}