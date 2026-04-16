package dev.crossroadsmc.crossroads.model;

import java.util.UUID;

public final class TeleportRequest {
    private final UUID requesterUuid;
    private final UUID targetUuid;
    private final String requesterName;
    private final boolean hereRequest;
    private final long createdAt;

    public TeleportRequest(UUID requesterUuid, UUID targetUuid, String requesterName, boolean hereRequest, long createdAt) {
        this.requesterUuid = requesterUuid;
        this.targetUuid = targetUuid;
        this.requesterName = requesterName;
        this.hereRequest = hereRequest;
        this.createdAt = createdAt;
    }

    public UUID getRequesterUuid() {
        return requesterUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public boolean isHereRequest() {
        return hereRequest;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
