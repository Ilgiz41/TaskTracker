package org.example.datasource.repositoryservice;

import org.example.datasource.model.TaskEntity;
import org.example.datasource.repository.BaseRepository;
import org.example.util.EventBus;

import java.time.LocalDate;
import java.util.List;

public class TaskRepositoryService extends BaseRepository<TaskEntity> {

    public TaskRepositoryService(EventBus eventBus) {
        super(TaskEntity.class, eventBus);
    }

    public List<TaskEntity> findAllByDate(LocalDate date) {
        return execute(session -> session.createQuery("FROM TaskEntity WHERE date = :date").setParameter("date", date).getResultList());
    }

    public void deleteAllByDate(LocalDate date) {
        executeInTransaction(session -> {
            session.createMutationQuery("DELETE FROM TaskEntity WHERE date = :date")
                    .setParameter("date", date)
                    .executeUpdate();
        });
    }
}
