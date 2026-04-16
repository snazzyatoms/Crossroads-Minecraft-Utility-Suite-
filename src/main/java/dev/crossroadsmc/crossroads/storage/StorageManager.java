package dev.crossroadsmc.crossroads.storage;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import java.util.logging.Level;

public final class StorageManager {
    private final CrossroadsPlugin plugin;
    private StorageProvider provider;

    public StorageManager(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        StorageType requested = StorageType.from(plugin.getConfig().getString("storage.type", "YAML"));
        StorageProvider candidate = requested == StorageType.YAML
            ? new YamlStorageProvider(plugin)
            : new JdbcStorageProvider(plugin, requested);

        try {
            candidate.initialize();
            this.provider = candidate;
            plugin.getLogger().info("Storage backend ready: " + provider.getType());
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Falling back to YAML storage after " + requested + " setup failed.", exception);
            this.provider = new YamlStorageProvider(plugin);
            try {
                provider.initialize();
            } catch (Exception yamlException) {
                throw new IllegalStateException("Unable to initialize fallback YAML storage.", yamlException);
            }
        }
    }

    public StorageProvider getProvider() {
        return provider;
    }

    public void shutdown() {
        if (provider != null) {
            provider.close();
        }
    }
}
