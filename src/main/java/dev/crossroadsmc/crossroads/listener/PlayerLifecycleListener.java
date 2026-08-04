package dev.crossroadsmc.crossroads.listener;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import dev.crossroadsmc.crossroads.util.Chat;
import dev.crossroadsmc.crossroads.util.TimeFormatter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerLifecycleListener implements Listener {
    private final CrossroadsPlugin plugin;

    public PlayerLifecycleListener(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        PlayerData data = plugin.getPlayerDataService().get(event.getUniqueId());
        if (data.isBanned()) {
            long remaining = data.getBannedUntil() == Long.MAX_VALUE
                ? Long.MAX_VALUE
                : Math.max(1L, (data.getBannedUntil() - System.currentTimeMillis()) / 1000L);
            String durationText = remaining == Long.MAX_VALUE
                ? plugin.getLanguageService().get(null, "moderation.tempban.permanent")
                : TimeFormatter.duration(remaining);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                plugin.getLanguageService().colorize(null, "moderation.tempban.message",
                    "%duration%", durationText,
                    "%reason%", data.getBanReason()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataService().get(player);
        if (plugin.getPermissionService() != null) {
            plugin.getPermissionService().ensurePlayerDefaults(data);
            plugin.getPermissionService().applyAttachments(player);
        }
        if (plugin.isFeatureEnabled("moderation")) {
            plugin.getModerationService().updateJoinState(player);
        }
        plugin.getStaffService().applyJoinVisibility(player);
        if (plugin.isFeatureEnabled("welcome")) {
            plugin.getWelcomeService().welcome(player);
        }
        if (!data.getNickname().isBlank()) {
            player.setDisplayName(Chat.color(plugin, data.getNickname()));
            player.setCustomName(Chat.color(plugin, data.getNickname()));
        }
        if (data.getUnreadMailCount() > 0) {
            plugin.getLanguageService().send(player, "social.mail.unread",
                "%count%", String.valueOf(data.getUnreadMailCount()));
        }

        if (data.isJailed()) {
            SavedLocation jail = plugin.getModerationService().getJailLocation(player.getUniqueId());
            Location jailLocation = jail == null ? null : jail.toLocation();
            if (jailLocation != null) {
                player.teleport(jailLocation);
                plugin.getLanguageService().send(player, "moderation.jail.active");
                return;
            }
        }

        if (plugin.isFeatureEnabled("spawn")
            && !player.hasPlayedBefore()
            && plugin.getConfig().getBoolean("spawn.teleport-on-first-join", false)) {
            String profile = plugin.getWorldProfileService().resolveProfile(player.getWorld());
            SavedLocation spawn = plugin.getSpawnService().getSpawn(profile);
            if (spawn != null) {
                Location location = spawn.toLocation();
                if (location != null) {
                    player.teleport(location);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.isFeatureEnabled("moderation")) {
            plugin.getModerationService().updateQuitState(player);
        }
        plugin.getTeleportRequestService().clear(player);
        plugin.getStaffService().removePlayerState(player);
        if (plugin.getPermissionService() != null) {
            plugin.getPermissionService().removeAttachments(player);
        }
        plugin.getPlayerDataService().save(player);
    }
}
