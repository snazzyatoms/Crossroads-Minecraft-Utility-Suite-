package dev.crossroadsmc.crossroads.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public final class ModerationLogEntry {
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final long occurredAt;
    private final String action;
    private final UUID actorUuid;
    private final String actorName;
    private final UUID targetUuid;
    private final String targetName;
    private final String details;

    public ModerationLogEntry(long occurredAt, String action, UUID actorUuid, String actorName, UUID targetUuid, String targetName, String details) {
        this.occurredAt = occurredAt;
        this.action = Objects.requireNonNull(action, "action");
        this.actorUuid = actorUuid;
        this.actorName = Objects.requireNonNullElse(actorName, "Console");
        this.targetUuid = targetUuid;
        this.targetName = Objects.requireNonNullElse(targetName, "");
        this.details = Objects.requireNonNullElse(details, "");
    }

    public long getOccurredAt() {
        return occurredAt;
    }

    public String getAction() {
        return action;
    }

    public UUID getActorUuid() {
        return actorUuid;
    }

    public String getActorName() {
        return actorName;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getDetails() {
        return details;
    }

    public String toDisplayLine() {
        String time = FORMATTER.format(Instant.ofEpochMilli(occurredAt));
        String suffix = details.isBlank() ? "" : " <muted>- <subtle>" + details;
        return "<muted>[" + time + "] <warn>" + action + " <subtle>by <text>" + actorName + " <subtle>-> <text>" + targetName + suffix;
    }
}
