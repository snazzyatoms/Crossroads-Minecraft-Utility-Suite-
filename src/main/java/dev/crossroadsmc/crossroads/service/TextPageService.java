package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.util.Chat;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class TextPageService {
    private final CrossroadsPlugin plugin;
    private final File motdFile;
    private final File helpFile;
    private final File infoFile;
    private YamlConfiguration motd = new YamlConfiguration();
    private YamlConfiguration help = new YamlConfiguration();
    private YamlConfiguration info = new YamlConfiguration();

    public TextPageService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        this.motdFile = new File(plugin.getDataFolder(), "motd.yml");
        this.helpFile = new File(plugin.getDataFolder(), "help.yml");
        this.infoFile = new File(plugin.getDataFolder(), "info.yml");
        reload();
    }

    public void reload() {
        motd = YamlConfiguration.loadConfiguration(motdFile);
        help = YamlConfiguration.loadConfiguration(helpFile);
        info = YamlConfiguration.loadConfiguration(infoFile);
    }

    public void sendMotd(CommandSender sender, Player viewer) {
        sendLines(sender, resolveText(motd, "lines", viewer), motd.getString("title", "<warn>Message of the Day"), viewer);
    }

    public void sendHelp(CommandSender sender, Player viewer, String page) {
        String effectivePage = page == null || page.isBlank()
            ? help.getString("default-page", "general")
            : page.toLowerCase(Locale.ROOT);
        List<String> lines = resolvePage(help, effectivePage, viewer);
        if (lines.isEmpty()) {
            Chat.send(plugin, sender, "<error>Unknown help page <warn>" + effectivePage + "<error>.");
            Chat.send(plugin, sender, "<subtle>Available pages: <text>" + String.join(", ", availablePages(help)));
            return;
        }
        sendLines(sender, lines, help.getString("title", "<warn>Crossroads Help") + " <subtle>(" + effectivePage + ")", viewer);
    }

    public void sendInfo(CommandSender sender, Player viewer, String page) {
        String effectivePage = page == null || page.isBlank()
            ? info.getString("default-page", "about")
            : page.toLowerCase(Locale.ROOT);
        List<String> lines = resolvePage(info, effectivePage, viewer);
        if (lines.isEmpty()) {
            Chat.send(plugin, sender, "<error>Unknown info page <warn>" + effectivePage + "<error>.");
            Chat.send(plugin, sender, "<subtle>Available pages: <text>" + String.join(", ", availablePages(info)));
            return;
        }
        sendLines(sender, lines, info.getString("title", "<warn>Crossroads Info") + " <subtle>(" + effectivePage + ")", viewer);
    }

    public Set<String> availableHelpPages() {
        return availablePages(help);
    }

    public Set<String> availableInfoPages() {
        return availablePages(info);
    }

    private void sendLines(CommandSender sender, List<String> lines, String title, Player viewer) {
        if (lines.isEmpty()) {
            Chat.send(plugin, sender, "<subtle>No text has been configured yet.");
            return;
        }
        Chat.send(plugin, sender, title);
        for (String line : lines) {
            Chat.sendRaw(plugin, sender, "<muted>- <subtle>" + replace(line, viewer));
        }
    }

    private List<String> resolvePage(YamlConfiguration yaml, String page, Player viewer) {
        String basePath = "pages." + page + ".lines";
        List<String> scoped = resolveText(yaml, basePath, viewer);
        return scoped.isEmpty() ? Collections.emptyList() : scoped;
    }

    private List<String> resolveText(YamlConfiguration yaml, String basePath, Player viewer) {
        if (viewer != null) {
            ConfigurationSection worldSection = yaml.getConfigurationSection("per-world." + viewer.getWorld().getName());
            if (worldSection != null) {
                List<String> lines = worldSection.getStringList(basePath);
                if (!lines.isEmpty()) {
                    return lines;
                }
            }
        }
        return yaml.getStringList(basePath);
    }

    private Set<String> availablePages(YamlConfiguration yaml) {
        ConfigurationSection section = yaml.getConfigurationSection("pages");
        if (section == null) {
            return Collections.emptySet();
        }
        Set<String> pages = new TreeSet<>();
        pages.addAll(section.getKeys(false));
        return Collections.unmodifiableSet(pages);
    }

    private String replace(String input, Player viewer) {
        if (viewer == null) {
            return input.replace("%player%", "Console").replace("%world%", "server").replace("%profile%", "global");
        }
        return input
            .replace("%player%", viewer.getName())
            .replace("%world%", viewer.getWorld().getName())
            .replace("%profile%", plugin.getWorldProfileService().resolveProfile(viewer.getWorld()));
    }
}
