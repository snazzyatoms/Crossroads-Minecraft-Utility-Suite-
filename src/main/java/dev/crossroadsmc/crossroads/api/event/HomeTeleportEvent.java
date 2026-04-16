package dev.crossroadsmc.crossroads.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class HomeTeleportEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String homeName;
    private Location destination;
    private boolean cancelled;

    public HomeTeleportEvent(Player player, String homeName, Location destination) {
        this.player = player;
        this.homeName = homeName;
        this.destination = destination;
    }

    public Player getPlayer() {
        return player;
    }

    public String getHomeName() {
        return homeName;
    }

    public Location getDestination() {
        return destination;
    }

    public void setDestination(Location destination) {
        this.destination = destination;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
