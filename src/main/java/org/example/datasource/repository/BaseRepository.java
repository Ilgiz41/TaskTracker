package org.example.datasource.repository;

import org.example.event.Event;
import org.example.util.DomainServiceUtil;
import org.example.util.EventBus;
import org.example.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class BaseRepository<T> {

    private static final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    Class<T> entityClass;
    EventBus eventBus = DomainServiceUtil.getEventBus();

    public BaseRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public T save(T entity) {
        executeInTransaction(session -> session.merge(entity));
        return entity;
    }

    public List<T> findAll() {
        return execute(session -> session.createQuery("from " + entityClass.getName(), entityClass).list());
    }

    public Optional<T> findById(Long id) {
        return execute(session -> Optional.ofNullable(session.get(entityClass, id)));
    }

    public void deleteById(Long id) {
        executeInTransaction(session -> {
            session.remove(session.get(entityClass, id));
        });
    }

    protected <R> R execute(Function<Session, R> function) {
        try (Session session = sessionFactory.openSession()) {
            return function.apply(session);
        }
    }

    public void executeInTransaction(Consumer<Session> consumer) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            try {
                transaction = session.beginTransaction();
                consumer.accept(session);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                eventBus.publish(new Event.CriticalErrorExceptionEvent(e));
            }
        }
    }
}
