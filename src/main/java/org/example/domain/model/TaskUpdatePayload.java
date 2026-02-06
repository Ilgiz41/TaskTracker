package org.example.domain.model;

import java.time.LocalDate;

public record TaskUpdatePayload(TaskId id, String title, String description, int priority, LocalDate newDate, boolean completed) {
}
