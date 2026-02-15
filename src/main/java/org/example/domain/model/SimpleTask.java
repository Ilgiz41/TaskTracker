package org.example.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.domain.service.TaskService;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SimpleTask extends Task {

    @Getter
    @Setter
    private boolean completed;
    @Getter
    @Setter
    private boolean isRegular;
    @Getter
    @Setter
    private Long templateId;

    public SimpleTask(String title, String description, LocalDate date, boolean completed, int priority, boolean isRegularTask, Long templateId) {
        super(title, description, priority, date);
        this.completed = completed;
        this.isRegular = isRegularTask;
        this.templateId = templateId;
        this.date = date;
    }

    @Override
    public void delete(TaskService taskService, LocalDate selectedDay) {
        taskService.deleteSimpleTask(this);
    }

    @Override
    public void update(TaskService taskService, LocalDate selectedDay, TaskUpdatePayload payload) {
        taskService.updateSimpleTask(this, selectedDay, payload);
    }
}
