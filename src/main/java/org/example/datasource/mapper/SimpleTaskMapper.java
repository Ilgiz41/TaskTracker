package org.example.datasource.mapper;

import org.example.datasource.model.TaskEntity;
import org.example.domain.model.SimpleTask;
import org.example.domain.model.TaskId;

public class SimpleTaskMapper {

    public static SimpleTask toDomain(TaskEntity taskEntity) {
        SimpleTask task = new SimpleTask();
        task.setId(new TaskId(taskEntity.getId(), false));
        task.setTitle(taskEntity.getTitle());
        task.setDescription(taskEntity.getDescription());
        task.setDate(taskEntity.getDate());
        task.setCompleted(taskEntity.isCompleted());
        task.setPriority(taskEntity.getPriority());
        task.setRegular(taskEntity.isRegularTask());
        task.setTemplateId(taskEntity.getTemplateId());
        return task;
    }

    public static TaskEntity toEntity(SimpleTask domain) {
        return new TaskEntity(domain.getRawId(), domain.getTitle(), domain.getDescription(), domain.getDate(), domain.isCompleted(),  domain.getPriority(), domain.isRegular(), domain.getTemplateId());
    }
}
