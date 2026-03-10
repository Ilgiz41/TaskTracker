package org.example.domain.service;

import org.example.domain.model.*;
import org.example.infrastructure.cache.Cache;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskCacheService {

    private final Cache<TaskId, Task> cache;

    public TaskCacheService(Cache<TaskId, Task> cache) {
        this.cache = cache;
    }

    public void loadCacheForDate(LocalDate date, DataPair<List<SimpleTask>, List<RegularTask>> data){
        if (!cache.isEmpty()) cache.clear();
        Set<Long> tasksTemplateId = data.first().stream().
                map(SimpleTask::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        data.first().forEach(cache::put);

        data.second().stream()
                .filter(regularTask -> !tasksTemplateId.contains(regularTask.getRawId()))
                .forEach(regularTask -> {
                    regularTask.setDate(date);
                    cache.put(regularTask);
                });
    }

    public void addIfCorrectDate(LocalDate date, SimpleTask task) {
        cache.compute(task.getId(), (taskId, existingTask) -> {
            if (date.equals(task.getDate())) {
                return task;
            }
            return null;
        });
    }

    public void addIfCorrectDayOfWeeks(LocalDate date, RegularTask task) {
        cache.compute(task.getId(), (taskId, existingTask) -> {
            if (task.getDayOfWeeks().contains(date.getDayOfWeek())) {
                task.setDate(date);
                return task;
            }
            return null;
        });
    }

    public void clearForDate(LocalDate date) {
        if (cache.isEmpty()) return;
        cache.getAll().stream()
                .filter(task -> task.getDate().equals(date) || task.getDayOfWeeks().contains(date.getDayOfWeek()))
                .forEach(cache::remove);
    }

    public List<Task> getAll() {
        return cache.getAll();
    }

    public void remove(Task task) {
        cache.remove(task);
    }

    public void put(Task task) {
        cache.put(task);
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }

}
