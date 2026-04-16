package dev.crossroadsmc.crossroads.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerData {
    private final UUID uuid;
    private final Map<String, SavedLocation> homes = new ConcurrentHashMap<>();
    private final Set<UUID> ignoredPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> kitCooldowns = new ConcurrentHashMap<>();
    private volatile String lastKnownName = "";
    private volatile long lastJoinAt;
    private volatile long lastQuitAt;
    private volatile long mutedUntil;
    private volatile String muteReason = "";
    private volatile boolean frozen;
    private volatile String freezeReason = "";

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<String, SavedLocation> getHomes() {
        return Collections.unmodifiableMap(homes);
    }

    public void setHome(String name, SavedLocation location) {
        homes.put(name.toLowerCase(), location);
    }

    public SavedLocation getHome(String name) {
        return homes.get(name.toLowerCase());
    }

    public SavedLocation removeHome(String name) {
        return homes.remove(name.toLowerCase());
    }

    public boolean hasHome(String name) {
        return homes.containsKey(name.toLowerCase());
    }

    public int getHomeCount() {
        return homes.size();
    }

    public void clearHomes() {
        homes.clear();
    }

    public void importHomes(Map<String, SavedLocation> loadedHomes) {
        homes.clear();
        homes.putAll(loadedHomes);
    }

    public Set<UUID> getIgnoredPlayers() {
        return Collections.unmodifiableSet(ignoredPlayers);
    }

    public boolean isIgnoring(UUID target) {
        return ignoredPlayers.contains(target);
    }

    public boolean toggleIgnore(UUID target) {
        if (ignoredPlayers.contains(target)) {
            ignoredPlayers.remove(target);
            return false;
        }

        ignoredPlayers.add(target);
        return true;
    }

    public void importIgnoredPlayers(Set<UUID> loadedPlayers) {
        ignoredPlayers.clear();
        ignoredPlayers.addAll(loadedPlayers);
    }

    public Map<String, Long> getKitCooldowns() {
        return Collections.unmodifiableMap(kitCooldowns);
    }

    public long getKitCooldown(String key) {
        return kitCooldowns.getOrDefault(key.toLowerCase(), 0L);
    }

    public void setKitCooldown(String key, long value) {
        kitCooldowns.put(key.toLowerCase(), value);
    }

    public void importKitCooldowns(Map<String, Long> loadedCooldowns) {
        kitCooldowns.clear();
        kitCooldowns.putAll(loadedCooldowns);
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = Objects.requireNonNullElse(lastKnownName, "");
    }

    public long getLastJoinAt() {
        return lastJoinAt;
    }

    public void setLastJoinAt(long lastJoinAt) {
        this.lastJoinAt = lastJoinAt;
    }

    public long getLastQuitAt() {
        return lastQuitAt;
    }

    public void setLastQuitAt(long lastQuitAt) {
        this.lastQuitAt = lastQuitAt;
    }

    public long getMutedUntil() {
        return mutedUntil;
    }

    public void setMutedUntil(long mutedUntil) {
        this.mutedUntil = mutedUntil;
    }

    public String getMuteReason() {
        return muteReason;
    }

    public void setMuteReason(String muteReason) {
        this.muteReason = Objects.requireNonNullElse(muteReason, "");
    }

    public boolean isMuted() {
        return mutedUntil > System.currentTimeMillis();
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public String getFreezeReason() {
        return freezeReason;
    }

    public void setFreezeReason(String freezeReason) {
        this.freezeReason = Objects.requireNonNullElse(freezeReason, "");
    }
}
