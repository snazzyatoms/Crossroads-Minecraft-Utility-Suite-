package dev.crossroadsmc.crossroads.model;

import java.util.Objects;
import java.util.UUID;

public final class MailMessage {
    private final long sentAt;
    private final UUID senderUuid;
    private final String senderName;
    private final String body;
    private final boolean read;

    public MailMessage(long sentAt, UUID senderUuid, String senderName, String body, boolean read) {
        this.sentAt = sentAt;
        this.senderUuid = senderUuid;
        this.senderName = Objects.requireNonNullElse(senderName, "Unknown");
        this.body = Objects.requireNonNullElse(body, "");
        this.read = read;
    }

    public long getSentAt() {
        return sentAt;
    }

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getBody() {
        return body;
    }

    public boolean isRead() {
        return read;
    }

    public MailMessage markRead() {
        if (read) {
            return this;
        }
        return new MailMessage(sentAt, senderUuid, senderName, body, true);
    }
}
