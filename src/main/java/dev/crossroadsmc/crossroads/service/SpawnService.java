package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class SpawnService {
    private final CrossroadsPlugin plugin;
    private final File file;
    private SavedLocation spawn;

    public SpawnService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawn.yml");
        reload();
    }

    public void reload() {
        spawn = null;
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("spawn");
        spawn = SavedLocation.fromSection(section);
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        if (spawn != null) {
            ConfigurationSection section = yaml.createSection("spawn");
            spawn.write(section);
        }

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to save spawn.yml.", exception);
        }
    }

    public SavedLocation getSpawn() {
        return spawn;
    }

    public void setSpawn(SavedLocation spawn) {
        this.spawn = spawn;
        save();
    }
}
