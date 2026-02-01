package org.example.util;

import lombok.Getter;
import org.example.domain.service.TaskService;

public class DomainServiceUtil {
    @Getter
    private static final TaskService taskService;

    static {
        taskService = new TaskService();
    }
}
