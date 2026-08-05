package dev.crossroadsmc.crossroads.integration.vault;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.service.PermissionService;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;

/**
 * Isolated Vault permission registration. Kept in its own class so
 * {@link PermissionService} can load without Vault on the classpath.
 */
public final class VaultPermissionBridge {
    private VaultPermissionBridge() {
    }

    public static Object register(PermissionService permissionService, CrossroadsPlugin plugin) {
        CrossroadsVaultPermission provider = new CrossroadsVaultPermission(permissionService);
        Bukkit.getServicesManager().register(Permission.class, provider, plugin, ServicePriority.High);
        return provider;
    }

    public static void unregister(Object provider) {
        if (provider instanceof Permission permission) {
            Bukkit.getServicesManager().unregister(Permission.class, permission);
        }
    }
}
