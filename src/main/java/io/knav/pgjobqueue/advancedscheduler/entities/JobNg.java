package io.knav.pgjobqueue.advancedscheduler.entities;

import java.util.Objects;
import java.util.UUID;

public record JobNg(UUID id, Metadata metadata, String CurrentJobStatus) {

    public boolean isProcessable() {
       return Objects.equals(this.CurrentJobStatus, "archive_pending");
    }
}
