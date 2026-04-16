package dev.crossroadsmc.crossroads.model;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public final class InventorySnapshot {
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack offHand;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean flying;
    private final float experience;
    private final int level;
    private final Location location;

    public InventorySnapshot(
        ItemStack[] contents,
        ItemStack[] armor,
        ItemStack offHand,
        GameMode gameMode,
        boolean allowFlight,
        boolean flying,
        float experience,
        int level,
        Location location
    ) {
        this.contents = contents;
        this.armor = armor;
        this.offHand = offHand;
        this.gameMode = gameMode;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.experience = experience;
        this.level = level;
        this.location = location;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public ItemStack[] getArmor() {
        return armor;
    }

    public ItemStack getOffHand() {
        return offHand;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public boolean isAllowFlight() {
        return allowFlight;
    }

    public boolean isFlying() {
        return flying;
    }

    public float getExperience() {
        return experience;
    }

    public int getLevel() {
        return level;
    }

    public Location getLocation() {
        return location;
    }
}
