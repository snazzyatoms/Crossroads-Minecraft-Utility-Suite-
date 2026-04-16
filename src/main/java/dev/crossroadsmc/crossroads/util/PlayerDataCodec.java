package dev.crossroadsmc.crossroads.util;

import dev.crossroadsmc.crossroads.model.ModerationLogEntry;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PlayerDataCodec {
    private PlayerDataCodec() {
    }

    public static YamlConfiguration serializePlayer(PlayerData data) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("last-known-name", data.getLastKnownName());
        yaml.set("last-join-at", data.getLastJoinAt());
        yaml.set("last-quit-at", data.getLastQuitAt());
        yaml.set("muted-until", data.getMutedUntil());
        yaml.set("mute-reason", data.getMuteReason());
        yaml.set("frozen", data.isFrozen());
        yaml.set("freeze-reason", data.getFreezeReason());
        yaml.set("ignored", data.getIgnoredPlayers().stream().map(UUID::toString).toList());

        ConfigurationSection homesSection = yaml.createSection("homes");
        for (Map.Entry<String, SavedLocation> entry : data.getHomes().entrySet()) {
            ConfigurationSection section = homesSection.createSection(entry.getKey());
            entry.getValue().write(section);
        }

        ConfigurationSection kitsSection = yaml.createSection("kits");
        for (Map.Entry<String, Long> entry : data.getKitCooldowns().entrySet()) {
            kitsSection.set(entry.getKey(), entry.getValue());
        }

        return yaml;
    }

    public static PlayerData deserializePlayer(UUID uuid, YamlConfiguration yaml) {
        PlayerData data = new PlayerData(uuid);
        data.setLastKnownName(yaml.getString("last-known-name", ""));
        data.setLastJoinAt(yaml.getLong("last-join-at", 0L));
        data.setLastQuitAt(yaml.getLong("last-quit-at", 0L));
        data.setMutedUntil(yaml.getLong("muted-until", 0L));
        data.setMuteReason(yaml.getString("mute-reason", ""));
        data.setFrozen(yaml.getBoolean("frozen", false));
        data.setFreezeReason(yaml.getString("freeze-reason", ""));

        ConfigurationSection homesSection = yaml.getConfigurationSection("homes");
        if (homesSection != null) {
            Map<String, SavedLocation> homes = new HashMap<>();
            for (String key : homesSection.getKeys(false)) {
                SavedLocation location = SavedLocation.fromSection(homesSection.getConfigurationSection(key));
                if (location != null) {
                    homes.put(key.toLowerCase(), location);
                }
            }
            data.importHomes(homes);
        }

        Set<UUID> ignored = new HashSet<>();
        for (String entry : yaml.getStringList("ignored")) {
            try {
                ignored.add(UUID.fromString(entry));
            } catch (IllegalArgumentException ignoredException) {
                // Ignore malformed entries from older data files.
            }
        }
        data.importIgnoredPlayers(ignored);

        ConfigurationSection kitsSection = yaml.getConfigurationSection("kits");
        if (kitsSection != null) {
            Map<String, Long> cooldowns = new HashMap<>();
            for (String key : kitsSection.getKeys(false)) {
                cooldowns.put(key.toLowerCase(), kitsSection.getLong(key));
            }
            data.importKitCooldowns(cooldowns);
        }

        return data;
    }

    public static String toText(YamlConfiguration yaml) {
        return yaml.saveToString();
    }

    public static YamlConfiguration fromText(String input) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        if (input != null && !input.isBlank()) {
            yaml.loadFromString(input);
        }
        return yaml;
    }

    public static YamlConfiguration serializeLocation(SavedLocation location) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (location != null) {
            location.write(yaml.createSection("location"));
        }
        return yaml;
    }

    public static SavedLocation deserializeLocation(YamlConfiguration yaml, String key) {
        return SavedLocation.fromSection(yaml.getConfigurationSection(key));
    }

    public static YamlConfiguration serializeModerationLogs(List<ModerationLogEntry> entries) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (int index = 0; index < entries.size(); index++) {
            ModerationLogEntry entry = entries.get(index);
            ConfigurationSection section = yaml.createSection("entries." + index);
            section.set("occurred-at", entry.getOccurredAt());
            section.set("action", entry.getAction());
            section.set("actor-uuid", entry.getActorUuid() == null ? "" : entry.getActorUuid().toString());
            section.set("actor-name", entry.getActorName());
            section.set("target-uuid", entry.getTargetUuid() == null ? "" : entry.getTargetUuid().toString());
            section.set("target-name", entry.getTargetName());
            section.set("details", entry.getDetails());
        }
        return yaml;
    }

    public static List<ModerationLogEntry> deserializeModerationLogs(YamlConfiguration yaml) {
        List<ModerationLogEntry> entries = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection("entries");
        if (root == null) {
            return entries;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            entries.add(new ModerationLogEntry(
                section.getLong("occurred-at", System.currentTimeMillis()),
                section.getString("action", "NOTE"),
                parseUuid(section.getString("actor-uuid", "")),
                section.getString("actor-name", "Console"),
                parseUuid(section.getString("target-uuid", "")),
                section.getString("target-name", ""),
                section.getString("details", "")
            ));
        }
        return entries;
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
