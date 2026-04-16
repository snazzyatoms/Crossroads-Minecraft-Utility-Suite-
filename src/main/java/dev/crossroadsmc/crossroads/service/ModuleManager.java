package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.api.module.CrossroadsModule;
import dev.crossroadsmc.crossroads.api.module.CrossroadsModuleContext;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;

public final class ModuleManager {
    private final CrossroadsPlugin plugin;
    private final List<CrossroadsModule> modules = new ArrayList<>();
    private final List<URLClassLoader> classLoaders = new ArrayList<>();

    public ModuleManager(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadModules() {
        File moduleDirectory = new File(plugin.getDataFolder(), "modules");
        if (!moduleDirectory.exists()) {
            moduleDirectory.mkdirs();
        }

        File[] jars = moduleDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null) {
            return;
        }

        CrossroadsModuleContext context = new CrossroadsModuleContext(plugin);
        for (File jar : jars) {
            try {
                URLClassLoader classLoader = new URLClassLoader(new URL[] {jar.toURI().toURL()}, plugin.getClass().getClassLoader());
                classLoaders.add(classLoader);
                ServiceLoader<CrossroadsModule> serviceLoader = ServiceLoader.load(CrossroadsModule.class, classLoader);
                for (CrossroadsModule module : serviceLoader) {
                    module.onLoad(context);
                    module.onEnable(context);
                    modules.add(module);
                    plugin.getLogger().info("Loaded Crossroads module: " + module.getId());
                }
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Unable to load Crossroads module jar " + jar.getName() + ".", exception);
            }
        }
    }

    public List<CrossroadsModule> getModules() {
        return List.copyOf(modules);
    }

    public void shutdown() {
        CrossroadsModuleContext context = new CrossroadsModuleContext(plugin);
        for (CrossroadsModule module : modules) {
            try {
                module.onDisable(context);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Unable to disable Crossroads module " + module.getId() + ".", exception);
            }
        }
        modules.clear();

        for (URLClassLoader classLoader : classLoaders) {
            try {
                classLoader.close();
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Unable to close module class loader.", exception);
            }
        }
        classLoaders.clear();
    }
}
