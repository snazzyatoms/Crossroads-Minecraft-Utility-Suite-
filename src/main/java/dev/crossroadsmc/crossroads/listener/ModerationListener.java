package dev.crossroadsmc.crossroads.listener;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.util.Chat;
import dev.crossroadsmc.crossroads.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class ModerationListener implements Listener {
    private final CrossroadsPlugin plugin;

    public ModerationListener(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.isFeatureEnabled("moderation")) {
            return;
        }

        PlayerData data = plugin.getModerationService().getPlayerData(event.getPlayer().getUniqueId());
        if (!data.isMuted()) {
            return;
        }

        event.setCancelled(true);
        long remaining = Math.max(1L, (data.getMutedUntil() - System.currentTimeMillis()) / 1000L);
        Bukkit.getScheduler().runTask(plugin, () -> Chat.send(plugin, event.getPlayer(),
            "<error>You are muted for another <warn>" + TimeFormatter.duration(remaining) + "<error>. Reason: <text>" + data.getMuteReason()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.isFeatureEnabled("moderation")) {
            return;
        }

        if (!plugin.getModerationService().isFrozen(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        event.setTo(event.getFrom());
        String reason = plugin.getModerationService().getPlayerData(event.getPlayer().getUniqueId()).getFreezeReason();
        Chat.send(plugin, event.getPlayer(), "<error>You are frozen. <subtle>" + reason);
    }
}
