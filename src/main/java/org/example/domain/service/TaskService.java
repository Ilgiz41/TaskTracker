package org.example.domain.service;

import lombok.Getter;
import org.example.datasource.mapper.TaskMapper;
import org.example.datasource.model.TaskEntity;
import org.example.datasource.repository.TaskRepository;
import org.example.domain.model.Task;
import org.example.exceptions.ValidationException;
import org.example.util.TaskRepositoryUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class TaskService {

    private final TaskRepository taskRepositoryService;
    @Getter
    private Map<Long, Task> taskCache;

    public TaskService() {
        this.taskRepositoryService = TaskRepositoryUtil.getTaskRepository();
        this.taskCache = new ConcurrentHashMap<>();
    }

    public void createAndSave(String title, String description, LocalDate date, LocalDate selectedDate, int priority) {
        if (title == null || title.isEmpty()) {
            throw new ValidationException("Title cannot be empty");
        }

        if (date == null) {
            throw new ValidationException("Date cannot be null");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new ValidationException("Date cannot be before current date");
        }

        Task task = new Task(title, description, date, false, priority);
        TaskEntity taskEntity = taskRepositoryService.save(TaskMapper.toEntity(task));
        if (selectedDate.equals(taskEntity.getDate())) {
            taskCache.put(taskEntity.getId(), TaskMapper.toDomain(taskEntity));
        }
    }

    public void updateTask(Task task, LocalDate selectedDate) {
        if (selectedDate.equals(task.getDate())) {
            taskCache.put(task.getId(), task);
        } else {
            taskCache.remove(task.getId());
        }
        taskRepositoryService.save(TaskMapper.toEntity(task));
    }

    public void deleteTask(Task task) {
        taskCache.remove(task.getId());
        taskRepositoryService.delete(TaskMapper.toEntity(task));
    }

    public void loadCacheByDate(LocalDate date) {
        List<Task> tasks = getTaskListFromRepository(date);
        if (!taskCache.isEmpty()) {
            taskCache.clear();
        }
        for (Task task : tasks) {
            taskCache.put(task.getId(), task);
        }
    }

    private List<Task> getTaskListFromRepository(LocalDate date) {
        List<Task> tasks = taskRepositoryService.findAllByDate(date).stream()
                .map(TaskMapper::toDomain)
                .toList();
        return new ArrayList<>(tasks);
    }

    private void deleteTaskListFromRepository(List<Task> toDelete) {
        toDelete.forEach(task -> {
            taskRepositoryService.deleteById(task.getId());
        });
    }

    public List<Task> getSortedTaskByPriority() {
        return taskCache.values().stream()
                .sorted(Comparator.comparing(Task::getPriority).reversed())
                .sorted(Comparator.comparing(Task::isCompleted))
                .toList();
    }

    public void deleteAllTasksForDate() {
        if (taskCache.isEmpty()) return;
        deleteTaskListFromRepository(taskCache.values().stream().toList());
        taskCache.clear();
    }

    public List<Task> dirtySearch(String query) {
        query = ".*" + String.join(".*", query.toLowerCase().split("")) + ".*";
        Pattern pattern = Pattern.compile(query);

        return taskCache.values().stream()
                .filter(task -> pattern.matcher(task.getTitle().toLowerCase()).matches())
                .toList();
    }
}
