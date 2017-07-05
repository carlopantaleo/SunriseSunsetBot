package com.simpleplus.telegram.bots;


import com.simpleplus.telegram.bots.helpers.Coordinates;
import com.simpleplus.telegram.bots.helpers.Step;
import com.simpleplus.telegram.bots.helpers.UserState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PersistenceManagerTest {
    private static final String TEST_DB = "test.db";
    private PersistenceManager persistenceManager;

    @Before
    @After
    public void init() {
        File file = new File(TEST_DB);
        if (file.exists()) {
            file.delete();
        }

        if (persistenceManager == null) {
            persistenceManager = new PersistenceManager(TEST_DB);
        }
    }

    @Test
    public void setAndGetWork() throws Exception {
        UserState userState = new UserState(new Coordinates(1.1F, 2.2F), Step.NEW_CHAT);
        persistenceManager.setUserState(99, userState);

        UserState gotUserState = persistenceManager.getUserState(99);
        assertTrue(gotUserState.equals(userState));

        userState.setStep(Step.RUNNING);
        persistenceManager.setUserState(99, userState);
        gotUserState = persistenceManager.getUserState(99);
        assertTrue(gotUserState.equals(userState));

        assertNull(persistenceManager.getUserState(1));
    }
}