package org.example.util;

import lombok.Getter;
import org.example.datasource.model.TaskEntity;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    @Getter
    private static final SessionFactory sessionFactory;

    static{
        try {
            Configuration configuration = new Configuration().configure();
            configuration.addAnnotatedClass(TaskEntity.class);
            sessionFactory = configuration.buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static void shutdown() {
        getSessionFactory().close();
    }

}
