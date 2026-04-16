package dev.crossroadsmc.crossroads.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerData {
    public static final String GLOBAL_SCOPE = "global";

    private final UUID uuid;
    private final Map<String, Map<String, SavedLocation>> homesByScope = new ConcurrentHashMap<>();
    private final Set<UUID> ignoredPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> kitCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Long> commandCooldowns = new ConcurrentHashMap<>();
    private final List<MailMessage> mailbox = Collections.synchronizedList(new ArrayList<>());
    private volatile String lastKnownName = "";
    private volatile String nickname = "";
    private volatile long lastJoinAt;
    private volatile long lastQuitAt;
    private volatile long mutedUntil;
    private volatile String muteReason = "";
    private volatile boolean shadowMuted;
    private volatile boolean frozen;
    private volatile String freezeReason = "";
    private volatile long bannedUntil;
    private volatile String banReason = "";
    private volatile long jailedUntil;
    private volatile String jailName = "";
    private volatile SavedLocation backLocation;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<String, SavedLocation> getHomes(String scope) {
        Map<String, SavedLocation> homes = homesByScope.get(normalizeScope(scope));
        return homes == null ? Collections.emptyMap() : Collections.unmodifiableMap(homes);
    }

    public Map<String, Map<String, SavedLocation>> getHomesByScope() {
        Map<String, Map<String, SavedLocation>> copy = new ConcurrentHashMap<>();
        for (Map.Entry<String, Map<String, SavedLocation>> entry : homesByScope.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public void setHome(String scope, String name, SavedLocation location) {
        homesByScope.computeIfAbsent(normalizeScope(scope), ignored -> new ConcurrentHashMap<>())
            .put(normalizeKey(name), location);
    }

    public SavedLocation getHome(String scope, String name) {
        Map<String, SavedLocation> homes = homesByScope.get(normalizeScope(scope));
        return homes == null ? null : homes.get(normalizeKey(name));
    }

    public SavedLocation removeHome(String scope, String name) {
        Map<String, SavedLocation> homes = homesByScope.get(normalizeScope(scope));
        if (homes == null) {
            return null;
        }
        SavedLocation removed = homes.remove(normalizeKey(name));
        if (homes.isEmpty()) {
            homesByScope.remove(normalizeScope(scope));
        }
        return removed;
    }

    public boolean hasHome(String scope, String name) {
        Map<String, SavedLocation> homes = homesByScope.get(normalizeScope(scope));
        return homes != null && homes.containsKey(normalizeKey(name));
    }

    public int getHomeCount(String scope) {
        Map<String, SavedLocation> homes = homesByScope.get(normalizeScope(scope));
        return homes == null ? 0 : homes.size();
    }

    public void importHomes(Map<String, Map<String, SavedLocation>> loadedHomes) {
        homesByScope.clear();
        for (Map.Entry<String, Map<String, SavedLocation>> entry : loadedHomes.entrySet()) {
            homesByScope.put(normalizeScope(entry.getKey()), new ConcurrentHashMap<>(entry.getValue()));
        }
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
        return kitCooldowns.getOrDefault(normalizeKey(key), 0L);
    }

    public void setKitCooldown(String key, long value) {
        kitCooldowns.put(normalizeKey(key), value);
    }

    public void importKitCooldowns(Map<String, Long> loadedCooldowns) {
        kitCooldowns.clear();
        loadedCooldowns.forEach((key, value) -> kitCooldowns.put(normalizeKey(key), value));
    }

    public Map<String, Long> getCommandCooldowns() {
        return Collections.unmodifiableMap(commandCooldowns);
    }

    public long getCommandCooldown(String key) {
        return commandCooldowns.getOrDefault(normalizeKey(key), 0L);
    }

    public void setCommandCooldown(String key, long value) {
        commandCooldowns.put(normalizeKey(key), value);
    }

    public void importCommandCooldowns(Map<String, Long> loadedCooldowns) {
        commandCooldowns.clear();
        loadedCooldowns.forEach((key, value) -> commandCooldowns.put(normalizeKey(key), value));
    }

    public List<MailMessage> getMailbox() {
        synchronized (mailbox) {
            return List.copyOf(mailbox);
        }
    }

    public MailMessage getMail(int index) {
        synchronized (mailbox) {
            if (index < 0 || index >= mailbox.size()) {
                return null;
            }
            return mailbox.get(index);
        }
    }

    public void addMail(MailMessage message) {
        synchronized (mailbox) {
            mailbox.add(message);
        }
    }

    public MailMessage removeMail(int index) {
        synchronized (mailbox) {
            if (index < 0 || index >= mailbox.size()) {
                return null;
            }
            return mailbox.remove(index);
        }
    }

    public void clearMail() {
        synchronized (mailbox) {
            mailbox.clear();
        }
    }

    public int getUnreadMailCount() {
        synchronized (mailbox) {
            int unread = 0;
            for (MailMessage message : mailbox) {
                if (!message.isRead()) {
                    unread++;
                }
            }
            return unread;
        }
    }

    public void markMailRead(int index) {
        synchronized (mailbox) {
            if (index >= 0 && index < mailbox.size()) {
                mailbox.set(index, mailbox.get(index).markRead());
            }
        }
    }

    public void importMailbox(List<MailMessage> loadedMail) {
        synchronized (mailbox) {
            mailbox.clear();
            mailbox.addAll(loadedMail);
        }
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = Objects.requireNonNullElse(lastKnownName, "");
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = Objects.requireNonNullElse(nickname, "");
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

    public boolean isShadowMuted() {
        return shadowMuted;
    }

    public void setShadowMuted(boolean shadowMuted) {
        this.shadowMuted = shadowMuted;
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

    public long getBannedUntil() {
        return bannedUntil;
    }

    public void setBannedUntil(long bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    public boolean isBanned() {
        return bannedUntil > System.currentTimeMillis();
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = Objects.requireNonNullElse(banReason, "");
    }

    public long getJailedUntil() {
        return jailedUntil;
    }

    public void setJailedUntil(long jailedUntil) {
        this.jailedUntil = jailedUntil;
    }

    public boolean isJailed() {
        return jailedUntil > System.currentTimeMillis() && !jailName.isBlank();
    }

    public String getJailName() {
        return jailName;
    }

    public void setJailName(String jailName) {
        this.jailName = Objects.requireNonNullElse(jailName, "");
    }

    public SavedLocation getBackLocation() {
        return backLocation;
    }

    public void setBackLocation(SavedLocation backLocation) {
        this.backLocation = backLocation;
    }

    private String normalizeScope(String scope) {
        return scope == null || scope.isBlank() ? GLOBAL_SCOPE : scope.toLowerCase();
    }

    private String normalizeKey(String key) {
        return Objects.requireNonNullElse(key, "").toLowerCase();
    }
}
