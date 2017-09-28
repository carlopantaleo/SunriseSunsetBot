package com.simpleplus.telegram.bots.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simpleplus.telegram.bots.MainTest;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import com.simpleplus.telegram.bots.mocks.PersistenceManagerWithTestDB;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.telegram.telegrambots.api.objects.Update;

import static org.junit.Assert.assertEquals;

public class CommandHandlerTest {
    private CommandHandler commandHandler;
    private PersistenceManager persistenceManager;

    @Before
    public void init() {
        MainTest.initDefaultBotContext();
        commandHandler = (CommandHandler) BotContext.getDefaultContext().getBean(CommandHandler.class);
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
    }

    @After
    public void cleanup() {
        ((PersistenceManagerWithTestDB) persistenceManager).cleanup();
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

        jsonInString = "{\"message\" : {\"text\" : \"/change_location arg1 \\narg2\"}}";
        update = mapper.readValue(jsonInString, Update.class);
        assertEquals("arg1 \narg2", commandHandler.getCommandArguments(update));
    }

    @Test
    public void handleStopWorks() throws Exception {
        persistenceManager.setUserState(101L, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "{\"message\" : {\"text\" : \"/stop \", \"chat\" : {\"id\" : \"101\"}}}";
        Update update = mapper.readValue(jsonInString, Update.class);

        commandHandler.handleCommand(update);

        UserState userState = persistenceManager.getUserState(101L);
        assertEquals(Step.STOPPED, userState.getStep());
    }

    @Test
    public void handleResumeWorks() throws Exception {
        persistenceManager.setUserState(101L, new UserState(
                new Coordinates(0, 0),
                Step.STOPPED,
                false
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "{\"message\" : {\"text\" : \"/resume \", \"chat\" : {\"id\" : \"101\"}}}";
        Update update = mapper.readValue(jsonInString, Update.class);

        commandHandler.handleCommand(update);

        UserState userState = persistenceManager.getUserState(101L);
        assertEquals(Step.RUNNING, userState.getStep());
    }

    @Test
    public void handleNewChatWorks() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "{\"message\" : {\"text\" : \"/start \", \"chat\" : {\"id\" : \"101\"}}}";
        Update update = mapper.readValue(jsonInString, Update.class);

        commandHandler.handleCommand(update);

        UserState userState = persistenceManager.getUserState(101L);
        assertEquals(Step.TO_ENTER_LOCATION, userState.getStep());
    }

    @Test
    public void handleReenterLocationWorks() throws Exception {
        persistenceManager.setUserState(101L, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "{\"message\" : {\"text\" : \"/change_location \", \"chat\" : {\"id\" : \"101\"}}}";
        Update update = mapper.readValue(jsonInString, Update.class);

        commandHandler.handleCommand(update);

        UserState userState = persistenceManager.getUserState(101L);
        assertEquals(Step.TO_ENTER_LOCATION, userState.getStep());
    }
}