package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.MainTest;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserAlert;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

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

}