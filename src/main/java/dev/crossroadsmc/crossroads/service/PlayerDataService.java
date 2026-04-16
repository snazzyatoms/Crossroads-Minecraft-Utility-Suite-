package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class PlayerDataService {
    private final CrossroadsPlugin plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    public PlayerData get(Player player) {
        return get(player.getUniqueId());
    }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, ignored -> plugin.getStorageManager().getProvider().loadPlayerData(uuid));
    }

    public void save(Player player) {
        save(player.getUniqueId());
    }

    public void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            plugin.getStorageManager().getProvider().savePlayerData(data);
        }
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            save(uuid);
        }
    }
}
