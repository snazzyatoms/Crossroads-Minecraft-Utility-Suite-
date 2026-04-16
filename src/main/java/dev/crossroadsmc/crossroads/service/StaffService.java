package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.InventorySnapshot;
import dev.crossroadsmc.crossroads.model.StaffModeSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

public final class StaffService {
    private final CrossroadsPlugin plugin;
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> flyingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, StaffModeSession> staffModeSessions = new HashMap<>();

    public StaffService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    public boolean isFlying(Player player) {
        return flyingPlayers.contains(player.getUniqueId());
    }

    public boolean isInStaffMode(Player player) {
        return staffModeSessions.containsKey(player.getUniqueId());
    }

    public boolean toggleVanish(Player player) {
        if (isVanished(player)) {
            vanishedPlayers.remove(player.getUniqueId());
            for (Player other : plugin.getServer().getOnlinePlayers()) {
                other.showPlayer(plugin, player);
            }
            return false;
        }

        vanishedPlayers.add(player.getUniqueId());
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (!other.equals(player) && !other.hasPermission("crossroads.staff.see")) {
                other.hidePlayer(plugin, player);
            }
        }
        return true;
    }

    public boolean toggleFly(Player player) {
        if (isFlying(player)) {
            flyingPlayers.remove(player.getUniqueId());
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
            return false;
        }

        flyingPlayers.add(player.getUniqueId());
        player.setAllowFlight(true);
        player.setFlying(true);
        return true;
    }

    public boolean toggleStaffMode(Player player) {
        if (isInStaffMode(player)) {
            disableStaffMode(player);
            return false;
        }

        boolean vanishedBefore = isVanished(player);
        boolean flyBefore = isFlying(player);

        InventorySnapshot snapshot = new InventorySnapshot(
            player.getInventory().getContents().clone(),
            player.getInventory().getArmorContents().clone(),
            player.getInventory().getItemInOffHand(),
            player.getGameMode(),
            player.getAllowFlight(),
            player.isFlying(),
            player.getExp(),
            player.getLevel(),
            player.getLocation().clone()
        );

        staffModeSessions.put(player.getUniqueId(), new StaffModeSession(snapshot, vanishedBefore, flyBefore));

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.setExp(0f);
        player.setLevel(0);
        player.setGameMode(GameMode.CREATIVE);

        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Inspector Compass");
            compass.setItemMeta(meta);
        }

        player.getInventory().addItem(compass);
        player.getInventory().addItem(new ItemStack(Material.BOOK, 1));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
        player.updateInventory();

        if (!vanishedBefore) {
            toggleVanish(player);
        }
        if (!flyBefore) {
            toggleFly(player);
        }

        return true;
    }

    public void disableStaffMode(Player player) {
        StaffModeSession session = staffModeSessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        InventorySnapshot snapshot = session.getSnapshot();
        player.getInventory().setContents(snapshot.getContents());
        player.getInventory().setArmorContents(snapshot.getArmor());
        player.getInventory().setItemInOffHand(snapshot.getOffHand());
        player.setGameMode(snapshot.getGameMode());
        player.setAllowFlight(snapshot.isAllowFlight());
        player.setFlying(snapshot.isFlying());
        player.setExp(snapshot.getExperience());
        player.setLevel(snapshot.getLevel());

        if (snapshot.getLocation() != null && snapshot.getLocation().getWorld() != null) {
            player.teleport(snapshot.getLocation());
        }

        if (!session.wasVanishedBefore() && isVanished(player)) {
            toggleVanish(player);
        }
        if (!session.wasFlyBefore() && isFlying(player)) {
            toggleFly(player);
        }
    }

    public void applyJoinVisibility(Player joiningPlayer) {
        for (UUID vanishedId : vanishedPlayers) {
            Player vanished = plugin.getServer().getPlayer(vanishedId);
            if (vanished != null && !joiningPlayer.hasPermission("crossroads.staff.see")) {
                joiningPlayer.hidePlayer(plugin, vanished);
            }
        }
    }

    public void removePlayerState(Player player) {
        if (isInStaffMode(player)) {
            disableStaffMode(player);
        }
        if (isVanished(player)) {
            toggleVanish(player);
        }
        flyingPlayers.remove(player.getUniqueId());
    }

    public void shutdown() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isInStaffMode(player)) {
                disableStaffMode(player);
            } else if (isVanished(player)) {
                toggleVanish(player);
            }
        }
        vanishedPlayers.clear();
        flyingPlayers.clear();
        staffModeSessions.clear();
    }
}
