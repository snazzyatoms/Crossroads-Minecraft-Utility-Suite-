package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.ModerationLogEntry;
import dev.crossroadsmc.crossroads.model.PlayerData;
import java.util.List;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ModerationService {
    private final CrossroadsPlugin plugin;
    private final PlayerDataService playerDataService;

    public ModerationService(CrossroadsPlugin plugin, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
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
        data.setMutedUntil(System.currentTimeMillis() + (durationSeconds * 1000L));
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

    public boolean isMuted(UUID uuid) {
        return playerDataService.get(uuid).isMuted();
    }

    public boolean isFrozen(UUID uuid) {
        return playerDataService.get(uuid).isFrozen();
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
