package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ImportService {
    private final CrossroadsPlugin plugin;

    public ImportService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    public ImportResult importEssentials() {
        File essentialsFolder = new File(plugin.getDataFolder().getParentFile(), "Essentials");
        if (!essentialsFolder.exists()) {
            return new ImportResult(0, 0, 0, false);
        }

        AtomicInteger homes = new AtomicInteger();
        AtomicInteger warps = new AtomicInteger();
        AtomicInteger nicknames = new AtomicInteger();

        File userdataFolder = new File(essentialsFolder, "userdata");
        File[] playerFiles = userdataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles != null) {
            for (File file : playerFiles) {
                try {
                    java.util.UUID uuid = java.util.UUID.fromString(file.getName().replace(".yml", ""));
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    PlayerData data = plugin.getPlayerDataService().get(uuid);

                    ConfigurationSection homesSection = yaml.getConfigurationSection("homes");
                    if (homesSection != null) {
                        for (String key : homesSection.getKeys(false)) {
                            SavedLocation location = SavedLocation.fromSection(homesSection.getConfigurationSection(key));
                            if (location != null) {
                                data.setHome(PlayerData.GLOBAL_SCOPE, key, location);
                                homes.incrementAndGet();
                            }
                        }
                    }

                    String nickname = yaml.getString("nickname", "");
                    if (!nickname.isBlank()) {
                        data.setNickname(nickname);
                        nicknames.incrementAndGet();
                    }
                    plugin.getPlayerDataService().save(uuid);
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed userdata files.
                }
            }
        }

        File warpsFolder = new File(essentialsFolder, "warps");
        File[] warpFiles = warpsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (warpFiles != null) {
            for (File file : warpFiles) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                SavedLocation location = SavedLocation.fromSection(yaml);
                if (location != null) {
                    plugin.getWarpService().setWarp(PlayerData.GLOBAL_SCOPE, file.getName().replace(".yml", ""), location);
                    warps.incrementAndGet();
                }
            }
        }

        File spawnFile = new File(essentialsFolder, "spawn.yml");
        if (spawnFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(spawnFile);
            SavedLocation location = SavedLocation.fromSection(yaml);
            if (location != null) {
                plugin.getSpawnService().setSpawn(PlayerData.GLOBAL_SCOPE, location);
            }
        }

        return new ImportResult(homes.get(), warps.get(), nicknames.get(), true);
    }

    public record ImportResult(int homes, int warps, int nicknames, boolean found) {
    }
}
