package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class SpawnService {
    private static final String DOCUMENT_KEY = "spawn-profiles";

    private final CrossroadsPlugin plugin;
    private final Map<String, SavedLocation> spawns = new TreeMap<>();

    public SpawnService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        spawns.clear();
        YamlConfiguration document = plugin.getStorageManager().getProvider().loadDocument(DOCUMENT_KEY);
        ConfigurationSection profiles = document.getConfigurationSection("profiles");
        if (profiles != null) {
            for (String profile : profiles.getKeys(false)) {
                SavedLocation location = SavedLocation.fromSection(profiles.getConfigurationSection(profile));
                if (location != null) {
                    spawns.put(profile.toLowerCase(), location);
                }
            }
        }
        if (spawns.isEmpty()) {
            SavedLocation legacySpawn = plugin.getStorageManager().getProvider().loadSpawn();
            if (legacySpawn != null) {
                spawns.put(PlayerData.GLOBAL_SCOPE, legacySpawn);
                save();
            }
        }
    }

    public void save() {
        YamlConfiguration document = new YamlConfiguration();
        ConfigurationSection profiles = document.createSection("profiles");
        for (Map.Entry<String, SavedLocation> entry : spawns.entrySet()) {
            entry.getValue().write(profiles.createSection(entry.getKey()));
        }
        plugin.getStorageManager().getProvider().saveDocument(DOCUMENT_KEY, document);
    }

    public SavedLocation getSpawn(String profile) {
        SavedLocation location = spawns.get(profile.toLowerCase());
        if (location == null && !PlayerData.GLOBAL_SCOPE.equalsIgnoreCase(profile)) {
            location = spawns.get(PlayerData.GLOBAL_SCOPE);
        }
        return location;
    }

    public void setSpawn(String profile, SavedLocation spawn) {
        this.spawns.put(profile.toLowerCase(), spawn);
        save();
    }

    public Map<String, SavedLocation> getSpawns() {
        return Collections.unmodifiableMap(spawns);
    }
}
