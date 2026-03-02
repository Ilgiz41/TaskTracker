package org.example.util;

import lombok.Getter;
import org.example.infrastructure.concurrency.LockManager;
import org.example.infrastructure.concurrency.TaskDispatcher;
import org.example.infrastructure.file.FileService;
import org.example.infrastructure.logging.LoggerService;

public class InfrastructureUtil {

    @Getter
    private final static FileService fileService;
    @Getter
    private final static TaskDispatcher taskDispatcher;
    @Getter
    private final static LoggerService loggerService;
    @Getter
    private final static LockManager lockManager;

    static {
        lockManager = new LockManager();
        taskDispatcher = new TaskDispatcher();
        fileService = new FileService();
        loggerService = new LoggerService();
    }

}
