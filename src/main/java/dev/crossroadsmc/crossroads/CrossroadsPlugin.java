package dev.crossroadsmc.crossroads;

import dev.crossroadsmc.crossroads.api.CrossroadsAPI;
import dev.crossroadsmc.crossroads.command.CrossroadsCommandRouter;
import dev.crossroadsmc.crossroads.listener.PlayerLifecycleListener;
import dev.crossroadsmc.crossroads.listener.PlayerTrackingListener;
import dev.crossroadsmc.crossroads.service.BackService;
import dev.crossroadsmc.crossroads.service.BackupService;
import dev.crossroadsmc.crossroads.service.KitService;
import dev.crossroadsmc.crossroads.service.MessagingService;
import dev.crossroadsmc.crossroads.service.PlayerDataService;
import dev.crossroadsmc.crossroads.service.SpawnService;
import dev.crossroadsmc.crossroads.service.StaffService;
import dev.crossroadsmc.crossroads.service.WarpService;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrossroadsPlugin extends JavaPlugin {
    private PlayerDataService playerDataService;
    private WarpService warpService;
    private SpawnService spawnService;
    private BackService backService;
    private MessagingService messagingService;
    private StaffService staffService;
    private KitService kitService;
    private BackupService backupService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("kits.yml", false);

        this.playerDataService = new PlayerDataService(this);
        this.warpService = new WarpService(this);
        this.spawnService = new SpawnService(this);
        this.backService = new BackService();
        this.messagingService = new MessagingService(this, playerDataService);
        this.staffService = new StaffService(this);
        this.kitService = new KitService(this);
        this.backupService = new BackupService(this);

        registerCommands();
        registerListeners();

        CrossroadsAPI.initialize(this);

        if (getConfig().getBoolean("backups.auto-on-startup", true)) {
            try {
                backupService.createBackup("startup");
            } catch (Exception exception) {
                getLogger().log(Level.WARNING, "Unable to create startup backup.", exception);
            }
        }

        getLogger().info("Crossroads is online and ready to guide your players.");
    }

    @Override
    public void onDisable() {
        try {
            if (staffService != null) {
                staffService.shutdown();
            }

            if (playerDataService != null) {
                playerDataService.saveAll();
            }

            if (warpService != null) {
                warpService.save();
            }

            if (spawnService != null) {
                spawnService.save();
            }

            if (backupService != null && getConfig().getBoolean("backups.auto-on-shutdown", true)) {
                backupService.createBackup("shutdown");
            }
        } catch (Exception exception) {
            getLogger().log(Level.WARNING, "Crossroads encountered an issue while shutting down.", exception);
        } finally {
            CrossroadsAPI.shutdown();
        }
    }

    public void reloadCrossroads() {
        reloadConfig();
        kitService.reload();
        warpService.reload();
        spawnService.reload();
    }

    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }

    public WarpService getWarpService() {
        return warpService;
    }

    public SpawnService getSpawnService() {
        return spawnService;
    }

    public BackService getBackService() {
        return backService;
    }

    public MessagingService getMessagingService() {
        return messagingService;
    }

    public StaffService getStaffService() {
        return staffService;
    }

    public KitService getKitService() {
        return kitService;
    }

    public BackupService getBackupService() {
        return backupService;
    }

    private void registerCommands() {
        CrossroadsCommandRouter router = new CrossroadsCommandRouter(this);
        List<String> commands = Arrays.asList(
            "home", "sethome", "delhome", "homes",
            "warp", "setwarp", "delwarp", "warps",
            "spawn", "setspawn", "back",
            "msg", "reply", "ignore",
            "fly", "vanish", "staffmode",
            "kit", "rules", "crossroads"
        );

        for (String name : commands) {
            PluginCommand command = Objects.requireNonNull(getCommand(name), "Missing command registration for " + name);
            command.setExecutor(router);
            command.setTabCompleter(router);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerTrackingListener(this), this);
    }
}
