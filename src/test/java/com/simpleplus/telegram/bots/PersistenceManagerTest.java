package com.simpleplus.telegram.bots;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class PersistenceManagerTest {
    private static final String TEST_DB = "test.db";

    @Before
    @After
    public void init() {
        File file = new File(TEST_DB);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void databaseIsCreatedIfNotExists() throws Exception {
        PersistenceManager persistenceManager = new PersistenceManager(TEST_DB);
    }
}