package org.example.datasource.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "Task")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private long id;

    @Column
    @Getter @Setter
    private String title;

    @Column
    @Getter @Setter
    private String description;

    @Column
    @Getter @Setter
    private LocalDate date;

    @Column
    @Getter @Setter
    private boolean completed;

    @Column
    @Getter @Setter
    private int priority;

    public TaskEntity() {}

    public TaskEntity(long id, String title, String description, LocalDate date, boolean completed, int priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.completed = completed;
        this.priority = priority;
    }
}
