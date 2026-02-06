package org.example.datasource.repositoryservice;

import org.example.datasource.model.TaskEntity;
import org.example.datasource.repository.BaseRepository;

import java.time.LocalDate;
import java.util.List;

public class TaskRepositoryService extends BaseRepository<TaskEntity> {

    public TaskRepositoryService() {
        super(TaskEntity.class);
    }

    public List<TaskEntity> findAllByDate(LocalDate date){
            return execute(session -> session.createQuery("FROM TaskEntity WHERE date = :date").setParameter("date", date).getResultList());
    }
}
