package dev.crossroadsmc.crossroads.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.inventory.ItemStack;

public final class KitDefinition {
    private final String key;
    private final String displayName;
    private final String permission;
    private final long cooldownSeconds;
    private final double cost;
    private final Set<String> allowedProfiles;
    private final Set<String> blockedProfiles;
    private final List<ItemStack> items;
    private final List<String> commands;

    public KitDefinition(
        String key,
        String displayName,
        String permission,
        long cooldownSeconds,
        double cost,
        Set<String> allowedProfiles,
        Set<String> blockedProfiles,
        List<ItemStack> items,
        List<String> commands
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.permission = permission;
        this.cooldownSeconds = cooldownSeconds;
        this.cost = cost;
        this.allowedProfiles = Set.copyOf(allowedProfiles);
        this.blockedProfiles = Set.copyOf(blockedProfiles);
        this.items = List.copyOf(items);
        this.commands = List.copyOf(commands);
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPermission() {
        return permission;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public double getCost() {
        return cost;
    }

    public boolean isAvailableIn(String profile) {
        String normalized = profile.toLowerCase();
        if (!allowedProfiles.isEmpty() && !allowedProfiles.contains(normalized)) {
            return false;
        }
        return !blockedProfiles.contains(normalized);
    }

    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<String> getCommands() {
        return Collections.unmodifiableList(commands);
    }
}
