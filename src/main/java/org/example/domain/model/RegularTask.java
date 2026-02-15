package org.example.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.domain.service.TaskService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RegularTask extends Task {

    @Getter
    @Setter
    private Set<DayOfWeek> dayOfWeeks = new HashSet<>();

    @Override
    public void delete(TaskService taskService, LocalDate selectedDay) {
        taskService.deleteRegularTask(this, selectedDay);
    }

    @Override
    public void update(TaskService taskService, LocalDate selectedDay, TaskUpdatePayload payload) {
        taskService.updateRegularTask(this, selectedDay, payload);
    }
}
