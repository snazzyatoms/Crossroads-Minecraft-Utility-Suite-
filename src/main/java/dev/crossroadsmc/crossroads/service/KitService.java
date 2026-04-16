package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.KitDefinition;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public final class KitService {
    private final CrossroadsPlugin plugin;
    private final File file;
    private final Map<String, KitDefinition> kits = new LinkedHashMap<>();

    public KitService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
        reload();
    }

    public void reload() {
        kits.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("kits");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection kitSection = section.getConfigurationSection(key);
            if (kitSection == null) {
                continue;
            }

            try {
                List<ItemStack> items = kitSection.getStringList("items").stream()
                    .map(this::parseItem)
                    .filter(item -> item != null)
                    .toList();

                kits.put(key.toLowerCase(), new KitDefinition(
                    key.toLowerCase(),
                    kitSection.getString("display-name", key),
                    kitSection.getString("permission", ""),
                    kitSection.getLong("cooldown-seconds", 0L),
                    items
                ));
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Unable to parse kit '" + key + "'.", exception);
            }
        }
    }

    public Collection<KitDefinition> getKits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public KitDefinition getKit(String key) {
        return kits.get(key.toLowerCase());
    }

    private ItemStack parseItem(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return null;
        }

        Material material = Material.matchMaterial(parts[0].toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("Unknown material in kits.yml: " + parts[0]);
            return null;
        }

        int amount = 1;
        if (parts.length > 1) {
            amount = Integer.parseInt(parts[1]);
        }

        return new ItemStack(material, Math.max(1, amount));
    }
}
