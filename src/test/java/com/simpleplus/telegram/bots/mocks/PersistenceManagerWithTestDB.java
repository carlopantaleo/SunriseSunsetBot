package com.simpleplus.telegram.bots.mocks;

import com.simpleplus.telegram.bots.components.PersistenceManager;
import com.simpleplus.telegram.bots.datamodel.SavedChat;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class PersistenceManagerWithTestDB extends PersistenceManager {
    @Override
    protected EntityManager createEntityManager() {
        return Persistence.createEntityManagerFactory("h2-test").createEntityManager();
    }

    public void cleanup() {
        EntityManager em = createEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SavedChat> q = cb.createQuery(SavedChat.class);
        Root<SavedChat> c = q.from(SavedChat.class);
        q.select(c);
        TypedQuery<SavedChat> query = em.createQuery(q);
        List<SavedChat> results = query.getResultList();

        for (SavedChat savedChat : results) {
            em.remove(savedChat);
        }

        em.close();
    }
}
