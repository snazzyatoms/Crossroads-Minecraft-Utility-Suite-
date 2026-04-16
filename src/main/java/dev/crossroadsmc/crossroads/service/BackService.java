package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class BackService {
    private final CrossroadsPlugin plugin;

    public BackService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    public void record(Player player, Location from) {
        if (from == null || from.getWorld() == null) {
            return;
        }
        PlayerData data = plugin.getPlayerDataService().get(player);
        data.setBackLocation(SavedLocation.fromLocation(from));
        plugin.getPlayerDataService().save(player);
    }

    public SavedLocation get(Player player) {
        return plugin.getPlayerDataService().get(player).getBackLocation();
    }
}
