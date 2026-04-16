package dev.crossroadsmc.crossroads.listener;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerLifecycleListener implements Listener {
    private final CrossroadsPlugin plugin;

    public PlayerLifecycleListener(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataService().get(player);
        plugin.getStaffService().applyJoinVisibility(player);

        if (!player.hasPlayedBefore() && plugin.getConfig().getBoolean("spawn.teleport-on-first-join", false)) {
            SavedLocation spawn = plugin.getSpawnService().getSpawn();
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
        plugin.getStaffService().removePlayerState(player);
        plugin.getPlayerDataService().save(player);
    }
}
