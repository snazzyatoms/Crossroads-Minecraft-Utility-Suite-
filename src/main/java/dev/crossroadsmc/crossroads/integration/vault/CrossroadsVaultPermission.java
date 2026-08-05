package dev.crossroadsmc.crossroads.integration.vault;

import dev.crossroadsmc.crossroads.model.PermissionGroup;
import dev.crossroadsmc.crossroads.model.PermissionNode;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.service.PermissionService;
import java.util.Locale;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Vault Permission provider backed by Crossroads native groups/nodes.
 * Loaded only when Vault is present and the permission bridge is enabled.
 */
public final class CrossroadsVaultPermission extends Permission {
    private final PermissionService permissions;

    public CrossroadsVaultPermission(PermissionService permissions) {
        this.permissions = permissions;
    }

    @Override
    public String getName() {
        return "Crossroads";
    }

    @Override
    public boolean isEnabled() {
        return permissions.isEnabled();
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
        PlayerData data = permissions.getPlugin().getPlayerDataService().get(online);
        data.setPermissionNode(new PermissionNode(
            permission,
            true,
            0L,
            world == null ? "" : world,
            permissions.getServerName()
        ));
        permissions.getPlugin().getPlayerDataService().save(online);
        permissions.applyAttachments(online);
        return true;
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        Player online = Bukkit.getPlayerExact(player);
        if (online == null) {
            return false;
        }
        PlayerData data = permissions.getPlugin().getPlayerDataService().get(online);
        boolean removed = data.unsetPermissionNode(
            permission,
            world == null ? "" : world,
            permissions.getServerName()
        );
        if (removed) {
            permissions.getPlugin().getPlayerDataService().save(online);
            permissions.applyAttachments(online);
        }
        return removed;
    }

    @Override
    public boolean groupHas(String world, String group, String permission) {
        PermissionGroup permissionGroup = permissions.getGroup(group);
        if (permissionGroup == null) {
            return false;
        }
        for (PermissionNode node : permissionGroup.getNodes()) {
            if (node.getPermission().equalsIgnoreCase(permission)
                && node.matchesContext(world == null ? "" : world, permissions.getServerName())
                && !node.isExpired()) {
                return node.getValue();
            }
        }
        return false;
    }

    @Override
    public boolean groupAdd(String world, String group, String permission) {
        PermissionGroup permissionGroup = permissions.createGroup(group);
        permissionGroup.setNode(new PermissionNode(
            permission,
            true,
            0L,
            world == null ? "" : world,
            permissions.getServerName()
        ));
        permissions.saveGroups();
        permissions.refreshOnlinePlayers();
        return true;
    }

    @Override
    public boolean groupRemove(String world, String group, String permission) {
        PermissionGroup permissionGroup = permissions.getGroup(group);
        if (permissionGroup == null) {
            return false;
        }
        boolean removed = permissionGroup.unsetNode(
            permission,
            world == null ? "" : world,
            permissions.getServerName()
        );
        if (removed) {
            permissions.saveGroups();
            permissions.refreshOnlinePlayers();
        }
        return removed;
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        Player online = Bukkit.getPlayerExact(player);
        if (online == null) {
            return false;
        }
        return permissions.getPlugin().getPlayerDataService().get(online)
            .getPermissionGroups()
            .contains(group.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group) {
        Player online = Bukkit.getPlayerExact(player);
        if (online == null) {
            return false;
        }
        permissions.createGroup(group);
        PlayerData data = permissions.getPlugin().getPlayerDataService().get(online);
        boolean added = data.addPermissionGroup(group);
        if (data.getPrimaryGroup().isBlank()) {
            data.setPrimaryGroup(group);
        }
        permissions.getPlugin().getPlayerDataService().save(online);
        permissions.applyAttachments(online);
        return added;
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
        Player online = Bukkit.getPlayerExact(player);
        if (online == null) {
            return false;
        }
        PlayerData data = permissions.getPlugin().getPlayerDataService().get(online);
        boolean removed = data.removePermissionGroup(group);
        if (removed) {
            if (data.getPrimaryGroup().equalsIgnoreCase(group)) {
                data.setPrimaryGroup(permissions.getDefaultGroup());
                data.addPermissionGroup(permissions.getDefaultGroup());
            }
            permissions.getPlugin().getPlayerDataService().save(online);
            permissions.applyAttachments(online);
        }
        return removed;
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        Player online = Bukkit.getPlayerExact(player);
        if (online == null) {
            return new String[0];
        }
        return permissions.getPlugin().getPlayerDataService().get(online)
            .getPermissionGroups()
            .toArray(String[]::new);
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        Player online = Bukkit.getPlayerExact(player);
        if (online == null) {
            return permissions.getDefaultGroup();
        }
        String primary = permissions.getPlugin().getPlayerDataService().get(online).getPrimaryGroup();
        return primary.isBlank() ? permissions.getDefaultGroup() : primary;
    }

    @Override
    public String[] getGroups() {
        return permissions.getGroups().keySet().toArray(String[]::new);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }
}
