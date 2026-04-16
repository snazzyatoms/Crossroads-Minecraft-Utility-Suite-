package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.util.Chat;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class WelcomeService {
    private final CrossroadsPlugin plugin;

    public WelcomeService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    public void welcome(Player player) {
        if (!plugin.getConfig().getBoolean("welcome.enabled", true)) {
            return;
        }

        boolean firstJoin = !player.hasPlayedBefore();
        for (String line : resolveMessages(player, firstJoin)) {
            player.sendMessage(Chat.color(plugin, line
                .replace("%player%", player.getName())
                .replace("%world%", player.getWorld().getName())));
        }
    }

    private List<String> resolveMessages(Player player, boolean firstJoin) {
        ConfigurationSection perWorld = plugin.getConfig().getConfigurationSection("welcome.per-world." + player.getWorld().getName());
        if (perWorld != null) {
            List<String> lines = perWorld.getStringList(firstJoin ? "first-join" : "returning");
            if (!lines.isEmpty()) {
                return lines;
            }
        }

        return plugin.getConfig().getStringList(firstJoin ? "welcome.first-join" : "welcome.returning");
    }
}
