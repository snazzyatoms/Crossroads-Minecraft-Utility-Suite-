package dev.crossroadsmc.crossroads.util;

import org.bukkit.Location;

public final class LocationFormatter {
    private LocationFormatter() {
    }

    public static String human(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }

        return location.getWorld().getName()
            + " @ "
            + Math.round(location.getX()) + ", "
            + Math.round(location.getY()) + ", "
            + Math.round(location.getZ());
    }
}
