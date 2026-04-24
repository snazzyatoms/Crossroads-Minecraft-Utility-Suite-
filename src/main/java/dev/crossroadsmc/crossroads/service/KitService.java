package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.KitDefinition;
import dev.crossroadsmc.crossroads.util.Chat;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

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
                List<ItemStack> items = parseItems(kitSection);
                List<String> commands = kitSection.getStringList("commands");
                Set<String> allowedProfiles = normalizeList(kitSection.getStringList("world-profiles"));
                if (allowedProfiles.isEmpty()) {
                    allowedProfiles = normalizeList(kitSection.getStringList("worlds"));
                }
                Set<String> blockedProfiles = normalizeList(kitSection.getStringList("disabled-world-profiles"));
                if (blockedProfiles.isEmpty()) {
                    blockedProfiles = normalizeList(kitSection.getStringList("disabled-worlds"));
                }
                kits.put(key.toLowerCase(), new KitDefinition(
                    key.toLowerCase(),
                    kitSection.getString("display-name", key),
                    kitSection.getString("permission", ""),
                    kitSection.getLong("cooldown-seconds", 0L),
                    kitSection.getDouble("cost", 0.0D),
                    allowedProfiles,
                    blockedProfiles,
                    items,
                    commands
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

    public Collection<KitDefinition> getAvailableKits(org.bukkit.entity.Player player, String profile) {
        return kits.values().stream()
            .filter(kit -> (kit.getPermission() == null || kit.getPermission().isBlank() || player.hasPermission(kit.getPermission())))
            .filter(kit -> kit.isAvailableIn(profile))
            .toList();
    }

    private List<ItemStack> parseItems(ConfigurationSection kitSection) {
        List<ItemStack> items = new ArrayList<>();
        ConfigurationSection itemsSection = kitSection.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ItemStack stack = parseItem(itemsSection.getConfigurationSection(key));
                if (stack != null) {
                    items.add(stack);
                }
            }
            return items;
        }

        for (String line : kitSection.getStringList("items")) {
            ItemStack item = parseLegacyItem(line);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private ItemStack parseItem(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        Material material = Material.matchMaterial(section.getString("material", ""));
        if (material == null) {
            plugin.getLogger().warning("Unknown material in kits.yml: " + section.getString("material", ""));
            return null;
        }

        ItemStack item = new ItemStack(material, Math.max(1, section.getInt("amount", 1)));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (section.isString("name")) {
            meta.setDisplayName(Chat.color(plugin, section.getString("name", "")));
        }

        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            meta.setLore(Chat.colorize(lore));
        }

        if (section.contains("custom-model-data")) {
            meta.setCustomModelData(section.getInt("custom-model-data"));
        }

        meta.setUnbreakable(section.getBoolean("unbreakable", false));

        for (String rawFlag : section.getStringList("flags")) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(rawFlag.toUpperCase()));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Unknown item flag in kits.yml: " + rawFlag);
            }
        }

        ConfigurationSection enchantments = section.getConfigurationSection("enchants");
        if (enchantments != null) {
            for (String key : enchantments.getKeys(false)) {
                Enchantment enchantment = resolveEnchantment(key);
                if (enchantment == null) {
                    plugin.getLogger().warning("Unknown enchantment in kits.yml: " + key);
                    continue;
                }
                meta.addEnchant(enchantment, enchantments.getInt(key, 1), true);
            }
        }

        if (meta instanceof Damageable damageable && section.contains("damage")) {
            damageable.setDamage(section.getInt("damage"));
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack parseLegacyItem(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return null;
        }

        Material material = Material.matchMaterial(parts[0].toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("Unknown material in kits.yml: " + parts[0]);
            return null;
        }

        int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        return new ItemStack(material, Math.max(1, amount));
    }

    private Enchantment resolveEnchantment(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return null;
        }

        String normalized = normalizeEnchantmentKey(rawKey);
        NamespacedKey namespacedKey = normalized.contains(":")
            ? NamespacedKey.fromString(normalized)
            : NamespacedKey.minecraft(normalized);
        if (namespacedKey != null) {
            Enchantment enchantment = Enchantment.getByKey(namespacedKey);
            if (enchantment != null) {
                return enchantment;
            }
        }

        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment.getKey().getKey().equalsIgnoreCase(normalized)) {
                return enchantment;
            }
        }
        return null;
    }

    private String normalizeEnchantmentKey(String rawKey) {
        String normalized = rawKey.trim().toLowerCase().replace(' ', '_');
        return switch (normalized) {
            case "protection_environmental" -> "protection";
            case "protection_fire" -> "fire_protection";
            case "protection_fall" -> "feather_falling";
            case "protection_explosions" -> "blast_protection";
            case "protection_projectiles" -> "projectile_protection";
            case "oxygen" -> "respiration";
            case "water_worker" -> "aqua_affinity";
            case "damage_all" -> "sharpness";
            case "damage_undead" -> "smite";
            case "damage_arthropods" -> "bane_of_arthropods";
            case "loot_bonus_mobs" -> "looting";
            case "dig_speed" -> "efficiency";
            case "durability" -> "unbreaking";
            case "loot_bonus_blocks" -> "fortune";
            case "arrow_damage" -> "power";
            case "arrow_knockback" -> "punch";
            case "arrow_fire" -> "flame";
            case "arrow_infinite" -> "infinity";
            default -> normalized;
        };
    }

    private Set<String> normalizeList(List<String> values) {
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.toLowerCase());
            }
        }
        return normalized;
    }
}
