package dev.crossroadsmc.crossroads;

import dev.crossroadsmc.crossroads.api.CrossroadsAPI;
import dev.crossroadsmc.crossroads.command.CrossroadsCommandRouter;
import dev.crossroadsmc.crossroads.listener.InteractionListener;
import dev.crossroadsmc.crossroads.listener.ModerationListener;
import dev.crossroadsmc.crossroads.listener.PlayerLifecycleListener;
import dev.crossroadsmc.crossroads.listener.PlayerTrackingListener;
import dev.crossroadsmc.crossroads.service.AegisGuardHookService;
import dev.crossroadsmc.crossroads.service.BackService;
import dev.crossroadsmc.crossroads.service.BackupService;
import dev.crossroadsmc.crossroads.service.EconomyService;
import dev.crossroadsmc.crossroads.service.ImportService;
import dev.crossroadsmc.crossroads.service.JailService;
import dev.crossroadsmc.crossroads.service.KitService;
import dev.crossroadsmc.crossroads.service.MenuService;
import dev.crossroadsmc.crossroads.service.MessagingService;
import dev.crossroadsmc.crossroads.service.ModerationService;
import dev.crossroadsmc.crossroads.service.ModuleManager;
import dev.crossroadsmc.crossroads.service.PlayerDataService;
import dev.crossroadsmc.crossroads.service.SpawnService;
import dev.crossroadsmc.crossroads.service.StaffService;
import dev.crossroadsmc.crossroads.service.TeleportRequestService;
import dev.crossroadsmc.crossroads.service.TextPageService;
import dev.crossroadsmc.crossroads.service.WarpService;
import dev.crossroadsmc.crossroads.service.WelcomeService;
import dev.crossroadsmc.crossroads.service.WorldProfileService;
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
    private WorldProfileService worldProfileService;
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
    private JailService jailService;
    private TeleportRequestService teleportRequestService;
    private TextPageService textPageService;
    private AegisGuardHookService aegisGuardHookService;
    private EconomyService economyService;
    private MenuService menuService;
    private ImportService importService;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("kits.yml", false);
        saveResource("motd.yml", false);
        saveResource("help.yml", false);
        saveResource("info.yml", false);

        this.storageManager = new StorageManager(this);
        storageManager.initialize();

        this.playerDataService = new PlayerDataService(this);
        this.worldProfileService = new WorldProfileService(this);
        this.warpService = new WarpService(this);
        this.spawnService = new SpawnService(this);
        this.backService = new BackService(this);
        this.staffService = new StaffService(this);
        this.jailService = new JailService(this);
        this.teleportRequestService = new TeleportRequestService(this);
        this.moderationService = new ModerationService(this, playerDataService, jailService);
        this.messagingService = new MessagingService(this, playerDataService, staffService, moderationService);
        this.kitService = new KitService(this);
        this.backupService = new BackupService(this);
        this.welcomeService = new WelcomeService(this);
        this.textPageService = new TextPageService(this);
        this.aegisGuardHookService = new AegisGuardHookService(this);
        this.economyService = new EconomyService(this);
        this.menuService = new MenuService(this);
        this.importService = new ImportService(this);
        this.moduleManager = new ModuleManager(this);

        registerCommands();
        registerListeners();
        if (isFeatureEnabled("modules")) {
            moduleManager.loadModules();
        }
        registerPlaceholderExpansion();
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
        worldProfileService.reload();
        kitService.reload();
        warpService.reload();
        spawnService.reload();
        jailService.reload();
        textPageService.reload();
        aegisGuardHookService.reload();
        economyService.reload();
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

    public WorldProfileService getWorldProfileService() {
        return worldProfileService;
    }

    public JailService getJailService() {
        return jailService;
    }

    public TeleportRequestService getTeleportRequestService() {
        return teleportRequestService;
    }

    public TextPageService getTextPageService() {
        return textPageService;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public AegisGuardHookService getAegisGuardHookService() {
        return aegisGuardHookService;
    }

    public MenuService getMenuService() {
        return menuService;
    }

    public ImportService getImportService() {
        return importService;
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
            "msg", "reply", "ignore", "mail",
            "tpa", "tpahere", "tpaccept", "tpdeny", "tpacancel", "rtp",
            "fly", "vanish", "staffmode", "socialspy",
            "invsee", "endersee", "freeze", "unfreeze", "mute", "unmute", "warn", "stafflog", "history", "seen",
            "kick", "tempban", "unban", "jail", "unjail", "setjail", "shadowmute", "staffnote",
            "kit", "rules", "motd", "help", "info", "nick", "crossroads"
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
        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);
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

        if (jailService != null) {
            jailService.save();
        }
    }

    private void registerPlaceholderExpansion() {
        if (!isFeatureEnabled("placeholders")) {
            return;
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        try {
            new dev.crossroadsmc.crossroads.placeholder.CrossroadsPlaceholderExpansion(this).register();
        } catch (Exception exception) {
            getLogger().log(Level.WARNING, "Unable to register PlaceholderAPI expansion.", exception);
        }
    }
}
