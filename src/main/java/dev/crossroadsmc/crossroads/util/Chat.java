package dev.crossroadsmc.crossroads.util;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Chat {
    private Chat() {
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String color(CrossroadsPlugin plugin, String message) {
        return color(applyPalette(plugin, message));
    }

    public static String prefix(CrossroadsPlugin plugin) {
        return color(plugin, plugin.getConfig().getString("messages.prefix", "<accent>Crossroads <muted>» <text>"));
    }

    public static void send(CrossroadsPlugin plugin, CommandSender sender, String message) {
        sender.sendMessage(prefix(plugin) + color(plugin, message));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    public static void sendRaw(CrossroadsPlugin plugin, CommandSender sender, String message) {
        sender.sendMessage(color(plugin, message));
    }

    public static List<String> colorize(List<String> input) {
        List<String> lines = new ArrayList<>();
        for (String line : input) {
            lines.add(color(line));
        }
        return lines;
    }

    public static String applyPalette(CrossroadsPlugin plugin, String message) {
        if (message == null) {
            return "";
        }

        Map<String, String> palette = new LinkedHashMap<>();
        palette.put("<error>", plugin.getConfig().getString("messages.palette.error", "&c"));
        palette.put("<success>", plugin.getConfig().getString("messages.palette.success", "&a"));
        palette.put("<warn>", plugin.getConfig().getString("messages.palette.warn", "&e"));
        palette.put("<info>", plugin.getConfig().getString("messages.palette.info", "&b"));
        palette.put("<accent>", plugin.getConfig().getString("messages.palette.accent", "&6"));
        palette.put("<text>", plugin.getConfig().getString("messages.palette.text", "&f"));
        palette.put("<subtle>", plugin.getConfig().getString("messages.palette.subtle", "&7"));
        palette.put("<muted>", plugin.getConfig().getString("messages.palette.muted", "&8"));
        palette.put("<highlight>", plugin.getConfig().getString("messages.palette.highlight", "&d"));
        palette.put("<reset>", "&r");

        String output = message;
        for (Map.Entry<String, String> entry : palette.entrySet()) {
            output = output.replace(entry.getKey(), entry.getValue());
        }
        return output;
    }
}
