package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.ModerationLogEntry;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import java.util.List;
import java.util.UUID;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ModerationService {
    private final CrossroadsPlugin plugin;
    private final PlayerDataService playerDataService;
    private final JailService jailService;

    public ModerationService(CrossroadsPlugin plugin, PlayerDataService playerDataService, JailService jailService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.jailService = jailService;
    }

    public void updateJoinState(Player player) {
        PlayerData data = playerDataService.get(player);
        data.setLastKnownName(player.getName());
        data.setLastJoinAt(System.currentTimeMillis());
        playerDataService.save(player);
    }

    public void updateQuitState(Player player) {
        PlayerData data = playerDataService.get(player);
        data.setLastKnownName(player.getName());
        data.setLastQuitAt(System.currentTimeMillis());
        playerDataService.save(player);
    }

    public void mute(CommandSender actor, Player target, long durationSeconds, String reason) {
        PlayerData data = playerDataService.get(target);
        data.setMutedUntil(durationSeconds == Long.MAX_VALUE ? Long.MAX_VALUE : System.currentTimeMillis() + (durationSeconds * 1000L));
        data.setMuteReason(reason);
        data.setLastKnownName(target.getName());
        playerDataService.save(target);
        log(actor, target, "MUTE", reason + " (" + durationSeconds + "s)");
    }

    public void unmute(CommandSender actor, OfflinePlayer target) {
        PlayerData data = playerDataService.get(target.getUniqueId());
        data.setMutedUntil(0L);
        data.setMuteReason("");
        if (target.getName() != null) {
            data.setLastKnownName(target.getName());
        }
        playerDataService.save(target.getUniqueId());
        log(actor, target, "UNMUTE", "Voice restored");
    }

    public void freeze(CommandSender actor, Player target, String reason) {
        PlayerData data = playerDataService.get(target);
        data.setFrozen(true);
        data.setFreezeReason(reason);
        data.setLastKnownName(target.getName());
        playerDataService.save(target);
        log(actor, target, "FREEZE", reason);
    }

    public void unfreeze(CommandSender actor, OfflinePlayer target) {
        PlayerData data = playerDataService.get(target.getUniqueId());
        data.setFrozen(false);
        data.setFreezeReason("");
        if (target.getName() != null) {
            data.setLastKnownName(target.getName());
        }
        playerDataService.save(target.getUniqueId());
        log(actor, target, "UNFREEZE", "Movement restored");
    }

    public void warn(CommandSender actor, OfflinePlayer target, String reason) {
        PlayerData data = playerDataService.get(target.getUniqueId());
        if (target.getName() != null) {
            data.setLastKnownName(target.getName());
        }
        playerDataService.save(target.getUniqueId());
        log(actor, target, "WARN", reason);
    }

    public void kick(CommandSender actor, Player target, String reason) {
        log(actor, target, "KICK", reason);
        target.kickPlayer(reason);
    }

    public void shadowMute(CommandSender actor, OfflinePlayer target, boolean enabled) {
        PlayerData data = playerDataService.get(target.getUniqueId());
        data.setShadowMuted(enabled);
        if (target.getName() != null) {
            data.setLastKnownName(target.getName());
        }
        playerDataService.save(target.getUniqueId());
        log(actor, target, enabled ? "SHADOW_MUTE" : "SHADOW_UNMUTE", enabled ? "Messages hidden from others" : "Messages restored");
    }

    public void tempBan(CommandSender actor, OfflinePlayer target, long durationSeconds, String reason) {
        PlayerData data = playerDataService.get(target.getUniqueId());
        long until = durationSeconds == Long.MAX_VALUE ? Long.MAX_VALUE : System.currentTimeMillis() + (durationSeconds * 1000L);
        data.setBannedUntil(until);
        data.setBanReason(reason);
        if (target.getName() != null) {
            data.setLastKnownName(target.getName());
            java.util.Date expires = until == Long.MAX_VALUE ? null : new java.util.Date(until);
            Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, expires, actor.getName());
        }
        playerDataService.save(target.getUniqueId());
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().kickPlayer(reason);
        }
        log(actor, target, "TEMPBAN", reason);
    }

    public void unban(CommandSender actor, OfflinePlayer target) {
        PlayerData data = playerDataService.get(target.getUniqueId());
        data.setBannedUntil(0L);
        data.setBanReason("");
        if (target.getName() != null) {
            data.setLastKnownName(target.getName());
            Bukkit.getBanList(BanList.Type.NAME).pardon(target.getName());
        }
        playerDataService.save(target.getUniqueId());
        log(actor, target, "UNBAN", "Ban cleared");
    }

    public void jail(CommandSender actor, Player target, String jailName, long durationSeconds, String reason) {
        SavedLocation jail = jailService.getJail(jailName);
        if (jail == null) {
            throw new IllegalArgumentException("Unknown jail " + jailName);
        }

        PlayerData data = playerDataService.get(target);
        data.setJailName(jailName.toLowerCase());
        data.setJailedUntil(durationSeconds == Long.MAX_VALUE ? Long.MAX_VALUE : System.currentTimeMillis() + (durationSeconds * 1000L));
        data.setLastKnownName(target.getName());
        playerDataService.save(target);
        if (jail.toLocation() != null) {
            target.teleport(jail.toLocation());
        }
        log(actor, target, "JAIL", jailName + " - " + reason);
    }

    public void unjail(CommandSender actor, OfflinePlayer target) {
        PlayerData data = playerDataService.get(target.getUniqueId());
        String jailName = data.getJailName();
        data.setJailName("");
        data.setJailedUntil(0L);
        playerDataService.save(target.getUniqueId());
        log(actor, target, "UNJAIL", jailName.isBlank() ? "Released" : "Released from " + jailName);
    }

    public void addStaffNote(CommandSender actor, OfflinePlayer target, String note) {
        log(actor, target, "NOTE", note);
    }

    public boolean isMuted(UUID uuid) {
        return playerDataService.get(uuid).isMuted();
    }

    public boolean isShadowMuted(UUID uuid) {
        return playerDataService.get(uuid).isShadowMuted();
    }

    public boolean isFrozen(UUID uuid) {
        return playerDataService.get(uuid).isFrozen();
    }

    public boolean isJailed(UUID uuid) {
        PlayerData data = playerDataService.get(uuid);
        if (data.isJailed()) {
            return true;
        }
        if (!data.getJailName().isBlank()) {
            data.setJailName("");
            data.setJailedUntil(0L);
            playerDataService.save(uuid);
        }
        return false;
    }

    public SavedLocation getJailLocation(UUID uuid) {
        String jailName = playerDataService.get(uuid).getJailName();
        return jailName.isBlank() ? null : jailService.getJail(jailName);
    }

    public boolean isBanned(UUID uuid) {
        return playerDataService.get(uuid).isBanned();
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataService.get(uuid);
    }

    public List<ModerationLogEntry> getLogs(OfflinePlayer target, int limit) {
        return plugin.getStorageManager().getProvider().loadModerationLogs(target.getUniqueId(), target.getName(), limit);
    }

    public void log(CommandSender actor, OfflinePlayer target, String action, String details) {
        UUID actorUuid = actor instanceof Player player ? player.getUniqueId() : null;
        String actorName = actor instanceof Player player ? player.getName() : actor.getName();
        String targetName = target.getName() == null ? "Unknown" : target.getName();

        plugin.getStorageManager().getProvider().appendModerationLog(new ModerationLogEntry(
            System.currentTimeMillis(),
            action,
            actorUuid,
            actorName,
            target.getUniqueId(),
            targetName,
            details
        ));
    }
}
