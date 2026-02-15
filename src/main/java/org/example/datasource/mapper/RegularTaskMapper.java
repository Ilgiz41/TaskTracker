package org.example.datasource.mapper;

import org.example.datasource.model.RegularTaskEntity;
import org.example.domain.model.RegularTask;
import org.example.domain.model.TaskId;

public class RegularTaskMapper {

    public static RegularTask toDomain(RegularTaskEntity regularTaskTemplate) {
        RegularTask regularTask = new RegularTask();
        regularTask.setId(new TaskId(regularTaskTemplate.getId(), true));
        regularTask.setTitle(regularTaskTemplate.getTitle());
        regularTask.setDescription(regularTaskTemplate.getDescription());
        regularTask.setPriority(regularTaskTemplate.getPriority());
        regularTask.setDayOfWeeks(regularTaskTemplate.getDayOfWeeks());
        return regularTask;
    }

    public static RegularTaskEntity toEntity(RegularTask regularTask) {
        RegularTaskEntity regularTaskTemplate = new RegularTaskEntity();
        regularTaskTemplate.setId(regularTask.getRawId());
        regularTaskTemplate.setTitle(regularTask.getTitle());
        regularTaskTemplate.setDescription(regularTask.getDescription());
        regularTaskTemplate.setPriority(regularTask.getPriority());
        regularTaskTemplate.setDayOfWeeks(regularTask.getDayOfWeeks());
        return regularTaskTemplate;
    }
}
