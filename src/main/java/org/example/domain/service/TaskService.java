package org.example.domain.service;

import org.example.datasource.mapper.TaskMapper;
import org.example.datasource.repository.TaskRepository;
import org.example.domain.model.Task;
import org.example.exceptions.ValidationException;
import org.example.util.TaskRepositoryUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskService {

    private final TaskRepository taskRepositoryService;
    private Map<Long, Task> taskCache;

    public TaskService() {
        this.taskRepositoryService = TaskRepositoryUtil.getTaskRepository();
        this.taskCache = new ConcurrentHashMap<>();
    }

    public void createAndSave(String title, String description, LocalDate date) {
        if (title == null || title.isEmpty()) {
            throw new ValidationException("Title cannot be empty");
        }

        if (date == null) {
            throw new ValidationException("Date cannot be null");
        }

        Task task = new Task(title, description, date, false);
        taskRepositoryService.save(TaskMapper.toEntity(task));
    }

    public void loadCache() {
        List<Task> tasks = getTaskListFromRepository();

        deleteOld(tasks);
        for (Task task : tasks) {
            taskCache.put(task.getId(), task);
        }
    }

    private void deleteOld(List<Task> tasks) {
        List<Task> toDelete = tasks.stream()
                .filter(task -> task.getDate() != null && task.getDate().isBefore(LocalDate.now().minusDays(3)))
                .toList();

        tasks.removeAll(toDelete);
        if (!toDelete.isEmpty()) {
            deleteTaskListFromRepository(toDelete);
        }
    }

    private List<Task> getTaskListFromRepository(){
        List<Task> tasks = taskRepositoryService.findAll().stream()
                .map(TaskMapper::toDomain)
                .toList();
        return new ArrayList<>(tasks);
    }

    private void deleteTaskListFromRepository(List<Task> toDelete){
        toDelete.forEach(task -> {
           taskRepositoryService.deleteById(task.getId());
        });
    }
}

