package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.World;

public final class WorldProfileService {
    private final CrossroadsPlugin plugin;
    private final Map<String, String> aliases = new HashMap<>();

    public WorldProfileService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        aliases.clear();
        if (plugin.getConfig().getConfigurationSection("world-profiles.aliases") == null) {
            return;
        }

        for (String profile : plugin.getConfig().getConfigurationSection("world-profiles.aliases").getKeys(false)) {
            for (String worldName : plugin.getConfig().getStringList("world-profiles.aliases." + profile)) {
                aliases.put(worldName.toLowerCase(), profile.toLowerCase());
            }
        }
    }

    public String resolveProfile(World world) {
        return resolveProfile(world == null ? null : world.getName());
    }

    public String resolveProfile(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return "global";
        }
        return aliases.getOrDefault(worldName.toLowerCase(), worldName.toLowerCase());
    }

    public String normalizeProfile(String profile) {
        if (profile == null || profile.isBlank()) {
            return "global";
        }
        return profile.toLowerCase();
    }

    public Set<String> getConfiguredProfiles() {
        return Collections.unmodifiableSet(Set.copyOf(aliases.values()));
    }
}
