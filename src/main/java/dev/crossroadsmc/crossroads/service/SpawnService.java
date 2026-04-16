package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.SavedLocation;

public final class SpawnService {
    private final CrossroadsPlugin plugin;
    private SavedLocation spawn;

    public SpawnService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        spawn = plugin.getStorageManager().getProvider().loadSpawn();
    }

    public void save() {
        plugin.getStorageManager().getProvider().saveSpawn(spawn);
    }

    public SavedLocation getSpawn() {
        return spawn;
    }

    public void setSpawn(SavedLocation spawn) {
        this.spawn = spawn;
        save();
    }
}
