package dev.crossroadsmc.crossroads.listener;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import dev.crossroadsmc.crossroads.util.Chat;
import dev.crossroadsmc.crossroads.util.TimeFormatter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerLifecycleListener implements Listener {
    private final CrossroadsPlugin plugin;

    public PlayerLifecycleListener(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        PlayerData data = plugin.getPlayerDataService().get(event.getPlayer());
        if (data.isBanned()) {
            long remaining = data.getBannedUntil() == Long.MAX_VALUE
                ? Long.MAX_VALUE
                : Math.max(1L, (data.getBannedUntil() - System.currentTimeMillis()) / 1000L);
            String durationText = remaining == Long.MAX_VALUE ? "permanently" : TimeFormatter.duration(remaining);
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                Chat.color(plugin, "<error>You are banned <warn>" + durationText + "<error>. Reason: <text>" + data.getBanReason()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataService().get(player);
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
            Chat.send(plugin, player, "<info>You have <warn>" + data.getUnreadMailCount() + "<info> unread mail messages.");
        }

        if (data.isJailed()) {
            SavedLocation jail = plugin.getModerationService().getJailLocation(player.getUniqueId());
            Location jailLocation = jail == null ? null : jail.toLocation();
            if (jailLocation != null) {
                player.teleport(jailLocation);
                Chat.send(plugin, player, "<error>You are currently jailed.");
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
        plugin.getPlayerDataService().save(player);
    }
}
