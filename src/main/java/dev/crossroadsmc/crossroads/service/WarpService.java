package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class WarpService {
    private static final String DOCUMENT_KEY = "scoped-warps";

    private final CrossroadsPlugin plugin;
    private final Map<String, Map<String, SavedLocation>> warps = new TreeMap<>();

    public WarpService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        warps.clear();
        YamlConfiguration document = plugin.getStorageManager().getProvider().loadDocument(DOCUMENT_KEY);
        ConfigurationSection profiles = document.getConfigurationSection("profiles");
        if (profiles != null) {
            for (String profile : profiles.getKeys(false)) {
                ConfigurationSection section = profiles.getConfigurationSection(profile);
                if (section == null) {
                    continue;
                }
                Map<String, SavedLocation> scoped = new TreeMap<>();
                for (String key : section.getKeys(false)) {
                    SavedLocation location = SavedLocation.fromSection(section.getConfigurationSection(key));
                    if (location != null) {
                        scoped.put(key.toLowerCase(), location);
                    }
                }
                if (!scoped.isEmpty()) {
                    warps.put(profile.toLowerCase(), scoped);
                }
            }
        }

        if (warps.isEmpty()) {
            Map<String, SavedLocation> legacyWarps = plugin.getStorageManager().getProvider().loadWarps();
            if (!legacyWarps.isEmpty()) {
                warps.put(PlayerData.GLOBAL_SCOPE, new TreeMap<>(legacyWarps));
                save();
            }
        }
    }

    public void save() {
        YamlConfiguration document = new YamlConfiguration();
        ConfigurationSection profiles = document.createSection("profiles");
        for (Map.Entry<String, Map<String, SavedLocation>> profileEntry : warps.entrySet()) {
            ConfigurationSection section = profiles.createSection(profileEntry.getKey());
            for (Map.Entry<String, SavedLocation> entry : profileEntry.getValue().entrySet()) {
                entry.getValue().write(section.createSection(entry.getKey()));
            }
        }
        plugin.getStorageManager().getProvider().saveDocument(DOCUMENT_KEY, document);
    }

    public Map<String, SavedLocation> getWarps(String profile) {
        Map<String, SavedLocation> scoped = warps.get(profile.toLowerCase());
        return scoped == null ? Collections.emptyMap() : Collections.unmodifiableMap(scoped);
    }

    public SavedLocation getWarp(String profile, String name) {
        Map<String, SavedLocation> scoped = warps.get(profile.toLowerCase());
        if (scoped != null && scoped.containsKey(name.toLowerCase())) {
            return scoped.get(name.toLowerCase());
        }
        if (!PlayerData.GLOBAL_SCOPE.equalsIgnoreCase(profile)) {
            Map<String, SavedLocation> global = warps.get(PlayerData.GLOBAL_SCOPE);
            if (global != null) {
                return global.get(name.toLowerCase());
            }
        }
        return null;
    }

    public void setWarp(String profile, String name, SavedLocation location) {
        warps.computeIfAbsent(profile.toLowerCase(), ignored -> new TreeMap<>()).put(name.toLowerCase(), location);
        save();
    }

    public SavedLocation removeWarp(String profile, String name) {
        Map<String, SavedLocation> scoped = warps.get(profile.toLowerCase());
        SavedLocation removed = scoped == null ? null : scoped.remove(name.toLowerCase());
        if (scoped != null && scoped.isEmpty()) {
            warps.remove(profile.toLowerCase());
        }
        save();
        return removed;
    }

    public List<String> getAvailableWarpNames(String profile) {
        List<String> names = new ArrayList<>(getWarps(profile).keySet());
        if (!PlayerData.GLOBAL_SCOPE.equalsIgnoreCase(profile)) {
            for (String global : getWarps(PlayerData.GLOBAL_SCOPE).keySet()) {
                if (!names.contains(global)) {
                    names.add(global);
                }
            }
        }
        Collections.sort(names);
        return names;
    }
}
