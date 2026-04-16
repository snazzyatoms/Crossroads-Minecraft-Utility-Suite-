package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class PlayerDataService {
    private final CrossroadsPlugin plugin;
    private final File playersDirectory;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        this.playersDirectory = new File(plugin.getDataFolder(), "players");
        if (!playersDirectory.exists() && !playersDirectory.mkdirs()) {
            plugin.getLogger().warning("Unable to create players data directory.");
        }
    }

    public PlayerData get(Player player) {
        return get(player.getUniqueId());
    }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public void save(Player player) {
        save(player.getUniqueId());
    }

    public void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) {
            return;
        }

        File file = getFile(uuid);
        YamlConfiguration yaml = new YamlConfiguration();

        ConfigurationSection homesSection = yaml.createSection("homes");
        for (Map.Entry<String, SavedLocation> entry : data.getHomes().entrySet()) {
            ConfigurationSection section = homesSection.createSection(entry.getKey());
            entry.getValue().write(section);
        }

        yaml.set("ignored", data.getIgnoredPlayers().stream().map(UUID::toString).toList());

        ConfigurationSection kitsSection = yaml.createSection("kits");
        for (Map.Entry<String, Long> entry : data.getKitCooldowns().entrySet()) {
            kitsSection.set(entry.getKey(), entry.getValue());
        }

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to save player data for " + uuid + ".", exception);
        }
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            save(uuid);
        }
    }

    private PlayerData load(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        File file = getFile(uuid);
        if (!file.exists()) {
            return data;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection homesSection = yaml.getConfigurationSection("homes");
        if (homesSection != null) {
            Map<String, SavedLocation> homes = new HashMap<>();
            for (String key : homesSection.getKeys(false)) {
                SavedLocation location = SavedLocation.fromSection(homesSection.getConfigurationSection(key));
                if (location != null) {
                    homes.put(key.toLowerCase(), location);
                }
            }
            data.importHomes(homes);
        }

        Set<UUID> ignored = new HashSet<>();
        for (String entry : yaml.getStringList("ignored")) {
            try {
                ignored.add(UUID.fromString(entry));
            } catch (IllegalArgumentException ignoredException) {
                plugin.getLogger().warning("Skipping invalid ignored UUID entry: " + entry);
            }
        }
        data.importIgnoredPlayers(ignored);

        ConfigurationSection kitsSection = yaml.getConfigurationSection("kits");
        if (kitsSection != null) {
            Map<String, Long> cooldowns = new HashMap<>();
            for (String key : kitsSection.getKeys(false)) {
                cooldowns.put(key.toLowerCase(), kitsSection.getLong(key));
            }
            data.importKitCooldowns(cooldowns);
        }

        return data;
    }

    private File getFile(UUID uuid) {
        return new File(playersDirectory, uuid + ".yml");
    }
}
