package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import dev.crossroadsmc.crossroads.util.PlayerDataCodec;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class JailService {
    private static final String DOCUMENT_KEY = "jails";

    private final CrossroadsPlugin plugin;
    private final Map<String, SavedLocation> jails = new TreeMap<>();

    public JailService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        jails.clear();
        YamlConfiguration document = plugin.getStorageManager().getProvider().loadDocument(DOCUMENT_KEY);
        ConfigurationSection section = document.getConfigurationSection("jails");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            SavedLocation location = SavedLocation.fromSection(section.getConfigurationSection(key));
            if (location != null) {
                jails.put(key.toLowerCase(), location);
            }
        }
    }

    public void save() {
        YamlConfiguration document = new YamlConfiguration();
        ConfigurationSection section = document.createSection("jails");
        for (Map.Entry<String, SavedLocation> entry : jails.entrySet()) {
            entry.getValue().write(section.createSection(entry.getKey()));
        }
        plugin.getStorageManager().getProvider().saveDocument(DOCUMENT_KEY, document);
    }

    public void setJail(String key, SavedLocation location) {
        jails.put(key.toLowerCase(), location);
        save();
    }

    public SavedLocation getJail(String key) {
        return jails.get(key.toLowerCase());
    }

    public Map<String, SavedLocation> getJails() {
        return Collections.unmodifiableMap(jails);
    }
}
