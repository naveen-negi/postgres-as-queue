package io.knav.pgjobqueue.entities;

import java.util.UUID;

public record Job(UUID id, String description, String status) {}
