package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.TeleportRequest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class TeleportRequestService {
    private final CrossroadsPlugin plugin;
    private final Map<UUID, TeleportRequest> byTarget = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> requesterToTarget = new ConcurrentHashMap<>();

    public TeleportRequestService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    public TeleportRequest create(Player requester, Player target, boolean hereRequest) {
        cleanup();
        TeleportRequest request = new TeleportRequest(
            requester.getUniqueId(),
            target.getUniqueId(),
            requester.getName(),
            hereRequest,
            System.currentTimeMillis()
        );
        byTarget.put(target.getUniqueId(), request);
        requesterToTarget.put(requester.getUniqueId(), target.getUniqueId());
        return request;
    }

    public TeleportRequest getIncoming(Player target) {
        cleanup();
        return byTarget.get(target.getUniqueId());
    }

    public TeleportRequest accept(Player target) {
        cleanup();
        TeleportRequest request = byTarget.remove(target.getUniqueId());
        if (request != null) {
            requesterToTarget.remove(request.getRequesterUuid());
        }
        return request;
    }

    public TeleportRequest deny(Player target) {
        return accept(target);
    }

    public boolean cancel(Player requester) {
        cleanup();
        UUID targetId = requesterToTarget.remove(requester.getUniqueId());
        if (targetId == null) {
            return false;
        }
        TeleportRequest request = byTarget.get(targetId);
        if (request != null && request.getRequesterUuid().equals(requester.getUniqueId())) {
            byTarget.remove(targetId);
            return true;
        }
        return false;
    }

    public void clear(Player player) {
        byTarget.remove(player.getUniqueId());
        requesterToTarget.remove(player.getUniqueId());
    }

    private void cleanup() {
        long timeoutMillis = Math.max(15L, plugin.getConfig().getLong("teleport-requests.timeout-seconds", 60L)) * 1000L;
        long now = System.currentTimeMillis();
        byTarget.entrySet().removeIf(entry -> {
            boolean expired = now - entry.getValue().getCreatedAt() > timeoutMillis;
            if (expired) {
                requesterToTarget.remove(entry.getValue().getRequesterUuid());
            }
            return expired;
        });
    }
}
