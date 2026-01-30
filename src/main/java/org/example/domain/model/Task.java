package org.example.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
public class Task {

    @Getter @Setter
    private long id;
    @Getter @Setter
    private String title;
    @Getter @Setter
    private String description;
    @Getter @Setter
    private LocalDateTime date;

    public Task(String title, String description, LocalDateTime date) {
        this.title = title;
        this.description = description;
        this.date = date;
    }
}
