package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class WarpService {
    private final CrossroadsPlugin plugin;
    private final File file;
    private final Map<String, SavedLocation> warps = new TreeMap<>();

    public WarpService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
        reload();
    }

    public void reload() {
        warps.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("warps");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            SavedLocation location = SavedLocation.fromSection(section.getConfigurationSection(key));
            if (location != null) {
                warps.put(key.toLowerCase(), location);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("warps");
        for (Map.Entry<String, SavedLocation> entry : warps.entrySet()) {
            ConfigurationSection child = section.createSection(entry.getKey());
            entry.getValue().write(child);
        }

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to save warps.yml.", exception);
        }
    }

    public Map<String, SavedLocation> getWarps() {
        return Collections.unmodifiableMap(warps);
    }

    public SavedLocation getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public void setWarp(String name, SavedLocation location) {
        warps.put(name.toLowerCase(), location);
        save();
    }

    public SavedLocation removeWarp(String name) {
        SavedLocation removed = warps.remove(name.toLowerCase());
        save();
        return removed;
    }
}
