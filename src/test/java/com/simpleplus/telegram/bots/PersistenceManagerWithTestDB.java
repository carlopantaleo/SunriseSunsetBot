package com.simpleplus.telegram.bots;

import com.simpleplus.telegram.bots.components.PersistenceManager;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

public class PersistenceManagerWithTestDB extends PersistenceManager {
    @Override
    protected EntityManager createEntityManager() {
        return Persistence.createEntityManagerFactory("h2-test").createEntityManager();
    }

}
