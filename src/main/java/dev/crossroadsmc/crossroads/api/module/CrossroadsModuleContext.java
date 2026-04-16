package dev.crossroadsmc.crossroads.api.module;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.service.KitService;
import dev.crossroadsmc.crossroads.service.ModerationService;
import dev.crossroadsmc.crossroads.service.PlayerDataService;
import dev.crossroadsmc.crossroads.service.SpawnService;
import dev.crossroadsmc.crossroads.service.WarpService;
import java.util.logging.Logger;

public final class CrossroadsModuleContext {
    private final CrossroadsPlugin plugin;

    public CrossroadsModuleContext(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    public CrossroadsPlugin getPlugin() {
        return plugin;
    }

    public Logger getLogger() {
        return plugin.getLogger();
    }

    public PlayerDataService getPlayerDataService() {
        return plugin.getPlayerDataService();
    }

    public WarpService getWarpService() {
        return plugin.getWarpService();
    }

    public SpawnService getSpawnService() {
        return plugin.getSpawnService();
    }

    public KitService getKitService() {
        return plugin.getKitService();
    }

    public ModerationService getModerationService() {
        return plugin.getModerationService();
    }
}
