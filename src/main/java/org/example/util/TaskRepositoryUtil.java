package org.example.util;

import lombok.Getter;
import org.example.datasource.repositoryservice.TaskRepositoryService;

public class TaskRepositoryUtil {
    @Getter
    private final static TaskRepositoryService taskRepositoryService;

    static {
        taskRepositoryService = new TaskRepositoryService();
    }

}
