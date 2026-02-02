package org.example.datasource.repository;

import org.example.datasource.model.TaskEntity;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {

    TaskEntity save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void delete(T entity);
    void deleteById(ID id);

}
