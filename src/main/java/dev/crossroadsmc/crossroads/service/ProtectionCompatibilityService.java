package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class ProtectionCompatibilityService {
    private static final String WORLDGUARD_GLOBAL_REGION = "__global__";

    private final CrossroadsPlugin plugin;
    private final AegisGuardHookService aegisGuardHookService;
    private List<ProtectionAdapter> adapters = List.of();

    public ProtectionCompatibilityService(CrossroadsPlugin plugin, AegisGuardHookService aegisGuardHookService) {
        this.plugin = plugin;
        this.aegisGuardHookService = aegisGuardHookService;
        reload();
    }

    public void reload() {
        List<ProtectionAdapter> loaded = new ArrayList<>();
        loadAdapter(loaded, new AegisGuardAdapter());
        loadAdapter(loaded, new LandsAdapter());
        loadAdapter(loaded, new PlotSquaredAdapter());
        loadAdapter(loaded, new GriefPreventionAdapter());
        loadAdapter(loaded, new ResidenceAdapter());
        loadAdapter(loaded, new TownyAdapter());
        loadAdapter(loaded, new WorldGuardAdapter());
        loaded.sort(Comparator.comparing(ProtectionAdapter::displayName, String.CASE_INSENSITIVE_ORDER));
        adapters = List.copyOf(loaded);
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("protection.enabled", true);
    }

    public boolean shouldRespectTeleportEntry() {
        if (plugin.getConfig().contains("protection.teleport-respect-entry")) {
            return plugin.getConfig().getBoolean("protection.teleport-respect-entry", true);
        }
        return plugin.getConfig().getBoolean("aegisguard.teleport-respect-entry", true);
    }

    public boolean shouldAvoidProtectedRtp() {
        if (plugin.getConfig().contains("protection.rtp-avoid-protected-areas")) {
            return plugin.getConfig().getBoolean("protection.rtp-avoid-protected-areas", true);
        }
        return plugin.getConfig().getBoolean("aegisguard.rtp-avoid-protected-plots", true);
    }

    public boolean isProtected(Location location) {
        if (!isEnabled() || location == null) {
            return false;
        }
        for (ProtectionAdapter adapter : adapters) {
            try {
                if (adapter.isProtected(location)) {
                    return true;
                }
            } catch (Exception exception) {
                logAdapterFailure(adapter, "could not evaluate protection status", exception);
            }
        }
        return false;
    }

    public String checkTeleportAccess(Player player, Location destination) {
        if (!isEnabled() || !shouldRespectTeleportEntry() || player == null || destination == null) {
            return null;
        }
        for (ProtectionAdapter adapter : adapters) {
            try {
                String failure = adapter.checkTeleportAccess(player, destination);
                if (failure != null && !failure.isBlank()) {
                    return failure;
                }
            } catch (Exception exception) {
                logAdapterFailure(adapter, "could not validate teleport access", exception);
            }
        }
        return null;
    }

    public ProtectionMatch describe(Location location) {
        if (!isEnabled() || location == null) {
            return null;
        }
        for (ProtectionAdapter adapter : adapters) {
            try {
                ProtectionMatch match = adapter.describe(location);
                if (match != null) {
                    return match;
                }
            } catch (Exception exception) {
                logAdapterFailure(adapter, "could not describe protected location", exception);
            }
        }
        return null;
    }

    public List<ProviderStatus> getActiveProviders() {
        return adapters.stream()
            .map(adapter -> new ProviderStatus(adapter.id(), adapter.displayName(), adapter.version()))
            .toList();
    }

    public String getActiveProviderSummary() {
        if (adapters.isEmpty()) {
            return "none";
        }
        return adapters.stream()
            .map(ProtectionAdapter::displayName)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .reduce((left, right) -> left + ", " + right)
            .orElse("none");
    }

    private void loadAdapter(List<ProtectionAdapter> loaded, ProtectionAdapter adapter) {
        if (!isProviderEnabled(adapter.id())) {
            return;
        }
        try {
            if (adapter.isAvailable()) {
                loaded.add(adapter);
            }
        } catch (Exception exception) {
            logAdapterFailure(adapter, "failed to initialize", exception);
        }
    }

    private boolean isProviderEnabled(String providerId) {
        return plugin.getConfig().getBoolean("protection.providers." + providerId, true);
    }

    private void logAdapterFailure(ProtectionAdapter adapter, String action, Exception exception) {
        plugin.getLogger().log(Level.FINE, adapter.displayName() + " adapter " + action + ".", exception);
    }

    private String blockedMessage(String providerName, String areaName, String ownerName) {
        StringBuilder builder = new StringBuilder(providerName).append(" blocked that teleport.");
        if (areaName != null && !areaName.isBlank()) {
            builder.append(" Area: ").append(areaName);
        }
        if (ownerName != null && !ownerName.isBlank() && !"Unknown".equalsIgnoreCase(ownerName)) {
            builder.append(" | Owner: ").append(ownerName);
        }
        return builder.toString();
    }

    private String playerName(UUID uuid) {
        if (uuid == null) {
            return "Unknown";
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() == null || offlinePlayer.getName().isBlank() ? uuid.toString() : offlinePlayer.getName();
    }

    private static Object invokeStatic(Class<?> type, String methodName, Object... args) throws ReflectiveOperationException {
        Method method = findMethod(type, methodName, args);
        if (method == null) {
            throw new NoSuchMethodException(type.getName() + "#" + methodName);
        }
        return method.invoke(null, args);
    }

    private static Object invoke(Object target, String methodName, Object... args) throws ReflectiveOperationException {
        if (target == null) {
            throw new NoSuchMethodException("Target was null for method " + methodName);
        }
        Method method = findMethod(target.getClass(), methodName, args);
        if (method == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "#" + methodName);
        }
        return method.invoke(target, args);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        try {
            Field field = type.getField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
    }

    private static Method findMethod(Class<?> type, String name, Object... args) {
        for (Method method : type.getMethods()) {
            if (matches(method, name, args)) {
                method.setAccessible(true);
                return method;
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (matches(method, name, args)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static boolean matches(Method method, String name, Object... args) {
        if (!method.getName().equals(name)) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != args.length) {
            return false;
        }
        for (int index = 0; index < parameterTypes.length; index++) {
            if (!isCompatible(parameterTypes[index], args[index])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCompatible(Class<?> parameterType, Object value) {
        if (value == null) {
            return !parameterType.isPrimitive();
        }
        Class<?> valueType = value.getClass();
        if (parameterType.isPrimitive()) {
            return wrapperType(parameterType).isAssignableFrom(valueType);
        }
        if (parameterType.isArray() && valueType.isArray()) {
            return parameterType.getComponentType().isAssignableFrom(valueType.getComponentType());
        }
        return parameterType.isAssignableFrom(valueType);
    }

    private static Class<?> wrapperType(Class<?> primitiveType) {
        if (primitiveType == boolean.class) {
            return Boolean.class;
        }
        if (primitiveType == int.class) {
            return Integer.class;
        }
        if (primitiveType == long.class) {
            return Long.class;
        }
        if (primitiveType == double.class) {
            return Double.class;
        }
        if (primitiveType == float.class) {
            return Float.class;
        }
        if (primitiveType == short.class) {
            return Short.class;
        }
        if (primitiveType == byte.class) {
            return Byte.class;
        }
        if (primitiveType == char.class) {
            return Character.class;
        }
        return primitiveType;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String stringMethod(Object target, String methodName) {
        try {
            return stringValue(invoke(target, methodName));
        } catch (ReflectiveOperationException exception) {
            return "";
        }
    }

    private static boolean booleanMethod(Object target, String methodName, Object... args) {
        try {
            Object result = invoke(target, methodName, args);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static Object first(Collection<?> values) {
        return values == null || values.isEmpty() ? null : values.iterator().next();
    }

    private abstract class ProtectionAdapter {
        abstract String id();

        abstract String displayName();

        abstract boolean isAvailable() throws Exception;

        abstract String version();

        abstract ProtectionMatch describe(Location location) throws Exception;

        String checkTeleportAccess(Player player, Location destination) throws Exception {
            return null;
        }

        boolean isProtected(Location location) throws Exception {
            return describe(location) != null;
        }
    }

    private final class AegisGuardAdapter extends ProtectionAdapter {
        @Override
        String id() {
            return "aegisguard";
        }

        @Override
        String displayName() {
            return "AegisGuard";
        }

        @Override
        boolean isAvailable() {
            return aegisGuardHookService.isAvailable();
        }

        @Override
        String version() {
            return aegisGuardHookService.getVersion();
        }

        @Override
        ProtectionMatch describe(Location location) {
            AegisGuardHookService.PlotInfo info = aegisGuardHookService.getPlotInfo(location);
            if (info == null) {
                return null;
            }
            return new ProtectionMatch(id(), displayName(), info.name(), info.ownerName());
        }

        @Override
        String checkTeleportAccess(Player player, Location destination) {
            return aegisGuardHookService.checkTeleportAccess(player, destination);
        }
    }

    private final class WorldGuardAdapter extends ProtectionAdapter {
        private final Plugin worldGuardPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");

        @Override
        String id() {
            return "worldguard";
        }

        @Override
        String displayName() {
            return "WorldGuard";
        }

        @Override
        boolean isAvailable() throws Exception {
            return worldGuardPlugin != null
                && worldGuardPlugin.isEnabled()
                && Class.forName("com.sk89q.worldguard.WorldGuard") != null;
        }

        @Override
        String version() {
            return worldGuardPlugin == null ? "unknown" : worldGuardPlugin.getDescription().getVersion();
        }

        @Override
        ProtectionMatch describe(Location location) throws Exception {
            Object region = firstNonGlobalRegion(location);
            if (region == null) {
                return null;
            }
            return new ProtectionMatch(id(), displayName(), stringMethod(region, "getId"), "");
        }

        @Override
        String checkTeleportAccess(Player player, Location destination) throws Exception {
            Object region = firstNonGlobalRegion(destination);
            if (region == null) {
                return null;
            }

            Object localPlayer = invoke(invokeStatic(Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin"), "inst"), "wrapPlayer", player);
            Object worldGuard = invokeStatic(Class.forName("com.sk89q.worldguard.WorldGuard"), "getInstance");
            Object platform = invoke(worldGuard, "getPlatform");
            Object sessionManager = invoke(platform, "getSessionManager");
            Object bypassWorld = invoke(localPlayer, "getWorld");
            if (Boolean.TRUE.equals(invoke(sessionManager, "hasBypass", localPlayer, bypassWorld))) {
                return null;
            }

            Object query = invoke(invoke(platform, "getRegionContainer"), "createQuery");
            Object adaptedLocation = invokeStatic(Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter"), "adapt", destination);
            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            Object buildFlag = findField(Class.forName("com.sk89q.worldguard.protection.flags.Flags"), "BUILD").get(null);
            Object flags = Array.newInstance(stateFlagClass, 1);
            Array.set(flags, 0, buildFlag);
            if (Boolean.TRUE.equals(invoke(query, "testBuild", adaptedLocation, localPlayer, flags))) {
                return null;
            }

            String regionName = stringMethod(region, "getId");
            return blockedMessage(displayName(), regionName, "");
        }

        private Object firstNonGlobalRegion(Location location) throws Exception {
            Object worldGuard = invokeStatic(Class.forName("com.sk89q.worldguard.WorldGuard"), "getInstance");
            Object platform = invoke(worldGuard, "getPlatform");
            Object query = invoke(invoke(platform, "getRegionContainer"), "createQuery");
            Object adaptedLocation = invokeStatic(Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter"), "adapt", location);
            Object applicable = invoke(query, "getApplicableRegions", adaptedLocation);
            @SuppressWarnings("unchecked")
            Set<Object> regions = (Set<Object>) invoke(applicable, "getRegions");
            for (Object region : regions) {
                if (!WORLDGUARD_GLOBAL_REGION.equalsIgnoreCase(stringMethod(region, "getId"))) {
                    return region;
                }
            }
            return null;
        }
    }

    private final class GriefPreventionAdapter extends ProtectionAdapter {
        private final Plugin griefPreventionPlugin = plugin.getServer().getPluginManager().getPlugin("GriefPrevention");

        @Override
        String id() {
            return "griefprevention";
        }

        @Override
        String displayName() {
            return "GriefPrevention";
        }

        @Override
        boolean isAvailable() throws Exception {
            return griefPreventionPlugin != null
                && griefPreventionPlugin.isEnabled()
                && Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention") != null;
        }

        @Override
        String version() {
            return griefPreventionPlugin == null ? "unknown" : griefPreventionPlugin.getDescription().getVersion();
        }

        @Override
        ProtectionMatch describe(Location location) throws Exception {
            Object claim = claimAt(location);
            if (claim == null) {
                return null;
            }
            String owner = stringMethod(claim, "getOwnerName");
            String name = booleanMethod(claim, "isAdminClaim") ? "Admin Claim" : "Player Claim";
            return new ProtectionMatch(id(), displayName(), name, owner);
        }

        @Override
        String checkTeleportAccess(Player player, Location destination) throws Exception {
            Object claim = claimAt(destination);
            if (claim == null || player.hasPermission("griefprevention.ignoreclaims")) {
                return null;
            }
            Object denial = invoke(claim, "allowAccess", player);
            if (denial == null) {
                return null;
            }
            String owner = stringMethod(claim, "getOwnerName");
            String areaName = booleanMethod(claim, "isAdminClaim") ? "Admin Claim" : "Player Claim";
            return blockedMessage(displayName(), areaName, owner);
        }

        private Object claimAt(Location location) throws Exception {
            Field dataStoreField = findField(griefPreventionPlugin.getClass(), "dataStore");
            Object dataStore = dataStoreField.get(griefPreventionPlugin);
            return invoke(dataStore, "getClaimAt", location, true, null);
        }
    }

    private final class LandsAdapter extends ProtectionAdapter {
        private final Plugin landsPlugin = plugin.getServer().getPluginManager().getPlugin("Lands");

        @Override
        String id() {
            return "lands";
        }

        @Override
        String displayName() {
            return "Lands";
        }

        @Override
        boolean isAvailable() throws Exception {
            return landsPlugin != null
                && landsPlugin.isEnabled()
                && Class.forName("me.angeschossen.lands.api.LandsIntegration") != null;
        }

        @Override
        String version() {
            return landsPlugin == null ? "unknown" : landsPlugin.getDescription().getVersion();
        }

        @Override
        ProtectionMatch describe(Location location) throws Exception {
            Object area = areaAt(location);
            if (area == null) {
                return null;
            }
            Object land = invoke(area, "getLand");
            String landName = land == null ? "" : stringMethod(land, "getName");
            String areaName = stringMethod(area, "getName");
            UUID ownerId = (UUID) invoke(area, "getOwnerUID");
            return new ProtectionMatch(id(), displayName(), combineAreaName(landName, areaName), playerName(ownerId));
        }

        @Override
        String checkTeleportAccess(Player player, Location destination) throws Exception {
            Object area = areaAt(destination);
            if (area == null || player.hasPermission("lands.admin")) {
                return null;
            }
            UUID ownerId = (UUID) invoke(area, "getOwnerUID");
            if (Objects.equals(ownerId, player.getUniqueId())) {
                return null;
            }

            Object integration = integration();
            Object landPlayer = invoke(integration, "getLandPlayer", player.getUniqueId());
            boolean allowed;
            if (landPlayer != null) {
                allowed = Boolean.TRUE.equals(invoke(area, "canEnter", landPlayer, false));
            } else {
                allowed = Boolean.TRUE.equals(invoke(area, "isTrusted", player.getUniqueId()));
            }
            if (allowed) {
                return null;
            }

            Object land = invoke(area, "getLand");
            String landName = land == null ? "" : stringMethod(land, "getName");
            String areaName = stringMethod(area, "getName");
            return blockedMessage(displayName(), combineAreaName(landName, areaName), playerName(ownerId));
        }

        private Object integration() throws Exception {
            return invokeStatic(Class.forName("me.angeschossen.lands.api.LandsIntegration"), "of", plugin);
        }

        private Object areaAt(Location location) throws Exception {
            return invoke(integration(), "getArea", location);
        }

        private String combineAreaName(String landName, String areaName) {
            if (areaName.isBlank()) {
                return landName;
            }
            if (landName.isBlank()) {
                return areaName;
            }
            if ("default".equalsIgnoreCase(areaName) || landName.equalsIgnoreCase(areaName)) {
                return landName;
            }
            return landName + " / " + areaName;
        }
    }

    private final class ResidenceAdapter extends ProtectionAdapter {
        private final Plugin residencePlugin = plugin.getServer().getPluginManager().getPlugin("Residence");

        @Override
        String id() {
            return "residence";
        }

        @Override
        String displayName() {
            return "Residence";
        }

        @Override
        boolean isAvailable() throws Exception {
            return residencePlugin != null
                && residencePlugin.isEnabled()
                && Class.forName("com.bekvon.bukkit.residence.api.ResidenceApi") != null;
        }

        @Override
        String version() {
            return residencePlugin == null ? "unknown" : residencePlugin.getDescription().getVersion();
        }

        @Override
        ProtectionMatch describe(Location location) throws Exception {
            Object residence = residenceAt(location);
            if (residence == null) {
                return null;
            }
            return new ProtectionMatch(id(), displayName(), stringMethod(residence, "getName"), stringMethod(residence, "getOwner"));
        }

        @Override
        String checkTeleportAccess(Player player, Location destination) throws Exception {
            Object residence = residenceAt(destination);
            if (residence == null || player.hasPermission("residence.admin") || player.hasPermission("residence.admin.tp")) {
                return null;
            }

            Object permissions = invoke(residence, "getPermissions");
            if (Boolean.TRUE.equals(invoke(permissions, "hasResidencePermission", player, true))) {
                return null;
            }

            Class<?> flagsClass = Class.forName("com.bekvon.bukkit.residence.containers.Flags");
            Class<?> flagComboClass = Class.forName("com.bekvon.bukkit.residence.containers.FlagCombo");
            Object tpFlag = findField(flagsClass, "tp").get(null);
            Object moveFlag = findField(flagsClass, "move").get(null);
            Object trueOrNone = findField(flagComboClass, "TrueOrNone").get(null);
            boolean canTeleport = Boolean.TRUE.equals(invoke(permissions, "playerHas", player, tpFlag, trueOrNone));
            boolean canMove = Boolean.TRUE.equals(invoke(permissions, "playerHas", player, moveFlag, trueOrNone));
            if (canTeleport && canMove) {
                return null;
            }

            return blockedMessage(displayName(), stringMethod(residence, "getName"), stringMethod(residence, "getOwner"));
        }

        private Object residenceAt(Location location) throws Exception {
            Object manager = invokeStatic(Class.forName("com.bekvon.bukkit.residence.api.ResidenceApi"), "getResidenceManager");
            return invoke(manager, "getByLoc", location);
        }
    }

    private final class TownyAdapter extends ProtectionAdapter {
        private final Plugin townyPlugin = plugin.getServer().getPluginManager().getPlugin("Towny");

        @Override
        String id() {
            return "towny";
        }

        @Override
        String displayName() {
            return "Towny";
        }

        @Override
        boolean isAvailable() throws Exception {
            return townyPlugin != null
                && townyPlugin.isEnabled()
                && Class.forName("com.palmergames.bukkit.towny.TownyAPI") != null;
        }

        @Override
        String version() {
            return townyPlugin == null ? "unknown" : townyPlugin.getDescription().getVersion();
        }

        @Override
        ProtectionMatch describe(Location location) throws Exception {
            Object townyApi = invokeStatic(Class.forName("com.palmergames.bukkit.towny.TownyAPI"), "getInstance");
            if (Boolean.TRUE.equals(invoke(townyApi, "isWilderness", location))) {
                return null;
            }
            String townName = stringValue(invoke(townyApi, "getTownName", location));
            return new ProtectionMatch(id(), displayName(), townName.isBlank() ? "Claimed Town Block" : townName, "");
        }

        @Override
        String checkTeleportAccess(Player player, Location destination) throws Exception {
            if (player.hasPermission("towny.admin")) {
                return null;
            }
            Object townyApi = invokeStatic(Class.forName("com.palmergames.bukkit.towny.TownyAPI"), "getInstance");
            if (Boolean.TRUE.equals(invoke(townyApi, "isWilderness", destination))) {
                return null;
            }

            Class<?> actionTypeClass = Class.forName("com.palmergames.bukkit.towny.object.TownyPermission$ActionType");
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object buildAction = Enum.valueOf((Class<? extends Enum>) actionTypeClass.asSubclass(Enum.class), "BUILD");
            boolean allowed = Boolean.TRUE.equals(invokeStatic(
                Class.forName("com.palmergames.bukkit.towny.utils.PlayerCacheUtil"),
                "getCachePermission",
                player,
                destination,
                Material.STONE,
                buildAction
            ));
            if (allowed) {
                return null;
            }

            String townName = stringValue(invoke(townyApi, "getTownName", destination));
            return blockedMessage(displayName(), townName.isBlank() ? "Claimed Town Block" : townName, "");
        }
    }

    private final class PlotSquaredAdapter extends ProtectionAdapter {
        private final Plugin plotSquaredPlugin = plugin.getServer().getPluginManager().getPlugin("PlotSquared");

        @Override
        String id() {
            return "plotsquared";
        }

        @Override
        String displayName() {
            return "PlotSquared";
        }

        @Override
        boolean isAvailable() throws Exception {
            return plotSquaredPlugin != null
                && plotSquaredPlugin.isEnabled()
                && Class.forName("com.plotsquared.core.plot.Plot") != null;
        }

        @Override
        String version() {
            return plotSquaredPlugin == null ? "unknown" : plotSquaredPlugin.getDescription().getVersion();
        }

        @Override
        ProtectionMatch describe(Location location) throws Exception {
            Object plot = plotAt(location);
            if (plot == null || !Boolean.TRUE.equals(invoke(plot, "hasOwner"))) {
                return null;
            }
            UUID ownerId = (UUID) invoke(plot, "getOwner");
            return new ProtectionMatch(id(), displayName(), "Plot " + stringValue(invoke(plot, "getId")), playerName(ownerId));
        }

        @Override
        String checkTeleportAccess(Player player, Location destination) throws Exception {
            Object plot = plotAt(destination);
            if (plot == null || !Boolean.TRUE.equals(invoke(plot, "hasOwner"))) {
                return null;
            }
            if (player.hasPermission("plots.admin") || player.hasPermission("plots.admin.*")) {
                return null;
            }
            if (Boolean.TRUE.equals(invoke(plot, "isAdded", player.getUniqueId()))) {
                return null;
            }
            UUID ownerId = (UUID) invoke(plot, "getOwner");
            return blockedMessage(displayName(), "Plot " + stringValue(invoke(plot, "getId")), playerName(ownerId));
        }

        private Object plotAt(Location location) throws Exception {
            Object plotLocation = invokeStatic(Class.forName("com.plotsquared.bukkit.util.BukkitUtil"), "adapt", location);
            return invokeStatic(Class.forName("com.plotsquared.core.plot.Plot"), "getPlot", plotLocation);
        }
    }

    public record ProviderStatus(String id, String name, String version) {
    }

    public record ProtectionMatch(String providerId, String providerName, String areaName, String ownerName) {
    }
}
