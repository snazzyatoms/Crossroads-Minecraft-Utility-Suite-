package dev.crossroadsmc.crossroads.storage;

import dev.crossroadsmc.crossroads.model.ModerationLogEntry;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

public interface StorageProvider extends AutoCloseable {
    void initialize() throws Exception;

    PlayerData loadPlayerData(UUID uuid);

    void savePlayerData(PlayerData data);

    Map<String, SavedLocation> loadWarps();

    void saveWarps(Map<String, SavedLocation> warps);

    SavedLocation loadSpawn();

    void saveSpawn(SavedLocation spawn);

    void appendModerationLog(ModerationLogEntry entry);

    List<ModerationLogEntry> loadModerationLogs(UUID targetUuid, String targetName, int limit);

    YamlConfiguration loadDocument(String key);

    void saveDocument(String key, YamlConfiguration document);

    StorageType getType();

    @Override
    void close();
}
