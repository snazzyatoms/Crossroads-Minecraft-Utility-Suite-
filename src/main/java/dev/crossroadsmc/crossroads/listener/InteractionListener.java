package dev.crossroadsmc.crossroads.listener;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.KitDefinition;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import dev.crossroadsmc.crossroads.service.MenuService;
import dev.crossroadsmc.crossroads.service.MenuService.CrossroadsMenuHolder;
import dev.crossroadsmc.crossroads.util.Chat;
import dev.crossroadsmc.crossroads.util.TimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class InteractionListener implements Listener {
    private final CrossroadsPlugin plugin;

    public InteractionListener(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof CrossroadsMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        String entry = holder.getEntry(event.getRawSlot());
        if (entry == null) {
            return;
        }

        if (holder.getType().equalsIgnoreCase(MenuService.WARP_MENU)) {
            String profile = plugin.getWorldProfileService().resolveProfile(player.getWorld());
            SavedLocation warp = plugin.getWarpService().getWarp(profile, entry);
            if (warp == null || warp.toLocation() == null) {
                Chat.send(plugin, player, "<error>That warp is no longer available.");
                player.closeInventory();
                return;
            }
            plugin.getBackService().record(player, player.getLocation());
            player.teleport(warp.toLocation());
            Chat.send(plugin, player, "<success>Warped to <warn>" + entry + "<success>.");
            player.closeInventory();
            return;
        }

        if (holder.getType().equalsIgnoreCase(MenuService.KIT_MENU)) {
            String profile = plugin.getWorldProfileService().resolveProfile(player.getWorld());
            KitDefinition kit = plugin.getKitService().getKit(entry);
            if (kit == null || !kit.isAvailableIn(profile)) {
                Chat.send(plugin, player, "<error>That kit is no longer available.");
                player.closeInventory();
                return;
            }

            PlayerData data = plugin.getPlayerDataService().get(player);
            long now = System.currentTimeMillis();
            long nextUse = data.getKitCooldown(profile + ":" + kit.getKey());
            if (nextUse > now) {
                long remaining = Math.max(1L, (nextUse - now) / 1000L);
                Chat.send(plugin, player, "<error>That kit is on cooldown for another <warn>" + TimeFormatter.duration(remaining) + "<error>.");
                return;
            }
            if (kit.getCost() > 0.0D) {
                String failure = plugin.getEconomyService().charge(player, kit.getCost(), "Crossroads kit " + kit.getKey());
                if (failure != null) {
                    Chat.send(plugin, player, "<error>" + failure);
                    return;
                }
            }

            for (ItemStack item : kit.getItems()) {
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
                leftovers.values().forEach(leftover -> Objects.requireNonNull(player.getWorld()).dropItemNaturally(player.getLocation(), leftover));
            }
            for (String command : kit.getCommands()) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command.replace("%player%", player.getName()));
            }
            if (kit.getCooldownSeconds() > 0L) {
                data.setKitCooldown(profile + ":" + kit.getKey(), now + (kit.getCooldownSeconds() * 1000L));
                plugin.getPlayerDataService().save(player);
            }
            Chat.send(plugin, player, "<success>Claimed kit <warn>" + kit.getDisplayName() + "<success>.");
            player.closeInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignCreate(SignChangeEvent event) {
        if (!plugin.isFeatureEnabled("signs")) {
            return;
        }
        if (!event.getPlayer().hasPermission("crossroads.admin")) {
            return;
        }
        String header = event.getLine(0);
        if (header == null || !header.equalsIgnoreCase("[crossroads]")) {
            return;
        }

        event.setLine(0, Chat.color(plugin, "<accent>[Crossroads]"));
        event.getPlayer().sendMessage(Chat.color(plugin, "<success>Crossroads sign created."));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignUse(PlayerInteractEvent event) {
        if (!plugin.isFeatureEnabled("signs")) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) {
            return;
        }
        String header = sign.getLine(0);
        if (header == null || !Chat.color(plugin, header).contains("[Crossroads]")) {
            return;
        }

        Player player = event.getPlayer();
        String action = sign.getLine(1).trim().toLowerCase(Locale.ROOT);
        String value = sign.getLine(2).trim();

        switch (action) {
            case "warp" -> {
                String profile = plugin.getWorldProfileService().resolveProfile(player.getWorld());
                SavedLocation warp = plugin.getWarpService().getWarp(profile, value);
                if (warp == null || warp.toLocation() == null) {
                    Chat.send(plugin, player, "<error>That warp sign points nowhere.");
                    return;
                }
                plugin.getBackService().record(player, player.getLocation());
                player.teleport(warp.toLocation());
                Chat.send(plugin, player, "<success>Warped to <warn>" + value + "<success>.");
            }
            case "kit" -> plugin.getMenuService().openKitMenu(player);
            case "spawn" -> {
                String profile = plugin.getWorldProfileService().resolveProfile(player.getWorld());
                SavedLocation spawn = plugin.getSpawnService().getSpawn(profile);
                if (spawn == null || spawn.toLocation() == null) {
                    Chat.send(plugin, player, "<error>No spawn is available here.");
                    return;
                }
                plugin.getBackService().record(player, player.getLocation());
                player.teleport(spawn.toLocation());
                Chat.send(plugin, player, "<success>Teleported to spawn.");
            }
            case "rtp" -> player.performCommand("rtp");
            case "motd" -> player.performCommand("motd");
            case "help" -> player.performCommand("help");
            case "info" -> player.performCommand("info");
            default -> Chat.send(plugin, player, "<error>That sign action is not supported.");
        }
    }
}
