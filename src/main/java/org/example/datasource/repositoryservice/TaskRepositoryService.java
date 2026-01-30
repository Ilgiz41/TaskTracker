package org.example.datasource.repositoryservice;

import org.example.datasource.model.TaskEntity;
import org.example.datasource.repository.TaskRepository;
import org.example.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class TaskRepositoryService implements TaskRepository {
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    @Override
    public void save(TaskEntity entity) {
        executeInTransaction(session -> session.merge(entity));
    }

    @Override
    public Optional<TaskEntity> findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return Optional.ofNullable(session.get(TaskEntity.class, id));
        }
    }

    @Override
    public List<TaskEntity> findAll() {
        try (Session session = sessionFactory.openSession()) {
        return session.createQuery("from TaskEntity").list();
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            session.remove(session.get(TaskEntity.class, id));
        }
    }

    @Override
    public TaskEntity findByTitle(String title) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(TaskEntity.class, title);
        }
    }

    @Override
    public List<TaskEntity> findByDescription(String description) {
        return List.of();
    }

    @Override
    public void delete(TaskEntity entity) {
        try (Session session = sessionFactory.openSession()) {
            session.remove(entity);
        }
    }

    private void executeInTransaction(Consumer<Session> action) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            action.accept(session);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new  RuntimeException("Ошибка бд" + e.getMessage(), e);
        }
    }
}
