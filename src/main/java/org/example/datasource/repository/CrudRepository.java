package org.example.datasource.repository;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {

    void save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void delete(T entity);
    void deleteById(ID id);

}
