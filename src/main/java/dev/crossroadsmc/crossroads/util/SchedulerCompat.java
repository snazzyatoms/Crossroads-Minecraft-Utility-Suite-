package dev.crossroadsmc.crossroads.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class SchedulerCompat {
    private static final boolean FOLIA = detectFolia();
    private static final Method ENTITY_GET_SCHEDULER = findMethod(Entity.class, "getScheduler");
    private static final Method ENTITY_SCHEDULER_RUN = findSchedulerMethod(
        "io.papermc.paper.threadedregions.scheduler.EntityScheduler", "run",
        Plugin.class, Consumer.class, Runnable.class);
    private static final SchedulerProvider GLOBAL_SCHEDULER = findSchedulerProvider("getGlobalRegionScheduler");
    private static final Method GLOBAL_SCHEDULER_RUN = findSchedulerMethod(
        "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler", "run",
        Plugin.class, Consumer.class);
    private static final SchedulerProvider REGION_SCHEDULER = findSchedulerProvider("getRegionScheduler");
    private static final Method REGION_SCHEDULER_RUN = findSchedulerMethod(
        "io.papermc.paper.threadedregions.scheduler.RegionScheduler", "run",
        Plugin.class, World.class, int.class, int.class, Consumer.class);
    private static final SchedulerProvider ASYNC_SCHEDULER = findSchedulerProvider("getAsyncScheduler");
    private static final Method ASYNC_SCHEDULER_RUN_NOW = findSchedulerMethod(
        "io.papermc.paper.threadedregions.scheduler.AsyncScheduler", "runNow",
        Plugin.class, Consumer.class);
    private static final Method ASYNC_SCHEDULER_RUN_AT_FIXED_RATE = findSchedulerMethod(
        "io.papermc.paper.threadedregions.scheduler.AsyncScheduler", "runAtFixedRate",
        Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
    private static final Method SCHEDULED_TASK_CANCEL = findSchedulerMethod(
        "io.papermc.paper.threadedregions.scheduler.ScheduledTask", "cancel");
    private static final Method ENTITY_TELEPORT_ASYNC = findMethod(Entity.class, "teleportAsync", Location.class);
    private static final Method WORLD_GET_CHUNK_AT_ASYNC = findMethod(World.class, "getChunkAtAsync", int.class, int.class);

    private SchedulerCompat() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runOnEntity(Plugin plugin, Entity entity, Runnable task) {
        if (entity == null) {
            return;
        }
        if (FOLIA && ENTITY_GET_SCHEDULER != null && ENTITY_SCHEDULER_RUN != null) {
            try {
                Object scheduler = ENTITY_GET_SCHEDULER.invoke(entity);
                ENTITY_SCHEDULER_RUN.invoke(scheduler, plugin, (Consumer<Object>) scheduledTask -> task.run(), null);
                return;
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                // Fall through to the Bukkit scheduler path.
            }
        }
        runOnMain(plugin, task);
    }

    public static void runGlobal(Plugin plugin, Runnable task) {
        if (FOLIA && GLOBAL_SCHEDULER != null && GLOBAL_SCHEDULER_RUN != null) {
            Object scheduler = GLOBAL_SCHEDULER.get();
            if (scheduler != null) {
                try {
                    GLOBAL_SCHEDULER_RUN.invoke(scheduler, plugin, (Consumer<Object>) scheduledTask -> task.run());
                    return;
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                    // Fall through to the Bukkit scheduler path.
                }
            }
        }
        runOnMain(plugin, task);
    }

    public static void runOnRegion(Plugin plugin, World world, int chunkX, int chunkZ, Runnable task) {
        if (FOLIA && REGION_SCHEDULER != null && REGION_SCHEDULER_RUN != null) {
            Object scheduler = REGION_SCHEDULER.get();
            if (scheduler != null) {
                try {
                    REGION_SCHEDULER_RUN.invoke(scheduler, plugin, world, chunkX, chunkZ,
                        (Consumer<Object>) scheduledTask -> task.run());
                    return;
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                    // Fall through to the Bukkit scheduler path.
                }
            }
        }
        runOnMain(plugin, task);
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (FOLIA && ASYNC_SCHEDULER != null && ASYNC_SCHEDULER_RUN_NOW != null) {
            Object scheduler = ASYNC_SCHEDULER.get();
            if (scheduler != null) {
                try {
                    ASYNC_SCHEDULER_RUN_NOW.invoke(scheduler, plugin, (Consumer<Object>) scheduledTask -> task.run());
                    return;
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                    // Fall through to the Bukkit scheduler path.
                }
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static ScheduledHandle runAtFixedRateAsync(Plugin plugin, Runnable task, long initialDelaySeconds, long periodSeconds) {
        if (FOLIA && ASYNC_SCHEDULER != null && ASYNC_SCHEDULER_RUN_AT_FIXED_RATE != null) {
            Object scheduler = ASYNC_SCHEDULER.get();
            if (scheduler != null) {
                try {
                    Object scheduledTask = ASYNC_SCHEDULER_RUN_AT_FIXED_RATE.invoke(scheduler, plugin,
                        (Consumer<Object>) tick -> task.run(),
                        Math.max(0L, initialDelaySeconds), Math.max(1L, periodSeconds), TimeUnit.SECONDS);
                    if (scheduledTask != null) {
                        return () -> cancelScheduledTask(scheduledTask);
                    }
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                    // Fall through to the Bukkit scheduler path.
                }
            }
        }
        long delayTicks = Math.max(0L, initialDelaySeconds) * 20L;
        long periodTicks = Math.max(1L, periodSeconds) * 20L;
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return bukkitTask::cancel;
    }

    public static CompletableFuture<Boolean> teleportAsync(Plugin plugin, Entity entity, Location destination) {
        if (entity == null || destination == null || destination.getWorld() == null) {
            return CompletableFuture.completedFuture(false);
        }
        if (ENTITY_TELEPORT_ASYNC != null) {
            try {
                @SuppressWarnings("unchecked")
                CompletableFuture<Boolean> result = (CompletableFuture<Boolean>) ENTITY_TELEPORT_ASYNC.invoke(entity, destination);
                if (result != null) {
                    return result;
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                // Fall through to the synchronous teleport path.
            }
        }
        if (FOLIA) {
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            runOnEntity(plugin, entity, () -> result.complete(entity.teleport(destination)));
            return result;
        }
        return CompletableFuture.completedFuture(entity.teleport(destination));
    }

    public static CompletableFuture<Chunk> getChunkAtAsync(Plugin plugin, World world, int chunkX, int chunkZ) {
        if (world == null) {
            CompletableFuture<Chunk> result = new CompletableFuture<>();
            result.completeExceptionally(new IllegalArgumentException("world is null"));
            return result;
        }
        if (WORLD_GET_CHUNK_AT_ASYNC != null) {
            try {
                @SuppressWarnings("unchecked")
                CompletableFuture<Chunk> result = (CompletableFuture<Chunk>) WORLD_GET_CHUNK_AT_ASYNC.invoke(world, chunkX, chunkZ);
                if (result != null) {
                    return result;
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                // Fall through to the region-thread chunk load path.
            }
        }
        CompletableFuture<Chunk> result = new CompletableFuture<>();
        Runnable load = () -> {
            try {
                result.complete(world.getChunkAt(chunkX, chunkZ));
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        };
        if (FOLIA) {
            runOnRegion(plugin, world, chunkX, chunkZ, load);
        } else {
            load.run();
        }
        return result;
    }

    private static void runOnMain(Plugin plugin, Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    private static void cancelScheduledTask(Object scheduledTask) {
        if (SCHEDULED_TASK_CANCEL == null) {
            return;
        }
        try {
            SCHEDULED_TASK_CANCEL.invoke(scheduledTask);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            // Nothing sensible left to do.
        }
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findSchedulerMethod(String className, String methodName, Class<?>... parameterTypes) {
        try {
            return Class.forName(className).getMethod(methodName, parameterTypes);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return null;
        }
    }

    private static SchedulerProvider findSchedulerProvider(String methodName) {
        Method bukkitMethod = findMethod(Bukkit.class, methodName);
        if (bukkitMethod != null) {
            return () -> invoke(bukkitMethod, null);
        }
        Method serverMethod = findMethod(Server.class, methodName);
        if (serverMethod != null) {
            return () -> invoke(serverMethod, Bukkit.getServer());
        }
        return null;
    }

    private static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    @FunctionalInterface
    public interface ScheduledHandle {
        void cancel();
    }

    @FunctionalInterface
    private interface SchedulerProvider {
        Object get();
    }
}
