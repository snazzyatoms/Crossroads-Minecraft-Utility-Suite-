package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PermissionGroup;
import dev.crossroadsmc.crossroads.model.PermissionNode;
import dev.crossroadsmc.crossroads.model.PlayerData;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.ServicePriority;

public final class PermissionService {
    private final CrossroadsPlugin plugin;
    private final Map<String, PermissionGroup> groups = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private String defaultGroup = "default";
    private String serverName = "global";
    private boolean worldContexts = true;
    private boolean vaultBridge;
    private Permission vaultProvider;

    public PermissionService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        defaultGroup = plugin.getConfig().getString("permissions.default-group", "default").toLowerCase(Locale.ROOT);
        serverName = plugin.getConfig().getString("permissions.contexts.server-name", "global").toLowerCase(Locale.ROOT);
        worldContexts = plugin.getConfig().getBoolean("permissions.contexts.world", true);
        vaultBridge = plugin.getConfig().getBoolean("permissions.vault-bridge", false);
        loadGroups();
        ensureDefaultGroup();
        saveGroups();
        refreshOnlinePlayers();
        updateVaultBridge();
    }

    public boolean isEnabled() {
        return plugin.isFeatureEnabled("permissions") && plugin.getConfig().getBoolean("permissions.enabled", true);
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public Map<String, PermissionGroup> getGroups() {
        return Collections.unmodifiableMap(groups);
    }

    public PermissionGroup getGroup(String name) {
        if (name == null) {
            return null;
        }
        return groups.get(name.toLowerCase(Locale.ROOT));
    }

    public PermissionGroup createGroup(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        PermissionGroup existing = groups.get(normalized);
        if (existing != null) {
            return existing;
        }
        PermissionGroup group = new PermissionGroup(normalized);
        groups.put(normalized, group);
        saveGroups();
        return group;
    }

    public boolean deleteGroup(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.equals(defaultGroup)) {
            return false;
        }
        PermissionGroup removed = groups.remove(normalized);
        if (removed == null) {
            return false;
        }
        for (PermissionGroup group : groups.values()) {
            group.removeParent(normalized);
        }
        saveGroups();
        refreshOnlinePlayers();
        return true;
    }

    public void ensurePlayerDefaults(PlayerData data) {
        if (!isEnabled()) {
            return;
        }
        if (data.getPrimaryGroup().isBlank()) {
            data.setPrimaryGroup(defaultGroup);
        }
        if (data.getPermissionGroups().isEmpty()) {
            data.addPermissionGroup(defaultGroup);
        }
    }

    public void applyAttachments(Player player) {
        if (!isEnabled() || player == null) {
            return;
        }
        PlayerData data = PermissionService.this.plugin.getPlayerDataService().get(player);
        ensurePlayerDefaults(data);
        pruneExpired(data);

        PermissionAttachment attachment = attachments.compute(player.getUniqueId(), (uuid, existing) -> {
            if (existing != null) {
                try {
                    player.removeAttachment(existing);
                } catch (IllegalArgumentException ignored) {
                    // Already gone.
                }
            }
            return player.addAttachment(plugin);
        });

        Map<String, Boolean> effective = resolveEffectivePermissions(data, player.getWorld().getName());
        for (Map.Entry<String, Boolean> entry : effective.entrySet()) {
            attachment.setPermission(entry.getKey(), entry.getValue());
        }
        player.recalculatePermissions();
    }

    public void removeAttachments(Player player) {
        if (player == null) {
            return;
        }
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            try {
                player.removeAttachment(attachment);
            } catch (IllegalArgumentException ignored) {
                // Already removed.
            }
        }
    }

    public void refreshOnlinePlayers() {
        if (!isEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyAttachments(player);
        }
    }

    public Map<String, Boolean> resolveEffectivePermissions(PlayerData data, String world) {
        Map<String, Boolean> effective = new LinkedHashMap<>();
        String currentWorld = worldContexts ? world : "";
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();

        List<String> membership = new ArrayList<>(data.getPermissionGroups());
        if (!data.getPrimaryGroup().isBlank() && !membership.contains(data.getPrimaryGroup())) {
            membership.add(data.getPrimaryGroup());
        }
        queue.addAll(membership);

        while (!queue.isEmpty()) {
            String groupName = queue.poll();
            if (groupName == null || !visited.add(groupName)) {
                continue;
            }
            PermissionGroup group = groups.get(groupName);
            if (group == null) {
                continue;
            }
            for (PermissionNode node : group.getNodes()) {
                if (node.isExpired() || !node.matchesContext(currentWorld, serverName)) {
                    continue;
                }
                effective.put(node.getPermission(), node.getValue());
            }
            queue.addAll(group.getParents());
        }

        for (PermissionNode node : data.getPermissionNodes()) {
            if (node.isExpired() || !node.matchesContext(currentWorld, serverName)) {
                continue;
            }
            effective.put(node.getPermission(), node.getValue());
        }
        return effective;
    }

    public boolean hasPermission(PlayerData data, String permission, String world) {
        Boolean value = resolveEffectivePermissions(data, world).get(permission.toLowerCase(Locale.ROOT));
        return Boolean.TRUE.equals(value);
    }

    public String resolvePrefix(PlayerData data) {
        if (!data.getPrefix().isBlank()) {
            return data.getPrefix();
        }
        return resolvePrimaryGroup(data).getPrefix();
    }

    public String resolveSuffix(PlayerData data) {
        if (!data.getSuffix().isBlank()) {
            return data.getSuffix();
        }
        return resolvePrimaryGroup(data).getSuffix();
    }

    public PermissionGroup resolvePrimaryGroup(PlayerData data) {
        PermissionGroup primary = getGroup(data.getPrimaryGroup());
        if (primary != null) {
            return primary;
        }
        return groups.values().stream()
            .max(Comparator.comparingInt(PermissionGroup::getWeight))
            .orElseGet(() -> createGroup(defaultGroup));
    }

    public void saveGroups() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("default-group", defaultGroup);
        ConfigurationSection groupsSection = yaml.createSection("groups");
        for (PermissionGroup group : groups.values()) {
            ConfigurationSection section = groupsSection.createSection(group.getName());
            section.set("prefix", group.getPrefix());
            section.set("suffix", group.getSuffix());
            section.set("weight", group.getWeight());
            section.set("default", group.isDefaultGroup());
            section.set("parents", group.getParents());
            ConfigurationSection nodesSection = section.createSection("permissions");
            List<PermissionNode> nodes = group.snapshotNodes();
            for (int index = 0; index < nodes.size(); index++) {
                PermissionNode node = nodes.get(index);
                ConfigurationSection nodeSection = nodesSection.createSection(String.valueOf(index));
                nodeSection.set("permission", node.getPermission());
                nodeSection.set("value", node.getValue());
                nodeSection.set("expires-at", node.getExpiresAt());
                nodeSection.set("world", node.getWorld());
                nodeSection.set("server", node.getServer());
            }
        }
        plugin.getStorageManager().getProvider().saveDocument("permissions", yaml);
    }

    public int importLuckPermsYaml() {
        File luckPermsFolder = new File(plugin.getDataFolder().getParentFile(), "LuckPerms");
        File groupsFile = new File(luckPermsFolder, "yaml-storage/groups.yml");
        if (!groupsFile.exists()) {
            groupsFile = new File(luckPermsFolder, "groups.yml");
        }
        if (!groupsFile.exists()) {
            return 0;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(groupsFile);
        int imported = 0;
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            PermissionGroup group = createGroup(key);
            group.setPrefix(section.getString("prefix", section.getString("meta.prefix", "")));
            group.setSuffix(section.getString("suffix", section.getString("meta.suffix", "")));
            group.setWeight(section.getInt("weight", section.getInt("meta.weight", 0)));
            List<String> parents = section.getStringList("parents");
            if (parents.isEmpty() && section.isConfigurationSection("parents")) {
                parents = new ArrayList<>(section.getConfigurationSection("parents").getKeys(false));
            }
            group.setParents(parents);

            List<PermissionNode> nodes = new ArrayList<>();
            ConfigurationSection permissions = section.getConfigurationSection("permissions");
            if (permissions != null) {
                for (String permission : permissions.getKeys(false)) {
                    nodes.add(PermissionNode.permanent(permission, permissions.getBoolean(permission, true)));
                }
            } else {
                for (String permission : section.getStringList("permissions")) {
                    boolean value = !permission.startsWith("-");
                    nodes.add(PermissionNode.permanent(value ? permission : permission.substring(1), value));
                }
            }
            group.setNodes(nodes);
            imported++;
        }
        saveGroups();
        refreshOnlinePlayers();
        return imported;
    }

    public void shutdown() {
        unregisterVaultBridge();
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeAttachments(player);
        }
        attachments.clear();
    }

    private void loadGroups() {
        groups.clear();
        YamlConfiguration yaml = plugin.getStorageManager().getProvider().loadDocument("permissions");
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }
        String storedDefault = yaml.getString("default-group");
        if (storedDefault != null && !storedDefault.isBlank()) {
            defaultGroup = storedDefault.toLowerCase(Locale.ROOT);
        }

        ConfigurationSection groupsSection = yaml.getConfigurationSection("groups");
        if (groupsSection != null) {
            for (String key : groupsSection.getKeys(false)) {
                ConfigurationSection section = groupsSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                PermissionGroup group = new PermissionGroup(key);
                group.setPrefix(section.getString("prefix", ""));
                group.setSuffix(section.getString("suffix", ""));
                group.setWeight(section.getInt("weight", 0));
                group.setDefaultGroup(section.getBoolean("default", key.equalsIgnoreCase(defaultGroup)));
                group.setParents(section.getStringList("parents"));
                List<PermissionNode> nodes = new ArrayList<>();
                ConfigurationSection nodesSection = section.getConfigurationSection("permissions");
                if (nodesSection != null) {
                    for (String nodeKey : nodesSection.getKeys(false)) {
                        ConfigurationSection nodeSection = nodesSection.getConfigurationSection(nodeKey);
                        if (nodeSection == null) {
                            Object raw = nodesSection.get(nodeKey);
                            if (raw instanceof Boolean bool) {
                                nodes.add(PermissionNode.permanent(nodeKey, bool));
                            }
                            continue;
                        }
                        nodes.add(new PermissionNode(
                            nodeSection.getString("permission", ""),
                            nodeSection.getBoolean("value", true),
                            nodeSection.getLong("expires-at", 0L),
                            nodeSection.getString("world", ""),
                            nodeSection.getString("server", "")
                        ));
                    }
                }
                group.setNodes(nodes);
                groups.put(group.getName(), group);
            }
        }
    }

    private void ensureDefaultGroup() {
        PermissionGroup group = groups.computeIfAbsent(defaultGroup, PermissionGroup::new);
        group.setDefaultGroup(true);
        if (group.getNodes().isEmpty()) {
            group.setNode(PermissionNode.permanent("crossroads.home", true));
            group.setNode(PermissionNode.permanent("crossroads.warp", true));
            group.setNode(PermissionNode.permanent("crossroads.spawn", true));
            group.setNode(PermissionNode.permanent("crossroads.back", true));
            group.setNode(PermissionNode.permanent("crossroads.msg", true));
            group.setNode(PermissionNode.permanent("crossroads.kit", true));
            group.setNode(PermissionNode.permanent("crossroads.rules", true));
            group.setNode(PermissionNode.permanent("crossroads.teleport", true));
            group.setNode(PermissionNode.permanent("crossroads.language", true));
            group.setNode(PermissionNode.permanent("crossroads.economy.balance", true));
            group.setNode(PermissionNode.permanent("crossroads.economy.pay", true));
        }
    }

    private void pruneExpired(PlayerData data) {
        List<PermissionNode> retained = new ArrayList<>();
        boolean changed = false;
        for (PermissionNode node : data.getPermissionNodes()) {
            if (node.isExpired()) {
                changed = true;
            } else {
                retained.add(node);
            }
        }
        if (changed) {
            data.setPermissionNodes(retained);
            PermissionService.this.plugin.getPlayerDataService().save(data.getUuid());
        }
    }

    private void updateVaultBridge() {
        unregisterVaultBridge();
        if (!vaultBridge || !isEnabled()) {
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        try {
            vaultProvider = new CrossroadsVaultPermission();
            Bukkit.getServicesManager().register(Permission.class, vaultProvider, plugin, ServicePriority.High);
            plugin.getLogger().info("Registered Crossroads as a Vault Permission provider.");
        } catch (Throwable exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to register Vault permission bridge.", exception);
            vaultProvider = null;
        }
    }

    private void unregisterVaultBridge() {
        if (vaultProvider != null) {
            try {
                Bukkit.getServicesManager().unregister(Permission.class, vaultProvider);
            } catch (Exception ignored) {
                // Best effort.
            }
            vaultProvider = null;
        }
    }

    private final class CrossroadsVaultPermission extends Permission {
        @Override
        public String getName() {
            return "Crossroads";
        }

        @Override
        public boolean isEnabled() {
            return PermissionService.this.isEnabled();
        }

        @Override
        public boolean hasSuperPermsCompat() {
            return true;
        }

        @Override
        public boolean playerHas(String world, String player, String permission) {
            Player online = Bukkit.getPlayerExact(player);
            if (online != null) {
                return online.hasPermission(permission);
            }
            return false;
        }

        @Override
        public boolean playerAdd(String world, String player, String permission) {
            Player online = Bukkit.getPlayerExact(player);
            if (online == null) {
                return false;
            }
            PlayerData data = PermissionService.this.plugin.getPlayerDataService().get(online);
            data.setPermissionNode(new PermissionNode(permission, true, 0L, world == null ? "" : world, serverName));
            PermissionService.this.plugin.getPlayerDataService().save(online);
            applyAttachments(online);
            return true;
        }

        @Override
        public boolean playerRemove(String world, String player, String permission) {
            Player online = Bukkit.getPlayerExact(player);
            if (online == null) {
                return false;
            }
            PlayerData data = PermissionService.this.plugin.getPlayerDataService().get(online);
            boolean removed = data.unsetPermissionNode(permission, world == null ? "" : world, serverName);
            if (removed) {
                PermissionService.this.plugin.getPlayerDataService().save(online);
                applyAttachments(online);
            }
            return removed;
        }

        @Override
        public boolean groupHas(String world, String group, String permission) {
            PermissionGroup permissionGroup = getGroup(group);
            if (permissionGroup == null) {
                return false;
            }
            for (PermissionNode node : permissionGroup.getNodes()) {
                if (node.getPermission().equalsIgnoreCase(permission)
                    && node.matchesContext(world == null ? "" : world, serverName)
                    && !node.isExpired()) {
                    return node.getValue();
                }
            }
            return false;
        }

        @Override
        public boolean groupAdd(String world, String group, String permission) {
            PermissionGroup permissionGroup = createGroup(group);
            permissionGroup.setNode(new PermissionNode(permission, true, 0L, world == null ? "" : world, serverName));
            saveGroups();
            refreshOnlinePlayers();
            return true;
        }

        @Override
        public boolean groupRemove(String world, String group, String permission) {
            PermissionGroup permissionGroup = getGroup(group);
            if (permissionGroup == null) {
                return false;
            }
            boolean removed = permissionGroup.unsetNode(permission, world == null ? "" : world, serverName);
            if (removed) {
                saveGroups();
                refreshOnlinePlayers();
            }
            return removed;
        }

        @Override
        public boolean playerInGroup(String world, String player, String group) {
            Player online = Bukkit.getPlayerExact(player);
            if (online == null) {
                return false;
            }
            return PermissionService.this.plugin.getPlayerDataService().get(online).getPermissionGroups().contains(group.toLowerCase(Locale.ROOT));
        }

        @Override
        public boolean playerAddGroup(String world, String player, String group) {
            Player online = Bukkit.getPlayerExact(player);
            if (online == null) {
                return false;
            }
            createGroup(group);
            PlayerData data = PermissionService.this.plugin.getPlayerDataService().get(online);
            boolean added = data.addPermissionGroup(group);
            if (data.getPrimaryGroup().isBlank()) {
                data.setPrimaryGroup(group);
            }
            PermissionService.this.plugin.getPlayerDataService().save(online);
            applyAttachments(online);
            return added;
        }

        @Override
        public boolean playerRemoveGroup(String world, String player, String group) {
            Player online = Bukkit.getPlayerExact(player);
            if (online == null) {
                return false;
            }
            PlayerData data = PermissionService.this.plugin.getPlayerDataService().get(online);
            boolean removed = data.removePermissionGroup(group);
            if (removed) {
                if (data.getPrimaryGroup().equalsIgnoreCase(group)) {
                    data.setPrimaryGroup(defaultGroup);
                    data.addPermissionGroup(defaultGroup);
                }
                PermissionService.this.plugin.getPlayerDataService().save(online);
                applyAttachments(online);
            }
            return removed;
        }

        @Override
        public String[] getPlayerGroups(String world, String player) {
            Player online = Bukkit.getPlayerExact(player);
            if (online == null) {
                return new String[0];
            }
            return PermissionService.this.plugin.getPlayerDataService().get(online).getPermissionGroups().toArray(String[]::new);
        }

        @Override
        public String getPrimaryGroup(String world, String player) {
            Player online = Bukkit.getPlayerExact(player);
            if (online == null) {
                return defaultGroup;
            }
            String primary = PermissionService.this.plugin.getPlayerDataService().get(online).getPrimaryGroup();
            return primary.isBlank() ? defaultGroup : primary;
        }

        @Override
        public String[] getGroups() {
            return groups.keySet().toArray(String[]::new);
        }

        @Override
        public boolean hasGroupSupport() {
            return true;
        }
    }
}
