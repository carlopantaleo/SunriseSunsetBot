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

    @Before
    public void init() {
        MainTest.initDefaultBotContext();
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        userAlertsManager = (UserAlertsManager) BotContext.getDefaultContext().getBean(UserAlertsManager.class);
    }

    @Test
    public void noUserAlertsResultsInAddingDefaults() throws Exception {
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
    public void validateSyntaxWorks() throws Exception {
        assertTrue(userAlertsManager.validateSyntax("add sunset delay 5"));
        assertTrue(userAlertsManager.validateSyntax("add sunset delay 55"));
        assertFalse(userAlertsManager.validateSyntax("add sunset delay 555"));
        assertFalse(userAlertsManager.validateSyntax("add sunset delay"));
        assertFalse(userAlertsManager.validateSyntax("add civil twilight"));
        assertTrue(userAlertsManager.validateSyntax("add civil twilight begin"));
        assertTrue(userAlertsManager.validateSyntax("add civil twilight end"));
        assertTrue(userAlertsManager.validateSyntax("add civil twilight end delay -4"));
        assertTrue(userAlertsManager.validateSyntax("add civil twilight end delay -4"));
        assertTrue(userAlertsManager.validateSyntax("add"));
        assertTrue(userAlertsManager.validateSyntax("remove 5"));
        assertTrue(userAlertsManager.validateSyntax("remove"));
        assertFalse(userAlertsManager.validateSyntax("remove 5L"));
        assertTrue(userAlertsManager.validateSyntax("edit 5 sunset delay 7"));
    }
}