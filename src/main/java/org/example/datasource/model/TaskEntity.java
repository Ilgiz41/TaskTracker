package org.example.datasource.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "Task")
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

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

    @Column
    @Getter @Setter
    private boolean isRegularTask;

    @Column
    @Getter @Setter
    private Long templateId;
}
