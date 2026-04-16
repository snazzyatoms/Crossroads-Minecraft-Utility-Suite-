package dev.crossroadsmc.crossroads.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.inventory.ItemStack;

public final class KitDefinition {
    private final String key;
    private final String displayName;
    private final String permission;
    private final long cooldownSeconds;
    private final List<ItemStack> items;
    private final List<String> commands;

    public KitDefinition(
        String key,
        String displayName,
        String permission,
        long cooldownSeconds,
        List<ItemStack> items,
        List<String> commands
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.permission = permission;
        this.cooldownSeconds = cooldownSeconds;
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

    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<String> getCommands() {
        return Collections.unmodifiableList(commands);
    }
}
