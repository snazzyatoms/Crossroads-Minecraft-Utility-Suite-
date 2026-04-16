package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.model.SavedLocation;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class BackService {
    private final Map<UUID, SavedLocation> lastLocations = new ConcurrentHashMap<>();

    public void record(Player player, Location from) {
        if (from == null || from.getWorld() == null) {
            return;
        }
        lastLocations.put(player.getUniqueId(), SavedLocation.fromLocation(from));
    }

    public SavedLocation get(Player player) {
        return lastLocations.get(player.getUniqueId());
    }
}
