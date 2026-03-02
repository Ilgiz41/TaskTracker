package org.example.infrastructure.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class FileService {

    private final static Path LOG_PATH = Paths.get("system.log");

    public void appendLinesToLog(List<String> lines) throws IOException {
        if (!Files.exists(LOG_PATH)) Files.createFile(LOG_PATH);
        Files.write(LOG_PATH, lines, StandardOpenOption.APPEND);
    }

}
