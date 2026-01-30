package org.example.datasource.repository;

import org.example.datasource.model.TaskEntity;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends CrudRepository<TaskEntity, Long> {

    @Override
    void save(TaskEntity entity);
    @Override
    Optional<TaskEntity> findById(Long id);
    @Override
    List<TaskEntity> findAll();
    @Override
    void deleteById(Long id);

    TaskEntity findByTitle(String title);
    List<TaskEntity> findByDescription(String description);
}
