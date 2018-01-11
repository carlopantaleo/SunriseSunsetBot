package com.simpleplus.telegram.bots.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simpleplus.telegram.bots.MainTest;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import com.simpleplus.telegram.bots.mocks.PersistenceManagerWithTestDB;
import com.simpleplus.telegram.bots.mocks.SunriseSunsetBotMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;

import java.util.List;

import static org.junit.Assert.*;

public class CommandHandlerTest {
    private CommandHandler commandHandler;
    private PersistenceManager persistenceManager;
    private SunriseSunsetBotMock sunriseSunsetBot;
    private MessageHandler messageHandler;

    @Before
    public void init() {
        MainTest.initDefaultBotContext();
        commandHandler = (CommandHandler) BotContext.getDefaultContext().getBean(CommandHandler.class);
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        sunriseSunsetBot = (SunriseSunsetBotMock) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        messageHandler = (MessageHandler) BotContext.getDefaultContext().getBean(MessageHandler.class);
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

        String jsonInString = "{\"message\" : {\"text\" : \"/stop \", \"chat\" : {\"id\" : \"101\"}}}";
        Update update = new ObjectMapper().readValue(jsonInString, Update.class);

        sunriseSunsetBot.onUpdateReceived(update);

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

        String jsonInString = "{\"message\" : {\"text\" : \"/resume \", \"chat\" : {\"id\" : \"101\"}}}";
        Update update = new ObjectMapper().readValue(jsonInString, Update.class);

        sunriseSunsetBot.onUpdateReceived(update);

        UserState userState = persistenceManager.getUserState(101L);
        assertEquals(Step.RUNNING, userState.getStep());
    }

    @Test
    public void handleNewChatWorks() throws Exception {
        String jsonInString = "{\"message\" : {\"text\" : \"/start \", \"chat\" : {\"id\" : \"101\"}}}";
        Update update = new ObjectMapper().readValue(jsonInString, Update.class);

        sunriseSunsetBot.onUpdateReceived(update);

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

        String jsonInString = "{\"message\" : {\"text\" : \"/change_location \", \"chat\" : {\"id\" : \"101\"}}}";
        Update update = new ObjectMapper().readValue(jsonInString, Update.class);

        sunriseSunsetBot.onUpdateReceived(update);

        UserState userState = persistenceManager.getUserState(101L);
        assertEquals(Step.TO_ENTER_LOCATION, userState.getStep());
    }

    @Test
    public void setAdministratorWithInvalidToken() throws Exception {
        persistenceManager.setUserState(101L, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        String jsonInString = "{\"message\" : {\"text\" : \"/set_administrator invalidToken \", " +
                "\"chat\" : {\"id\" : \"101\"}}}";
        Update update = new ObjectMapper().readValue(jsonInString, Update.class);

        sunriseSunsetBot.onUpdateReceived(update);

        UserState userState = persistenceManager.getUserState(101L);
        assertFalse(userState.isAdmin());
    }

    @Test
    public void setAdministratorWithValidToken() throws Exception {
        persistenceManager.setUserState(101L, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        String jsonInString = "{\"message\" : {\"text\" : \"/set_administrator DummyToken\", " +
                "\"chat\" : {\"id\" : \"101\"}}}";
        Update update = new ObjectMapper().readValue(jsonInString, Update.class);

        sunriseSunsetBot.onUpdateReceived(update);

        UserState userState = persistenceManager.getUserState(101L);
        assertTrue(userState.isAdmin());
    }

    @Test
    public void supportMessageSetsCorrectMode() throws Exception {
        persistenceManager.setUserState(101L, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        String jsonInString = "{\"message\" : {\"text\" : \"/support\", " +
                "\"chat\" : {\"id\" : \"101\"}}}";
        Update update = new ObjectMapper().readValue(jsonInString, Update.class);

        sunriseSunsetBot.onUpdateReceived(update);

        UserState userState = persistenceManager.getUserState(101L);
        assertEquals(Step.TO_ENTER_SUPPORT_MESSAGE, userState.getStep());
    }

    @Test
    public void supportMessageIsSent1() throws Exception {
        persistenceManager.setUserState(101L, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        // Add and admin chat, so that the support message is sent to an admin
        persistenceManager.setUserState(102L, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                true
        ));

        ObjectMapper mapper = new ObjectMapper();

        String jsonInString = "{\"message\" : {\"text\" : \"/support\", " +
                "\"chat\" : {\"id\" : \"101\"}}}";
        Update update = mapper.readValue(jsonInString, Update.class);
        sunriseSunsetBot.onUpdateReceived(update);

        jsonInString = "{\"message\" : {\"text\" : \"test\", " +
                "\"chat\" : {\"id\" : \"101\"}}}";
        update = mapper.readValue(jsonInString, Update.class);
        sunriseSunsetBot.onUpdateReceived(update);

        UserState userState = persistenceManager.getUserState(101L);
        assertEquals(Step.RUNNING, userState.getStep());

        List<String> sentMessages = sunriseSunsetBot.getSentMessages();
        assertEquals(3, sentMessages.size());
        assertTrue(sentMessages.get(0).contains("Ok, send me your message for support"));
        assertTrue(sentMessages.get(1).contains("Support request from chatId"));
        assertTrue(sentMessages.get(2).contains("Message to support sent."));
    }

    @Test
    public void supportMessageIsSent2() throws Exception {
        persistenceManager.setUserState(101L, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        // Add and admin chat, so that the support message is sent to an admin
        persistenceManager.setUserState(102L, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                true
        ));

        String jsonInString = "{\"message\" : {\"text\" : \"/support help me\", " +
                "\"chat\" : {\"id\" : \"101\"}}}";
        Update update = new ObjectMapper().readValue(jsonInString, Update.class);
        sunriseSunsetBot.onUpdateReceived(update);

        UserState userState = persistenceManager.getUserState(101L);
        assertEquals(Step.RUNNING, userState.getStep());

        List<String> sentMessages = sunriseSunsetBot.getSentMessages();
        assertEquals(2, sentMessages.size());
        assertTrue(sentMessages.get(0).contains("Support request from chatId"));
        assertTrue(sentMessages.get(1).contains("Message to support sent."));
    }
}