package dev.crossroadsmc.crossroads.storage;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.ModerationLogEntry;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import dev.crossroadsmc.crossroads.util.PlayerDataCodec;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class YamlStorageProvider implements StorageProvider {
    private final CrossroadsPlugin plugin;
    private final File playersDirectory;
    private final File warpsFile;
    private final File spawnFile;
    private final File moderationLogsFile;
    private final Object ioLock = new Object();

    public YamlStorageProvider(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        this.playersDirectory = new File(plugin.getDataFolder(), "players");
        this.warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        this.spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
        this.moderationLogsFile = new File(plugin.getDataFolder(), "moderation-logs.yml");
    }

    @Override
    public void initialize() {
        if (!playersDirectory.exists()) {
            playersDirectory.mkdirs();
        }
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        File file = new File(playersDirectory, uuid + ".yml");
        if (!file.exists()) {
            return new PlayerData(uuid);
        }

        synchronized (ioLock) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            return PlayerDataCodec.deserializePlayer(uuid, yaml);
        }
    }

    @Override
    public void savePlayerData(PlayerData data) {
        File file = new File(playersDirectory, data.getUuid() + ".yml");
        YamlConfiguration yaml = PlayerDataCodec.serializePlayer(data);
        try {
            saveYamlAtomically(yaml, file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to save player data for " + data.getUuid() + ".", exception);
        }
    }

    @Override
    public Map<String, SavedLocation> loadWarps() {
        Map<String, SavedLocation> warps = new HashMap<>();
        if (!warpsFile.exists()) {
            return warps;
        }

        synchronized (ioLock) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(warpsFile);
            ConfigurationSection root = yaml.getConfigurationSection("warps");
            if (root == null) {
                return warps;
            }

            for (String key : root.getKeys(false)) {
                SavedLocation location = SavedLocation.fromSection(root.getConfigurationSection(key));
                if (location != null) {
                    warps.put(key.toLowerCase(), location);
                }
            }
        }
        return warps;
    }

    @Override
    public void saveWarps(Map<String, SavedLocation> warps) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("warps");
        for (Map.Entry<String, SavedLocation> entry : warps.entrySet()) {
            ConfigurationSection section = root.createSection(entry.getKey());
            entry.getValue().write(section);
        }

        try {
            saveYamlAtomically(yaml, warpsFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to save warps.yml.", exception);
        }
    }

    @Override
    public SavedLocation loadSpawn() {
        if (!spawnFile.exists()) {
            return null;
        }

        synchronized (ioLock) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(spawnFile);
            return SavedLocation.fromSection(yaml.getConfigurationSection("spawn"));
        }
    }

    @Override
    public void saveSpawn(SavedLocation spawn) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (spawn != null) {
            spawn.write(yaml.createSection("spawn"));
        }

        try {
            saveYamlAtomically(yaml, spawnFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to save spawn.yml.", exception);
        }
    }

    @Override
    public void appendModerationLog(ModerationLogEntry entry) {
        List<ModerationLogEntry> entries = loadAllModerationLogs();
        entries.add(entry);
        YamlConfiguration yaml = PlayerDataCodec.serializeModerationLogs(entries);
        try {
            saveYamlAtomically(yaml, moderationLogsFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to save moderation logs.", exception);
        }
    }

    @Override
    public List<ModerationLogEntry> loadModerationLogs(UUID targetUuid, String targetName, int limit) {
        return loadAllModerationLogs().stream()
            .filter(entry -> {
                if (targetUuid == null && (targetName == null || targetName.isBlank())) {
                    return true;
                }
                if (targetUuid != null && targetUuid.equals(entry.getTargetUuid())) {
                    return true;
                }
                return targetName != null && !targetName.isBlank() && targetName.equalsIgnoreCase(entry.getTargetName());
            })
            .sorted(Comparator.comparingLong(ModerationLogEntry::getOccurredAt).reversed())
            .limit(limit)
            .toList();
    }

    @Override
    public StorageType getType() {
        return StorageType.YAML;
    }

    @Override
    public void close() {
    }

    private List<ModerationLogEntry> loadAllModerationLogs() {
        if (!moderationLogsFile.exists()) {
            return new ArrayList<>();
        }

        synchronized (ioLock) {
            try {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.load(moderationLogsFile);
                return PlayerDataCodec.deserializeModerationLogs(yaml);
            } catch (IOException | InvalidConfigurationException exception) {
                plugin.getLogger().log(Level.WARNING, "Unable to load moderation logs.", exception);
                return new ArrayList<>();
            }
        }
    }

    private void saveYamlAtomically(YamlConfiguration yaml, File target) throws IOException {
        synchronized (ioLock) {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            File tempFile = new File(parent == null ? plugin.getDataFolder() : parent, target.getName() + ".tmp");
            yaml.save(tempFile);

            Path source = tempFile.toPath();
            Path destination = target.toPath();
            try {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
