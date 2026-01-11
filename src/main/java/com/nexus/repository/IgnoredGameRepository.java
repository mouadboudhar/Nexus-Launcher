package com.nexus.repository;

import com.nexus.model.IgnoredGame;
import com.nexus.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Repository for IgnoredGame entity CRUD operations using Hibernate.
 */
public class IgnoredGameRepository {

    /**
     * Save or update an ignored game.
     */
    public IgnoredGame save(IgnoredGame ignoredGame) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            if (ignoredGame.getId() == null) {
                session.persist(ignoredGame);
            } else {
                ignoredGame = session.merge(ignoredGame);
            }
            transaction.commit();
            return ignoredGame;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Find an ignored game by ID.
     */
    public Optional<IgnoredGame> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(IgnoredGame.class, id));
        }
    }

    /**
     * Get all ignored games.
     */
    public List<IgnoredGame> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM IgnoredGame ORDER BY title", IgnoredGame.class).list();
        }
    }

    /**
     * Find an ignored game by its unique ID.
     */
    public Optional<IgnoredGame> findByUniqueId(String uniqueId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM IgnoredGame WHERE uniqueId = :uid", IgnoredGame.class)
                    .setParameter("uid", uniqueId)
                    .uniqueResultOptional();
        }
    }

    /**
     * Find an ignored game by install path.
     */
    public Optional<IgnoredGame> findByInstallPath(String installPath) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM IgnoredGame WHERE installPath = :path", IgnoredGame.class)
                    .setParameter("path", installPath)
                    .uniqueResultOptional();
        }
    }

    /**
     * Check if a game is ignored by its unique ID.
     */
    public boolean isIgnored(String uniqueId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("SELECT COUNT(ig) FROM IgnoredGame ig WHERE ig.uniqueId = :uid", Long.class)
                    .setParameter("uid", uniqueId)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    /**
     * Get all ignored unique IDs.
     */
    public List<String> findAllUniqueIds() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT ig.uniqueId FROM IgnoredGame ig WHERE ig.uniqueId IS NOT NULL", String.class).list();
        }
    }

    /**
     * Delete an ignored game by ID.
     */
    public void delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            IgnoredGame ignoredGame = session.get(IgnoredGame.class, id);
            if (ignoredGame != null) {
                session.remove(ignoredGame);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Delete an ignored game entity.
     */
    public void delete(IgnoredGame ignoredGame) {
        if (ignoredGame != null && ignoredGame.getId() != null) {
            delete(ignoredGame.getId());
        }
    }

    /**
     * Delete an ignored game by unique ID.
     */
    public void deleteByUniqueId(String uniqueId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.createMutationQuery("DELETE FROM IgnoredGame WHERE uniqueId = :uid")
                    .setParameter("uid", uniqueId)
                    .executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Count all ignored games.
     */
    public long count() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT COUNT(ig) FROM IgnoredGame ig", Long.class).uniqueResult();
        }
    }
}

