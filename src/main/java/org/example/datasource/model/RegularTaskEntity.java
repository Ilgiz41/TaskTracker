package org.example.datasource.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@NoArgsConstructor
@Table(name = "regular_task")
public class RegularTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private long id;
    @Column
    @Getter @Setter
    private String title;
    @Column
    @Getter @Setter
    private String description;

    @Column
    @Getter @Setter
    private int priority;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "regular_task_days",
            joinColumns = @JoinColumn(name = "task_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    @Getter @Setter
    private Set<DayOfWeek> dayOfWeeks = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "regular_task_exclusions",
            joinColumns = @JoinColumn(name = "task_id")
    )
    @Column(name = "excluded_days")
    @Getter @Setter
    private Set<LocalDate> excludedDays = new HashSet<>();

    public RegularTaskEntity(String title, String description, int priority, Set<DayOfWeek> dayOfWeeks) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dayOfWeeks = dayOfWeeks;
    }
}
