package com.simpleplus.telegram.bots.components;

import com.google.common.collect.Sets;
import com.simpleplus.telegram.bots.MainTest;
import com.simpleplus.telegram.bots.datamodel.*;
import com.simpleplus.telegram.bots.mocks.PersistenceManagerWithTestDB;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NotifierTest {
    private Notifier notifier;
    private PersistenceManager persistenceManager;

    @Before
    public void init() {
        MainTest.initDefaultBotContext();
        notifier = (Notifier) BotContext.getDefaultContext().getBean(Notifier.class);
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
    }

    @After
    public void cleanup() {
        ((PersistenceManagerWithTestDB) persistenceManager).cleanup();
    }

    @Test
    public void installDefaultNotifiers() throws Exception {
        persistenceManager.setUserState(101L,
                new UserState(new Coordinates(0, 0), Step.RUNNING, false));

        notifier.tryToInstallNotifiers(101L, 5);

        Set<UserAlert> userAlerts = persistenceManager.getUserAlerts(101L);
        assertEquals(2, userAlerts.size());

        UserAlert[] userAlertsArr = userAlerts.toArray(new UserAlert[2]);
        Set<TimeType> expectedTimes = Sets.newHashSet(TimeType.SUNRISE, TimeType.SUNSET);
        Set<TimeType> savedTimes = Sets.newHashSet(userAlertsArr[0].getTimeType(), userAlertsArr[1].getTimeType());
        assertEquals(101L, userAlertsArr[0].getChatId());
        assertEquals(101L, userAlertsArr[1].getChatId());
        assertEquals(0, userAlertsArr[0].getDelay());
        assertEquals(0, userAlertsArr[1].getDelay());
        assertTrue(expectedTimes.containsAll(savedTimes));
    }
}