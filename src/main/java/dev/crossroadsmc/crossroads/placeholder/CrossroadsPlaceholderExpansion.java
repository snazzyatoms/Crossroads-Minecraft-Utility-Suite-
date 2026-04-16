package dev.crossroadsmc.crossroads.placeholder;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class CrossroadsPlaceholderExpansion extends PlaceholderExpansion {
    private final CrossroadsPlugin plugin;

    public CrossroadsPlaceholderExpansion(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "crossroads";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("storage")) {
            return plugin.getStorageManager().getProvider().getType().name();
        }
        if (params.equalsIgnoreCase("economy_provider")) {
            return plugin.getEconomyService().getProviderName();
        }
        if (params.equalsIgnoreCase("aegis_enabled")) {
            return String.valueOf(plugin.getAegisGuardHookService().isAvailable());
        }
        if (params.equalsIgnoreCase("aegis_version")) {
            return plugin.getAegisGuardHookService().getVersion();
        }
        if (player == null || player.getUniqueId() == null) {
            return "";
        }

        PlayerData data = plugin.getPlayerDataService().get(player.getUniqueId());
        if (params.equalsIgnoreCase("nick")) {
            return data.getNickname().isBlank() ? data.getLastKnownName() : data.getNickname();
        }
        if (params.equalsIgnoreCase("mail_unread")) {
            return String.valueOf(data.getUnreadMailCount());
        }
        if (params.equalsIgnoreCase("muted")) {
            return String.valueOf(data.isMuted());
        }
        if (params.equalsIgnoreCase("shadow_muted")) {
            return String.valueOf(data.isShadowMuted());
        }
        if (params.equalsIgnoreCase("jailed")) {
            return String.valueOf(data.isJailed());
        }
        if (params.equalsIgnoreCase("homes_global")) {
            return String.valueOf(data.getHomeCount(PlayerData.GLOBAL_SCOPE));
        }
        if (params.equalsIgnoreCase("balance")) {
            return plugin.getEconomyService().format(plugin.getEconomyService().getBalance(player));
        }
        if (params.equalsIgnoreCase("aegis_claimblocks_available")) {
            return String.valueOf(plugin.getAegisGuardHookService().getAvailableClaimBlocks(player.getUniqueId()));
        }
        if (player.isOnline() && player.getPlayer() != null) {
            var plotInfo = plugin.getAegisGuardHookService().getPlotInfo(player.getPlayer().getLocation());
            if (params.equalsIgnoreCase("aegis_plot_name")) {
                return plotInfo == null ? "" : plotInfo.name();
            }
            if (params.equalsIgnoreCase("aegis_plot_owner")) {
                return plotInfo == null ? "" : plotInfo.ownerName();
            }
        }
        return null;
    }
}
