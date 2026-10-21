package com.example.tasktracker.project.dto;

import java.time.Instant;

public record ProjectResponse (
        Long id,
        String name,
        String description,
        Long ownerId,
        Instant createdAt
) {
}
