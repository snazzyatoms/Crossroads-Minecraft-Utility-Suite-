package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.util.Chat;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class LanguageService {
    public static final String[] BUNDLE_FILES = {
        "common.yml",
        "travel.yml",
        "social.yml",
        "kits.yml",
        "staff.yml",
        "moderation.yml",
        "admin.yml",
        "menus.yml",
        "economy.yml",
        "protection.yml",
        "permissions.yml",
        "welcome.yml"
    };

    private final CrossroadsPlugin plugin;
    private final Map<String, Map<String, String>> packs = new ConcurrentHashMap<>();
    private String defaultLanguage = "modern_english";
    private String fallbackLanguage = "modern_english";
    private boolean syncAegisGuard = true;
    private boolean allowPlayerLanguage = true;
    private List<String> availableLanguages = List.of("modern_english");

    public LanguageService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        defaultLanguage = normalize(plugin.getConfig().getString("localization.default_language", "modern_english"));
        fallbackLanguage = normalize(plugin.getConfig().getString("localization.fallback_language", "modern_english"));
        syncAegisGuard = plugin.getConfig().getBoolean("localization.sync_aegisguard", true);
        allowPlayerLanguage = plugin.getConfig().getBoolean("localization.allow_player_language", true);

        List<String> configured = plugin.getConfig().getStringList("localization.available_languages");
        LinkedHashSet<String> languages = new LinkedHashSet<>();
        if (configured.isEmpty()) {
            languages.add("modern_english");
            languages.add("french_fr");
            languages.add("spanish_mx");
            languages.add("spanish_ar");
            languages.add("portuguese_br");
            languages.add("italian_it");
            languages.add("german_de");
            languages.add("polish_pl");
        } else {
            for (String language : configured) {
                languages.add(normalize(language));
            }
        }
        languages.add(defaultLanguage);
        languages.add(fallbackLanguage);
        availableLanguages = List.copyOf(languages);

        if (plugin.getConfig().getBoolean("localization.extract_defaults", true)) {
            extractDefaults();
        }

        packs.clear();
        for (String language : availableLanguages) {
            packs.put(language, loadPack(language));
        }
    }

    public List<String> getAvailableLanguages() {
        return availableLanguages;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public String resolveStyle(CommandSender sender) {
        if (sender instanceof Player player) {
            if (allowPlayerLanguage) {
                PlayerData data = plugin.getPlayerDataService().get(player);
                String override = normalize(data.getLanguage());
                if (!override.isBlank() && packs.containsKey(override)) {
                    return override;
                }
            }

            if (syncAegisGuard) {
                String aegisStyle = plugin.getAegisGuardHookService().resolveLanguageStyle(player);
                if (aegisStyle != null) {
                    String normalized = normalize(aegisStyle);
                    if (packs.containsKey(normalized)) {
                        return normalized;
                    }
                }
            }
        }
        return packs.containsKey(defaultLanguage) ? defaultLanguage : fallbackLanguage;
    }

    public boolean setPlayerLanguage(Player player, String language) {
        String normalized = normalize(language);
        if (!packs.containsKey(normalized)) {
            return false;
        }
        PlayerData data = plugin.getPlayerDataService().get(player);
        data.setLanguage(normalized);
        plugin.getPlayerDataService().save(player);
        return true;
    }

    public void clearPlayerLanguage(Player player) {
        PlayerData data = plugin.getPlayerDataService().get(player);
        data.setLanguage("");
        plugin.getPlayerDataService().save(player);
    }

    public String get(CommandSender sender, String key, String... replacements) {
        String style = resolveStyle(sender);
        String message = lookup(style, key);
        if (message == null) {
            message = lookup(fallbackLanguage, key);
        }
        if (message == null) {
            message = lookup("modern_english", key);
        }
        if (message == null) {
            message = key;
        }
        return applyPlaceholders(message, replacements);
    }

    public void send(CommandSender sender, String key, String... replacements) {
        Chat.send(plugin, sender, get(sender, key, replacements));
    }

    public void sendRaw(CommandSender sender, String key, String... replacements) {
        Chat.sendRaw(plugin, sender, get(sender, key, replacements));
    }

    public String colorize(CommandSender sender, String key, String... replacements) {
        return Chat.color(plugin, get(sender, key, replacements));
    }

    private String lookup(String style, String key) {
        Map<String, String> pack = packs.get(style);
        if (pack == null) {
            return null;
        }
        return pack.get(key);
    }

    private String applyPlaceholders(String message, String... replacements) {
        if (message == null) {
            return "";
        }
        String output = message;
        if (replacements != null) {
            for (int index = 0; index + 1 < replacements.length; index += 2) {
                String placeholder = replacements[index];
                String value = replacements[index + 1] == null ? "" : replacements[index + 1];
                output = output.replace(placeholder, value);
            }
        }
        return output;
    }

    private void extractDefaults() {
        String folder = plugin.getConfig().getString("localization.folder", "lang");
        Path langRoot = plugin.getDataFolder().toPath().resolve(folder);
        for (String language : availableLanguages) {
            for (String bundle : BUNDLE_FILES) {
                String resourcePath = "lang/" + language + "/" + bundle;
                Path destination = langRoot.resolve(language).resolve(bundle);
                if (Files.exists(destination)) {
                    continue;
                }
                try (InputStream inputStream = plugin.getResource(resourcePath)) {
                    if (inputStream == null) {
                        continue;
                    }
                    Files.createDirectories(destination.getParent());
                    Files.copy(inputStream, destination);
                } catch (IOException exception) {
                    plugin.getLogger().log(Level.WARNING, "Unable to extract language pack " + resourcePath, exception);
                }
            }
        }
    }

    private Map<String, String> loadPack(String language) {
        Map<String, String> messages = new LinkedHashMap<>();
        String folder = plugin.getConfig().getString("localization.folder", "lang");
        Path langRoot = plugin.getDataFolder().toPath().resolve(folder).resolve(language);

        for (String bundle : BUNDLE_FILES) {
            YamlConfiguration yaml = new YamlConfiguration();
            Path diskFile = langRoot.resolve(bundle);
            boolean loaded = false;
            if (Files.exists(diskFile)) {
                try {
                    yaml.load(diskFile.toFile());
                    loaded = true;
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.WARNING, "Unable to read language file " + diskFile, exception);
                }
            }
            if (!loaded) {
                String resourcePath = "lang/" + language + "/" + bundle;
                try (InputStream inputStream = plugin.getResource(resourcePath)) {
                    if (inputStream != null) {
                        yaml.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        loaded = true;
                    }
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.WARNING, "Unable to read bundled language file " + resourcePath, exception);
                }
            }
            if (loaded) {
                String bundlePrefix = bundle.endsWith(".yml") ? bundle.substring(0, bundle.length() - 4) : bundle;
                flatten(bundlePrefix, yaml, messages);
            }
        }
        return Collections.unmodifiableMap(messages);
    }

    private void flatten(String prefix, org.bukkit.configuration.ConfigurationSection section, Map<String, String> output) {
        for (String key : section.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (section.isConfigurationSection(key)) {
                flatten(path, section.getConfigurationSection(key), output);
            } else {
                Object value = section.get(key);
                if (value != null) {
                    output.put(path, String.valueOf(value));
                }
            }
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
