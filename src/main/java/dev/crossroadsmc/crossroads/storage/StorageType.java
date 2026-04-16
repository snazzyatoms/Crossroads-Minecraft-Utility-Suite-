package dev.crossroadsmc.crossroads.storage;

public enum StorageType {
    YAML,
    SQLITE,
    MYSQL;

    public static StorageType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return YAML;
        }

        try {
            return StorageType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return YAML;
        }
    }
}
