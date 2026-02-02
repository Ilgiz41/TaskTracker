package org.example.util;

import lombok.Getter;
import org.example.datasource.repository.TaskRepository;
import org.example.datasource.repositoryservice.TaskRepositoryService;

public class TaskRepositoryUtil {
    @Getter
    private static final TaskRepository taskRepository;

    static {
        taskRepository = new TaskRepositoryService();
    }

}
