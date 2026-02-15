package org.example.util;

import lombok.Getter;
import org.example.domain.service.TaskService;

public class DomainServiceUtil {
    @Getter
    private static final TaskService taskService;
    @Getter
    private static final EventBus eventBus;

    static {
        eventBus = new EventBus();
        taskService = new TaskService();
    }
}
