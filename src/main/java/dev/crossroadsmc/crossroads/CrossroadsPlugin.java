package dev.crossroadsmc.crossroads;

import dev.crossroadsmc.crossroads.api.CrossroadsAPI;
import dev.crossroadsmc.crossroads.command.CrossroadsCommandRouter;
import dev.crossroadsmc.crossroads.listener.ModerationListener;
import dev.crossroadsmc.crossroads.listener.PlayerLifecycleListener;
import dev.crossroadsmc.crossroads.listener.PlayerTrackingListener;
import dev.crossroadsmc.crossroads.service.BackService;
import dev.crossroadsmc.crossroads.service.BackupService;
import dev.crossroadsmc.crossroads.service.KitService;
import dev.crossroadsmc.crossroads.service.MessagingService;
import dev.crossroadsmc.crossroads.service.ModerationService;
import dev.crossroadsmc.crossroads.service.ModuleManager;
import dev.crossroadsmc.crossroads.service.PlayerDataService;
import dev.crossroadsmc.crossroads.service.SpawnService;
import dev.crossroadsmc.crossroads.service.StaffService;
import dev.crossroadsmc.crossroads.service.WarpService;
import dev.crossroadsmc.crossroads.service.WelcomeService;
import dev.crossroadsmc.crossroads.storage.StorageManager;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class CrossroadsPlugin extends JavaPlugin {
    private StorageManager storageManager;
    private PlayerDataService playerDataService;
    private WarpService warpService;
    private SpawnService spawnService;
    private BackService backService;
    private MessagingService messagingService;
    private StaffService staffService;
    private KitService kitService;
    private BackupService backupService;
    private ModerationService moderationService;
    private WelcomeService welcomeService;
    private ModuleManager moduleManager;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("kits.yml", false);

        this.storageManager = new StorageManager(this);
        storageManager.initialize();

        this.playerDataService = new PlayerDataService(this);
        this.warpService = new WarpService(this);
        this.spawnService = new SpawnService(this);
        this.backService = new BackService();
        this.staffService = new StaffService(this);
        this.moderationService = new ModerationService(this, playerDataService);
        this.messagingService = new MessagingService(this, playerDataService, staffService, moderationService);
        this.kitService = new KitService(this);
        this.backupService = new BackupService(this);
        this.welcomeService = new WelcomeService(this);
        this.moduleManager = new ModuleManager(this);

        registerCommands();
        registerListeners();
        if (isFeatureEnabled("modules")) {
            moduleManager.loadModules();
        }
        scheduleAutosave();

        CrossroadsAPI.initialize(this);

        if (isFeatureEnabled("backups") && getConfig().getBoolean("backups.auto-on-startup", true)) {
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
            if (moduleManager != null) {
                moduleManager.shutdown();
            }

            if (autosaveTask != null) {
                autosaveTask.cancel();
                autosaveTask = null;
            }

            if (staffService != null) {
                staffService.shutdown();
            }

            flushPersistentState();

            if (isFeatureEnabled("backups") && backupService != null && getConfig().getBoolean("backups.auto-on-shutdown", true)) {
                backupService.createBackup("shutdown");
            }
        } catch (Exception exception) {
            getLogger().log(Level.WARNING, "Crossroads encountered an issue while shutting down.", exception);
        } finally {
            CrossroadsAPI.shutdown();
            if (storageManager != null) {
                storageManager.shutdown();
            }
        }
    }

    public void reloadCrossroads() {
        reloadConfig();
        kitService.reload();
        warpService.reload();
        spawnService.reload();
        scheduleAutosave();
    }

    public StorageManager getStorageManager() {
        return storageManager;
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

    public ModerationService getModerationService() {
        return moderationService;
    }

    public WelcomeService getWelcomeService() {
        return welcomeService;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public boolean isFeatureEnabled(String featureKey) {
        return getConfig().getBoolean("features." + featureKey, true);
    }

    private void registerCommands() {
        CrossroadsCommandRouter router = new CrossroadsCommandRouter(this);
        List<String> commands = Arrays.asList(
            "home", "sethome", "delhome", "homes",
            "warp", "setwarp", "delwarp", "warps",
            "spawn", "setspawn", "back",
            "msg", "reply", "ignore",
            "fly", "vanish", "staffmode", "socialspy",
            "invsee", "endersee", "freeze", "unfreeze", "mute", "unmute", "warn", "stafflog", "history", "seen",
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
        getServer().getPluginManager().registerEvents(new ModerationListener(this), this);
    }

    private void scheduleAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }

        if (!getConfig().getBoolean("persistence.autosave.enabled", true)) {
            return;
        }

        long intervalSeconds = Math.max(30L, getConfig().getLong("persistence.autosave.interval-seconds", 120L));
        long intervalTicks = intervalSeconds * 20L;
        autosaveTask = getServer().getScheduler().runTaskTimer(this, this::flushPersistentState, intervalTicks, intervalTicks);
    }

    private void flushPersistentState() {
        if (playerDataService != null) {
            playerDataService.saveAll();
        }

        if (warpService != null) {
            warpService.save();
        }

        if (spawnService != null) {
            spawnService.save();
        }
    }
}
