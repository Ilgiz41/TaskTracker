package org.example.domain.model;

import lombok.*;
import org.example.domain.service.TaskService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
public abstract class Task implements Comparable<Task>, Identifiable<TaskId> {

    @Getter
    @Setter
    protected TaskId id;
    @Getter
    @Setter
    protected String title;
    @Getter
    @Setter
    protected String description;
    @Getter
    @Setter
    protected int priority;
    @Getter
    @Setter
    protected LocalDate date;

    protected Task(String title, String description, int priority, LocalDate date) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.date = date;
    }

    public abstract void delete(TaskService taskService, LocalDate selectedDay);

    public abstract void update(TaskService taskService, LocalDate selectedDay, TaskUpdatePayload payload);

    @Override
    public int compareTo(Task o) {
        return Comparator.comparing(Task::isCompleted).reversed()
                .thenComparing(Task::isRegular)
                .thenComparing(Task::getPriority).reversed()
                .compare(this, o);
    }

    public Set<DayOfWeek> getDayOfWeeks() {
        return null;
    }

    public Long getRawId() {
        return id == null ? null : id.id();
    }

    public boolean isCompleted() {
        return false;
    }

    public boolean isRegular() {
        return true;
    }
}