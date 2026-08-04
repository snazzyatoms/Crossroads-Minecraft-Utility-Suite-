package dev.crossroadsmc.crossroads.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PermissionGroup {
    private final String name;
    private final List<PermissionNode> nodes = new CopyOnWriteArrayList<>();
    private final List<String> parents = new CopyOnWriteArrayList<>();
    private volatile String prefix = "";
    private volatile String suffix = "";
    private volatile int weight;
    private volatile boolean defaultGroup;

    public PermissionGroup(String name) {
        this.name = Objects.requireNonNullElse(name, "").toLowerCase(Locale.ROOT);
    }

    public String getName() {
        return name;
    }

    public List<PermissionNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public void setNodes(List<PermissionNode> loaded) {
        nodes.clear();
        if (loaded != null) {
            nodes.addAll(loaded);
        }
    }

    public void setNode(PermissionNode node) {
        nodes.removeIf(existing -> existing.getPermission().equals(node.getPermission())
            && existing.getWorld().equals(node.getWorld())
            && existing.getServer().equals(node.getServer()));
        nodes.add(node);
    }

    public boolean unsetNode(String permission, String world, String server) {
        String normalized = permission == null ? "" : permission.toLowerCase(Locale.ROOT);
        String worldKey = world == null ? "" : world.toLowerCase(Locale.ROOT);
        String serverKey = server == null ? "" : server.toLowerCase(Locale.ROOT);
        return nodes.removeIf(node -> node.getPermission().equals(normalized)
            && node.getWorld().equals(worldKey)
            && node.getServer().equals(serverKey));
    }

    public List<String> getParents() {
        return Collections.unmodifiableList(parents);
    }

    public void setParents(List<String> loaded) {
        parents.clear();
        if (loaded != null) {
            for (String parent : loaded) {
                if (parent != null && !parent.isBlank()) {
                    parents.add(parent.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    public boolean addParent(String parent) {
        String normalized = parent == null ? "" : parent.toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.equals(name) || parents.contains(normalized)) {
            return false;
        }
        parents.add(normalized);
        return true;
    }

    public boolean removeParent(String parent) {
        return parents.remove(parent == null ? "" : parent.toLowerCase(Locale.ROOT));
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = Objects.requireNonNullElse(prefix, "");
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = Objects.requireNonNullElse(suffix, "");
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(boolean defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public List<PermissionNode> snapshotNodes() {
        return new ArrayList<>(nodes);
    }
}
