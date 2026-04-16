package dev.crossroadsmc.crossroads.util;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Chat {
    private Chat() {
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String prefix(CrossroadsPlugin plugin) {
        return color(plugin.getConfig().getString("messages.prefix", "&6Crossroads &8» &7"));
    }

    public static void send(CrossroadsPlugin plugin, CommandSender sender, String message) {
        sender.sendMessage(prefix(plugin) + color(message));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    public static List<String> colorize(List<String> input) {
        List<String> lines = new ArrayList<>();
        for (String line : input) {
            lines.add(color(line));
        }
        return lines;
    }
}
