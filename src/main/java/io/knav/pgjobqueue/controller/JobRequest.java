package io.knav.pgjobqueue.controller;

import io.knav.pgjobqueue.entities.Job;

import java.util.UUID;

public record JobRequest(String description) {
    public Job toDomain() {
        return new Job(UUID.randomUUID(), description, "active");
    }
}
