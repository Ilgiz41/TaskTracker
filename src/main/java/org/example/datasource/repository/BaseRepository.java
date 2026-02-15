package org.example.datasource.repository;

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

    public BaseRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public T save(T entity, Session session) {
        session.merge(entity);
        return entity;
    }

    public List<T> findAll() {
        return execute(session -> session.createQuery("from " + entityClass.getName(), entityClass).list());
    }

    public Optional<T> findById(Long id) {
        return execute(session -> Optional.ofNullable(session.get(entityClass, id)));
    }

    public void deleteById(Long id, Session session) {
        session.remove(session.get(entityClass, id));
    }

    protected <R> R execute(Function<Session, R> function) {
        try (Session session = sessionFactory.openSession()) {
            return function.apply(session);
        }
    }
}
