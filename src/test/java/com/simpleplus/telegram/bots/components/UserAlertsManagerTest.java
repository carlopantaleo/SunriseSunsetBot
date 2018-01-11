package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.MainTest;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserAlert;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class UserAlertsManagerTest {
    private PersistenceManager persistenceManager;
    private UserAlertsManager userAlertsManager;
    private CommandHandler commandHandler;


    @Before
    public void init() {
        MainTest.initDefaultBotContext();
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        userAlertsManager = (UserAlertsManager) BotContext.getDefaultContext().getBean(UserAlertsManager.class);
        commandHandler = (CommandHandler) BotContext.getDefaultContext().getBean(CommandHandler.class);
    }

    @Test
    public void noUserAlertsResultsInAddingDefaults() {
        long testChatId = 101L;
        persistenceManager.setUserState(testChatId, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        Set<UserAlert> userAlerts = userAlertsManager.getUserAlerts(testChatId);
        assertEquals(2, userAlerts.size());
    }

    @Test
    public void validateSyntaxWorks() {
        assertTrue(userAlertsManager.validateSyntax("add sunset delay 5"));
        assertTrue(userAlertsManager.validateSyntax("add sunset delay 55"));
        assertFalse(userAlertsManager.validateSyntax("add sunset delay 555"));
        assertFalse(userAlertsManager.validateSyntax("add sunset delay"));
        assertFalse(userAlertsManager.validateSyntax("add civil twilight"));
        assertTrue(userAlertsManager.validateSyntax("add civil twilight begin"));
        assertTrue(userAlertsManager.validateSyntax("add civil twilight end"));
        assertTrue(userAlertsManager.validateSyntax("add civil twilight end delay -4"));
        assertTrue(userAlertsManager.validateSyntax("add civil twilight end delay -4"));
        assertTrue(userAlertsManager.validateSyntax("add civil twilight end delay null"));
        assertTrue(userAlertsManager.validateSyntax("add nautical twilight end delay null"));
        assertTrue(userAlertsManager.validateSyntax("add astronomical twilight end delay null"));
        assertTrue(userAlertsManager.validateSyntax("add"));
        assertTrue(userAlertsManager.validateSyntax("remove 5"));
        assertTrue(userAlertsManager.validateSyntax("remove"));
        assertFalse(userAlertsManager.validateSyntax("remove 5L"));
        assertTrue(userAlertsManager.validateSyntax("edit 5 sunset delay 7"));
    }

    @Test
    public void dontInsertDuplicateAlerts1() {
        long testChatId = 102L;
        persistenceManager.setUserState(testChatId, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        userAlertsManager.handleCommand(testChatId, "add sunrise delay null", 1L);
        userAlertsManager.handleCommand(testChatId, "add sunrise delay null", 1L);

        Set<UserAlert> userAlerts = persistenceManager.getUserAlerts(testChatId);
        assertEquals(1, userAlerts.size());
    }

    @Test
    public void dontInsertDuplicateAlerts2() {
        long testChatId = 103L;
        persistenceManager.setUserState(testChatId, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        userAlertsManager.handleCommand(testChatId, "add sunrise delay null", 1L);

        // Retrieve max id -- i.e. id of the only inserted user alert for this chatId
        long maxUserAlert = 0;
        for (UserAlert userAlert : persistenceManager.getUserAlerts(testChatId)) {
            maxUserAlert = Math.max(userAlert.getId(), maxUserAlert);
        }

        userAlertsManager.handleCommand(testChatId, "edit " + (maxUserAlert) + " delay 0", 1L);
        Set<UserAlert> userAlerts = persistenceManager.getUserAlerts(testChatId);
        assertEquals(1, userAlerts.size());

        userAlertsManager.handleCommand(testChatId, "add sunrise delay null", 1L);
        userAlerts = persistenceManager.getUserAlerts(testChatId);
        assertEquals(2, userAlerts.size());

        userAlertsManager.handleCommand(testChatId, "edit " + (maxUserAlert + 1) + " delay 0", 1L);
        userAlerts = persistenceManager.getUserAlerts(testChatId);
        assertEquals(1, userAlerts.size());
    }
}