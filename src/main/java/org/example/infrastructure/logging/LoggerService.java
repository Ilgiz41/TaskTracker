package org.example.infrastructure.logging;

import org.example.event.Event;
import org.example.infrastructure.file.FileService;
import org.example.util.DomainServiceUtil;
import org.example.util.EventBus;
import org.example.util.InfrastructureUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LoggerService {

    private final FileService fileService;
    private final EventBus eventBus;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LoggerService() {
        this.fileService = InfrastructureUtil.getFileService();
        this.eventBus = DomainServiceUtil.getEventBus();
        eventBus.subscribe(Event.CriticalErrorExceptionEvent.class, this::handleException);
    }

    private void handleException(Event.CriticalErrorExceptionEvent event){
        try {
            Throwable cause = event.getCause();
            List<String> logLines = new ArrayList<>();
            String timestamp = LocalDateTime.now().format(formatter);
            logLines.add(String.format("--- [%s] %s ---", timestamp, event.getClass().getSimpleName()));
            logLines.add("Message: " + cause.getMessage());

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            cause.printStackTrace(pw);

            logLines.add("StackTrace:");
            logLines.add(sw.toString());
            logLines.add("------------------------------------------");

            fileService.appendLinesToLog(logLines);
        } catch (Exception e){
            System.err.println("Critical error occurred while handling event " + event.getClass().getSimpleName());
        }
    }
}
