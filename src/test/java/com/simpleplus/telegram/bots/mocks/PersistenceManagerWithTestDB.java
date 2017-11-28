package com.simpleplus.telegram.bots.mocks;

import com.simpleplus.telegram.bots.components.PersistenceManager;
import com.simpleplus.telegram.bots.datamodel.SavedChat;
import org.apache.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class PersistenceManagerWithTestDB extends PersistenceManager {
    private static final Logger LOG = Logger.getLogger(PersistenceManagerWithTestDB.class);

    @Override
    protected EntityManager createEntityManager() {
        return Persistence.createEntityManagerFactory("h2-test").createEntityManager();
    }

    public void cleanup() {
        LOG.info("Going to cleanup test database.");

        EntityManager em = createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SavedChat> q = cb.createQuery(SavedChat.class);
        Root<SavedChat> c = q.from(SavedChat.class);
        q.select(c);
        TypedQuery<SavedChat> query = em.createQuery(q);
        List<SavedChat> results = query.getResultList();

        for (SavedChat savedChat : results) {
            em.remove(savedChat);
        }

        transaction.commit();
        em.close();
    }
}
