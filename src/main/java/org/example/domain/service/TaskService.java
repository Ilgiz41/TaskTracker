package org.example.domain.service;

import org.example.datasource.mapper.TaskMapper;
import org.example.datasource.repository.TaskRepository;
import org.example.domain.model.Task;
import org.example.exceptions.ValidationException;
import org.example.util.TaskRepositoryUtil;

import java.time.LocalDate;

public class TaskService {

    private final TaskRepository taskRepositoryService;

    public TaskService() {
        this.taskRepositoryService = TaskRepositoryUtil.getTaskRepository();
    }

    public void createAndSave(String title, String description, LocalDate date) {
        if (title == null || title.isEmpty()) {
            throw new ValidationException("Title cannot be empty");
        }

        if (date == null) {
            throw new ValidationException("Date cannot be null");
        }

        Task task = new Task(title, description, date.atStartOfDay(), false);
        taskRepositoryService.save(TaskMapper.toEntity(task));
    }
}
