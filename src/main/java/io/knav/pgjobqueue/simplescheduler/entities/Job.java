package io.knav.pgjobqueue.simplescheduler.entities;

import java.util.UUID;

public record Job(UUID id, String description, String status) {}
