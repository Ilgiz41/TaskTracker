package org.example.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

@NoArgsConstructor
public class Task {

    @Getter @Setter
    private long id;
    @Getter @Setter
    private String title;
    @Getter @Setter
    private String description;
    @Getter @Setter
    private LocalDate date;
    @Getter @Setter
    private boolean completed;
    @Getter @Setter
    private int priority;

    public Task(String title, String description, LocalDate date, boolean completed,  int priority) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.completed = completed;
        this.priority = priority;
    }
}
