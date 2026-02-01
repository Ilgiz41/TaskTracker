package org.example.datasource.mapper;

import org.example.datasource.model.TaskEntity;
import org.example.domain.model.Task;

public class TaskMapper {

    public static Task toDomain(TaskEntity taskEntity) {
        Task task = new Task();
        task.setId(taskEntity.getId());
        task.setTitle(taskEntity.getTitle());
        task.setDescription(taskEntity.getDescription());
        task.setDate(taskEntity.getDate());
        task.setCompleted(taskEntity.isCompleted());
        task.setPriority(taskEntity.getPriority());
        return task;
    }

    public static TaskEntity toEntity(Task domain) {
        return new TaskEntity(domain.getId(), domain.getTitle(), domain.getDescription(), domain.getDate(), domain.isCompleted(),  domain.getPriority());
    }
}
