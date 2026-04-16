package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class AegisGuardHookService {
    private final CrossroadsPlugin plugin;
    private HookState state = HookState.unavailable();

    public AegisGuardHookService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        Plugin aegisPlugin = plugin.getServer().getPluginManager().getPlugin("AegisGuard");
        if (aegisPlugin == null || !aegisPlugin.isEnabled()) {
            state = HookState.unavailable();
            return;
        }

        try {
            Class<?> aegisClass = Class.forName("com.aegisguard.AegisGuard");
            Class<?> apiClass = Class.forName("com.aegisguard.api.AegisGuardAPI");
            Object instance = invokeStatic(aegisClass, "getInstance");
            if (instance == null) {
                instance = aegisPlugin;
            }

            Object api = invoke(instance, "api");
            if (api == null) {
                api = invoke(instance, "getApi");
            }
            if (api == null || !apiClass.isInstance(api)) {
                state = HookState.unavailable();
                return;
            }

            Object plots = invoke(api, "plots");
            Object claimBlocks = invoke(api, "claimBlocks");
            state = new HookState(
                true,
                aegisPlugin,
                String.valueOf(invoke(api, "version")),
                plots,
                claimBlocks,
                plots == null ? null : plots.getClass().getMethod("getPlotAt", Location.class),
                claimBlocks == null ? null : claimBlocks.getClass().getMethod("getAvailableBlocks", UUID.class)
            );
        } catch (Exception exception) {
            plugin.getLogger().warning("AegisGuard hook failed to initialize: " + exception.getMessage());
            state = HookState.unavailable();
        }
    }

    public boolean isAvailable() {
        return state.available();
    }

    public String getVersion() {
        return state.version();
    }

    public PlotInfo getPlotInfo(Location location) {
        Object plot = rawPlot(location);
        if (plot == null) {
            return null;
        }

        String name = stringMethod(plot, "getPlotName");
        if (name == null || name.isBlank()) {
            name = "Unnamed Plot";
        }
        String owner = stringMethod(plot, "getOwnerName");
        if (owner == null || owner.isBlank()) {
            owner = "Unknown";
        }
        return new PlotInfo(name, owner, stringMethod(plot, "getWorld"));
    }

    public long getAvailableClaimBlocks(UUID playerId) {
        if (!state.available() || state.claimBlocks() == null || state.availableBlocksMethod() == null || playerId == null) {
            return 0L;
        }
        try {
            Object result = state.availableBlocksMethod().invoke(state.claimBlocks(), playerId);
            return result instanceof Number number ? number.longValue() : 0L;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return 0L;
        }
    }

    public boolean spendClaimBlocks(UUID playerId, long amount) {
        if (!state.available() || state.claimBlocks() == null || playerId == null || amount <= 0L) {
            return false;
        }
        try {
            Method spend = state.claimBlocks().getClass().getMethod("spend", UUID.class, long.class);
            Object result = spend.invoke(state.claimBlocks(), playerId, amount);
            return Boolean.TRUE.equals(result);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return false;
        }
    }

    public String checkTeleportAccess(Player player, Location destination) {
        if (!state.available() || player == null || destination == null || !plugin.getConfig().getBoolean("aegisguard.teleport-respect-entry", true)) {
            return null;
        }

        Object plot = rawPlot(destination);
        if (plot == null) {
            return null;
        }

        if (booleanMethod(plot, "isOwner", player) || booleanMethod(plot, "isTrusted", player)) {
            return null;
        }
        if (booleanMethod(plot, "getFlag", "entry", true)) {
            return null;
        }

        String name = stringMethod(plot, "getPlotName");
        String owner = stringMethod(plot, "getOwnerName");
        return "AegisGuard blocked that teleport. Plot: " + (name == null || name.isBlank() ? "Unnamed Plot" : name)
            + " | Owner: " + (owner == null || owner.isBlank() ? "Unknown" : owner);
    }

    public boolean isProtected(Location location) {
        return rawPlot(location) != null;
    }

    private Object rawPlot(Location location) {
        if (!state.available() || state.plots() == null || state.plotAtMethod() == null || location == null) {
            return null;
        }
        try {
            return state.plotAtMethod().invoke(state.plots(), location);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return null;
        }
    }

    private static Object invokeStatic(Class<?> type, String methodName) {
        try {
            Method method = type.getMethod(methodName);
            return method.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return null;
        }
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return null;
        }
    }

    private static String stringMethod(Object target, String methodName) {
        Object result = invoke(target, methodName);
        return result == null ? "" : String.valueOf(result);
    }

    private static boolean booleanMethod(Object target, String methodName, Object... args) {
        if (target == null) {
            return false;
        }
        try {
            Class<?>[] types = new Class<?>[args.length];
            for (int index = 0; index < args.length; index++) {
                Object arg = args[index];
                if (arg instanceof String) {
                    types[index] = String.class;
                } else if (arg instanceof Boolean) {
                    types[index] = boolean.class;
                } else if (arg instanceof Player) {
                    types[index] = Player.class;
                } else {
                    types[index] = arg.getClass();
                }
            }
            Method method = target.getClass().getMethod(methodName, types);
            Object result = method.invoke(target, args);
            return Boolean.TRUE.equals(result);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return false;
        }
    }

    public record PlotInfo(String name, String ownerName, String worldName) {
    }

    private record HookState(
        boolean available,
        Plugin plugin,
        String version,
        Object plots,
        Object claimBlocks,
        Method plotAtMethod,
        Method availableBlocksMethod
    ) {
        private static HookState unavailable() {
            return new HookState(false, null, "unavailable", null, null, null, null);
        }
    }
}
