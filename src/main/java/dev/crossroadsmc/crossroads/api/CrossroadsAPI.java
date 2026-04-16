package dev.crossroadsmc.crossroads.api;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import dev.crossroadsmc.crossroads.service.ModuleManager;
import org.bukkit.entity.Player;

public final class CrossroadsAPI {
    private static CrossroadsPlugin plugin;

    private CrossroadsAPI() {
    }

    public static void initialize(CrossroadsPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    public static void shutdown() {
        plugin = null;
    }

    public static CrossroadsPlugin getPlugin() {
        if (plugin == null) {
            throw new IllegalStateException("CrossroadsAPI is not ready yet.");
        }
        return plugin;
    }

    public static PlayerData getPlayerData(Player player) {
        return getPlugin().getPlayerDataService().get(player);
    }

    public static SavedLocation getWarp(String name) {
        return getPlugin().getWarpService().getWarp(PlayerData.GLOBAL_SCOPE, name);
    }

    public static ModuleManager getModuleManager() {
        return getPlugin().getModuleManager();
    }
}
