package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.KitDefinition;
import dev.crossroadsmc.crossroads.util.Chat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class MenuService {
    public static final String WARP_MENU = "warp";
    public static final String KIT_MENU = "kit";

    private final CrossroadsPlugin plugin;

    public MenuService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    public void openWarpMenu(Player player) {
        String profile = plugin.getWorldProfileService().resolveProfile(player.getWorld());
        List<String> warps = new ArrayList<>(plugin.getWarpService().getAvailableWarpNames(profile));
        if (warps.isEmpty()) {
            Chat.send(plugin, player, "<subtle>No warps are available from this profile.");
            return;
        }

        int size = Math.min(54, Math.max(9, ((warps.size() - 1) / 9 + 1) * 9));
        CrossroadsMenuHolder holder = new CrossroadsMenuHolder(WARP_MENU);
        Inventory inventory = Bukkit.createInventory(holder, size, "Crossroads Warps");
        holder.setInventory(inventory);

        for (int index = 0; index < warps.size() && index < size; index++) {
            String warp = warps.get(index);
            holder.setEntry(index, warp);
            inventory.setItem(index, buildItem(Material.ENDER_PEARL, "<accent>" + warp, List.of(
                "<subtle>Profile: <text>" + profile,
                "<success>Click to teleport"
            )));
        }

        player.openInventory(inventory);
    }

    public void openKitMenu(Player player) {
        String profile = plugin.getWorldProfileService().resolveProfile(player.getWorld());
        List<KitDefinition> kits = plugin.getKitService().getAvailableKits(player, profile).stream().toList();
        if (kits.isEmpty()) {
            Chat.send(plugin, player, "<subtle>No kits are available from this profile.");
            return;
        }

        int size = Math.min(54, Math.max(9, ((kits.size() - 1) / 9 + 1) * 9));
        CrossroadsMenuHolder holder = new CrossroadsMenuHolder(KIT_MENU);
        Inventory inventory = Bukkit.createInventory(holder, size, "Crossroads Kits");
        holder.setInventory(inventory);

        for (int index = 0; index < kits.size() && index < size; index++) {
            KitDefinition kit = kits.get(index);
            holder.setEntry(index, kit.getKey());
            Material icon = kit.getItems().isEmpty() ? Material.CHEST : kit.getItems().get(0).getType();
            List<String> lore = new ArrayList<>();
            lore.add("<subtle>Profile: <text>" + profile);
            lore.add("<subtle>Cooldown: <text>" + (kit.getCooldownSeconds() <= 0 ? "none" : kit.getCooldownSeconds() + "s"));
            if (kit.getCost() > 0.0D) {
                lore.add("<subtle>Cost: <text>" + plugin.getEconomyService().format(kit.getCost()));
            }
            lore.add("<success>Click to claim");
            inventory.setItem(index, buildItem(icon, "<accent>" + kit.getDisplayName(), lore));
        }

        player.openInventory(inventory);
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.color(plugin, name));
            meta.setLore(lore.stream().map(line -> Chat.color(plugin, line)).toList());
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final class CrossroadsMenuHolder implements InventoryHolder {
        private final String type;
        private final java.util.Map<Integer, String> entries = new java.util.HashMap<>();
        private Inventory inventory;

        public CrossroadsMenuHolder(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public void setEntry(int slot, String value) {
            entries.put(slot, value);
        }

        public String getEntry(int slot) {
            return entries.get(slot);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
