package dev.crossroadsmc.crossroads.model;

public final class StaffModeSession {
    private final InventorySnapshot snapshot;
    private final boolean vanishedBefore;
    private final boolean flyBefore;

    public StaffModeSession(InventorySnapshot snapshot, boolean vanishedBefore, boolean flyBefore) {
        this.snapshot = snapshot;
        this.vanishedBefore = vanishedBefore;
        this.flyBefore = flyBefore;
    }

    public InventorySnapshot getSnapshot() {
        return snapshot;
    }

    public boolean wasVanishedBefore() {
        return vanishedBefore;
    }

    public boolean wasFlyBefore() {
        return flyBefore;
    }
}
