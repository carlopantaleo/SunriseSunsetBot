package com.simpleplus.telegram.bots;


import com.simpleplus.telegram.bots.components.BotContext;
import com.simpleplus.telegram.bots.components.PersistenceManager;
import com.simpleplus.telegram.bots.components.PropertiesManager;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PersistenceManagerTest {
    private PersistenceManager persistenceManager;
    private PropertiesManager propertiesManager;

    @Before
    public void setup() {
        MainTest.initDefaultBotContext();

        propertiesManager = (PropertiesManager) BotContext.getDefaultContext().getBean(PropertiesManager.class);
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
    }

    @Test
    public void setAndGetWork() throws Exception {
        UserState userState = new UserState(new Coordinates(1.1F, 2.2F), Step.NEW_CHAT, false);
        persistenceManager.setUserState(99, userState);

        UserState gotUserState = persistenceManager.getUserState(99);
        assertTrue(gotUserState.equals(userState));

        userState.setStep(Step.RUNNING);
        persistenceManager.setUserState(99, userState);
        gotUserState = persistenceManager.getUserState(99);
        assertTrue(gotUserState.equals(userState));

        assertNull(persistenceManager.getUserState(1));
    }

    @Test
    public void getAllUserStatesWork() throws Exception {
        UserState userState = new UserState(new Coordinates(1.1F, 2.2F), Step.NEW_CHAT, false);
        persistenceManager.setUserState(99, userState);
        userState = new UserState(new Coordinates(1.2F, 2.3F), Step.RUNNING, false);
        persistenceManager.setUserState(98, userState);

        Map<Long, UserState> result = persistenceManager.getUserStatesMap();
        assertTrue(!result.isEmpty());
        assertTrue(result.size() != 1);
    }
}