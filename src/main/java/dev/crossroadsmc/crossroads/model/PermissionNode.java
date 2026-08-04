package dev.crossroadsmc.crossroads.model;

import java.util.Locale;
import java.util.Objects;

public final class PermissionNode {
    private final String permission;
    private final boolean value;
    private final long expiresAt;
    private final String world;
    private final String server;

    public PermissionNode(String permission, boolean value, long expiresAt, String world, String server) {
        this.permission = Objects.requireNonNullElse(permission, "").toLowerCase(Locale.ROOT);
        this.value = value;
        this.expiresAt = expiresAt;
        this.world = world == null || world.isBlank() ? "" : world.toLowerCase(Locale.ROOT);
        this.server = server == null || server.isBlank() ? "" : server.toLowerCase(Locale.ROOT);
    }

    public static PermissionNode permanent(String permission, boolean value) {
        return new PermissionNode(permission, value, 0L, "", "");
    }

    public String getPermission() {
        return permission;
    }

    public boolean getValue() {
        return value;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return expiresAt > 0L && expiresAt <= System.currentTimeMillis();
    }

    public boolean isTemporary() {
        return expiresAt > 0L;
    }

    public String getWorld() {
        return world;
    }

    public String getServer() {
        return server;
    }

    public boolean matchesContext(String currentWorld, String currentServer) {
        if (!world.isBlank() && (currentWorld == null || !world.equalsIgnoreCase(currentWorld))) {
            return false;
        }
        if (!server.isBlank() && (currentServer == null || !server.equalsIgnoreCase(currentServer))) {
            return false;
        }
        return true;
    }
}
