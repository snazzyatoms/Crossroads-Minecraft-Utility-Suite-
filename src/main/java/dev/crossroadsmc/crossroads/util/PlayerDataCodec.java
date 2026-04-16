package dev.crossroadsmc.crossroads.util;

import dev.crossroadsmc.crossroads.model.MailMessage;
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
        yaml.set("nickname", data.getNickname());
        yaml.set("last-join-at", data.getLastJoinAt());
        yaml.set("last-quit-at", data.getLastQuitAt());
        yaml.set("muted-until", data.getMutedUntil());
        yaml.set("mute-reason", data.getMuteReason());
        yaml.set("shadow-muted", data.isShadowMuted());
        yaml.set("frozen", data.isFrozen());
        yaml.set("freeze-reason", data.getFreezeReason());
        yaml.set("banned-until", data.getBannedUntil());
        yaml.set("ban-reason", data.getBanReason());
        yaml.set("jailed-until", data.getJailedUntil());
        yaml.set("jail-name", data.getJailName());
        yaml.set("ignored", data.getIgnoredPlayers().stream().map(UUID::toString).toList());
        if (data.getBackLocation() != null) {
            data.getBackLocation().write(yaml.createSection("back"));
        }

        ConfigurationSection homesSection = yaml.createSection("homes");
        for (Map.Entry<String, Map<String, SavedLocation>> scopeEntry : data.getHomesByScope().entrySet()) {
            ConfigurationSection scopeSection = homesSection.createSection(scopeEntry.getKey());
            for (Map.Entry<String, SavedLocation> entry : scopeEntry.getValue().entrySet()) {
                ConfigurationSection section = scopeSection.createSection(entry.getKey());
                entry.getValue().write(section);
            }
        }

        ConfigurationSection kitsSection = yaml.createSection("kits");
        for (Map.Entry<String, Long> entry : data.getKitCooldowns().entrySet()) {
            kitsSection.set(entry.getKey(), entry.getValue());
        }

        ConfigurationSection cooldownsSection = yaml.createSection("command-cooldowns");
        for (Map.Entry<String, Long> entry : data.getCommandCooldowns().entrySet()) {
            cooldownsSection.set(entry.getKey(), entry.getValue());
        }

        ConfigurationSection mailSection = yaml.createSection("mail");
        List<MailMessage> mailbox = data.getMailbox();
        for (int index = 0; index < mailbox.size(); index++) {
            MailMessage message = mailbox.get(index);
            ConfigurationSection section = mailSection.createSection(String.valueOf(index));
            section.set("sent-at", message.getSentAt());
            section.set("sender-uuid", message.getSenderUuid() == null ? "" : message.getSenderUuid().toString());
            section.set("sender-name", message.getSenderName());
            section.set("body", message.getBody());
            section.set("read", message.isRead());
        }

        return yaml;
    }

    public static PlayerData deserializePlayer(UUID uuid, YamlConfiguration yaml) {
        PlayerData data = new PlayerData(uuid);
        data.setLastKnownName(yaml.getString("last-known-name", ""));
        data.setNickname(yaml.getString("nickname", ""));
        data.setLastJoinAt(yaml.getLong("last-join-at", 0L));
        data.setLastQuitAt(yaml.getLong("last-quit-at", 0L));
        data.setMutedUntil(yaml.getLong("muted-until", 0L));
        data.setMuteReason(yaml.getString("mute-reason", ""));
        data.setShadowMuted(yaml.getBoolean("shadow-muted", false));
        data.setFrozen(yaml.getBoolean("frozen", false));
        data.setFreezeReason(yaml.getString("freeze-reason", ""));
        data.setBannedUntil(yaml.getLong("banned-until", 0L));
        data.setBanReason(yaml.getString("ban-reason", ""));
        data.setJailedUntil(yaml.getLong("jailed-until", 0L));
        data.setJailName(yaml.getString("jail-name", ""));
        data.setBackLocation(SavedLocation.fromSection(yaml.getConfigurationSection("back")));

        ConfigurationSection homesSection = yaml.getConfigurationSection("homes");
        if (homesSection != null) {
            Map<String, Map<String, SavedLocation>> scopedHomes = new HashMap<>();
            boolean legacyFlatHomes = false;
            for (String key : homesSection.getKeys(false)) {
                ConfigurationSection scopeSection = homesSection.getConfigurationSection(key);
                SavedLocation legacyLocation = SavedLocation.fromSection(scopeSection);
                if (legacyLocation != null) {
                    legacyFlatHomes = true;
                    scopedHomes.computeIfAbsent(PlayerData.GLOBAL_SCOPE, ignored -> new HashMap<>())
                        .put(key.toLowerCase(), legacyLocation);
                    continue;
                }

                if (scopeSection == null) {
                    continue;
                }
                Map<String, SavedLocation> homes = new HashMap<>();
                for (String homeKey : scopeSection.getKeys(false)) {
                    SavedLocation location = SavedLocation.fromSection(scopeSection.getConfigurationSection(homeKey));
                    if (location != null) {
                        homes.put(homeKey.toLowerCase(), location);
                    }
                }
                if (!homes.isEmpty()) {
                    scopedHomes.put(key.toLowerCase(), homes);
                }
            }
            if (!scopedHomes.isEmpty() || legacyFlatHomes) {
                data.importHomes(scopedHomes);
            }
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

        ConfigurationSection commandCooldownsSection = yaml.getConfigurationSection("command-cooldowns");
        if (commandCooldownsSection != null) {
            Map<String, Long> cooldowns = new HashMap<>();
            for (String key : commandCooldownsSection.getKeys(false)) {
                cooldowns.put(key.toLowerCase(), commandCooldownsSection.getLong(key));
            }
            data.importCommandCooldowns(cooldowns);
        }

        ConfigurationSection mailSection = yaml.getConfigurationSection("mail");
        if (mailSection != null) {
            List<MailMessage> mailbox = new ArrayList<>();
            for (String key : mailSection.getKeys(false)) {
                ConfigurationSection section = mailSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                mailbox.add(new MailMessage(
                    section.getLong("sent-at", System.currentTimeMillis()),
                    parseUuid(section.getString("sender-uuid", "")),
                    section.getString("sender-name", "Unknown"),
                    section.getString("body", ""),
                    section.getBoolean("read", false)
                ));
            }
            data.importMailbox(mailbox);
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
