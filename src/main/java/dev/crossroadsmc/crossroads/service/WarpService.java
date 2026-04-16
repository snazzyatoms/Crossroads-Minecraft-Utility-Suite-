package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class WarpService {
    private final CrossroadsPlugin plugin;
    private final Map<String, SavedLocation> warps = new TreeMap<>();

    public WarpService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        warps.clear();
        warps.putAll(plugin.getStorageManager().getProvider().loadWarps());
    }

    public void save() {
        plugin.getStorageManager().getProvider().saveWarps(warps);
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
