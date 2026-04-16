package dev.crossroadsmc.crossroads.command;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.api.event.HomeTeleportEvent;
import dev.crossroadsmc.crossroads.model.KitDefinition;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import dev.crossroadsmc.crossroads.util.Chat;
import dev.crossroadsmc.crossroads.util.LocationFormatter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CrossroadsCommandRouter implements CommandExecutor, TabCompleter {
    private final CrossroadsPlugin plugin;

    public CrossroadsCommandRouter(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "home" -> handleHome(sender, args);
            case "sethome" -> handleSetHome(sender, args);
            case "delhome" -> handleDelHome(sender, args);
            case "homes" -> handleHomes(sender);
            case "warp" -> handleWarp(sender, args);
            case "setwarp" -> handleSetWarp(sender, args);
            case "delwarp" -> handleDelWarp(sender, args);
            case "warps" -> handleWarps(sender);
            case "spawn" -> handleSpawn(sender);
            case "setspawn" -> handleSetSpawn(sender);
            case "back" -> handleBack(sender);
            case "msg" -> handleMessage(sender, args);
            case "reply" -> handleReply(sender, args);
            case "ignore" -> handleIgnore(sender, args);
            case "fly" -> handleFly(sender);
            case "vanish" -> handleVanish(sender);
            case "staffmode" -> handleStaffMode(sender);
            case "kit" -> handleKit(sender, args);
            case "rules" -> handleRules(sender);
            case "crossroads" -> handleCrossroads(sender, args);
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        return switch (name) {
            case "home", "delhome" -> args.length == 1 ? filterPrefix(homeNames(sender), args[0]) : Collections.emptyList();
            case "warp", "delwarp" -> args.length == 1 ? filterPrefix(new ArrayList<>(plugin.getWarpService().getWarps().keySet()), args[0]) : Collections.emptyList();
            case "setwarp", "sethome" -> Collections.emptyList();
            case "msg", "ignore" -> args.length == 1 ? filterPrefix(onlineNames(sender), args[0]) : Collections.emptyList();
            case "reply" -> Collections.emptyList();
            case "kit" -> args.length == 1 ? filterPrefix(plugin.getKitService().getKits().stream().map(KitDefinition::getKey).toList(), args[0]) : Collections.emptyList();
            case "crossroads" -> {
                if (args.length == 1) {
                    yield filterPrefix(List.of("reload", "backup", "about"), args[0]);
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("backup")) {
                    yield filterPrefix(List.of("create"), args[1]);
                }
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    private boolean handleHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.home")) {
            return true;
        }

        String homeName = args.length > 0 ? args[0].toLowerCase(Locale.ROOT)
            : plugin.getConfig().getString("players.homes.default-name", "home").toLowerCase(Locale.ROOT);

        SavedLocation savedLocation = plugin.getPlayerDataService().get(player).getHome(homeName);
        if (savedLocation == null) {
            Chat.send(plugin, player, "&cYou do not have a home named &e" + homeName + "&c.");
            return true;
        }

        Location destination = savedLocation.toLocation();
        if (destination == null) {
            Chat.send(plugin, player, "&cThat home points to a world that is not loaded.");
            return true;
        }

        HomeTeleportEvent event = new HomeTeleportEvent(player, homeName, destination);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getDestination() == null) {
            Chat.send(plugin, player, "&cYour home teleport was cancelled.");
            return true;
        }

        teleport(player, event.getDestination(), "&aWelcome back to home &e" + homeName + "&a.");
        return true;
    }

    private boolean handleSetHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.home")) {
            return true;
        }

        String homeName = args.length > 0 ? args[0].toLowerCase(Locale.ROOT)
            : plugin.getConfig().getString("players.homes.default-name", "home").toLowerCase(Locale.ROOT);

        PlayerData data = plugin.getPlayerDataService().get(player);
        int homeLimit = plugin.getConfig().getInt("players.homes.default-limit", 3);
        if (!data.hasHome(homeName) && !player.hasPermission("crossroads.home.unlimited") && data.getHomeCount() >= homeLimit) {
            Chat.send(plugin, player, "&cYou have reached your home limit of &e" + homeLimit + "&c.");
            return true;
        }

        data.setHome(homeName, SavedLocation.fromLocation(player.getLocation()));
        plugin.getPlayerDataService().save(player);
        Chat.send(plugin, player, "&aSaved home &e" + homeName + "&a at &f" + LocationFormatter.human(player.getLocation()));
        return true;
    }

    private boolean handleDelHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.home")) {
            return true;
        }

        String homeName = args.length > 0 ? args[0].toLowerCase(Locale.ROOT)
            : plugin.getConfig().getString("players.homes.default-name", "home").toLowerCase(Locale.ROOT);

        SavedLocation removed = plugin.getPlayerDataService().get(player).removeHome(homeName);
        if (removed == null) {
            Chat.send(plugin, player, "&cNo home named &e" + homeName + "&c exists.");
            return true;
        }

        plugin.getPlayerDataService().save(player);
        Chat.send(plugin, player, "&aDeleted home &e" + homeName + "&a.");
        return true;
    }

    private boolean handleHomes(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.home")) {
            return true;
        }

        Map<String, SavedLocation> homes = plugin.getPlayerDataService().get(player).getHomes();
        if (homes.isEmpty()) {
            Chat.send(plugin, player, "&7You have not set any homes yet.");
            return true;
        }

        Chat.send(plugin, player, "&eYour homes&7: &f" + String.join(", ", homes.keySet()));
        return true;
    }

    private boolean handleWarp(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.warp")) {
            return true;
        }

        if (args.length == 0) {
            Chat.send(plugin, player, "&7Available warps: &f" + String.join(", ", plugin.getWarpService().getWarps().keySet()));
            return true;
        }

        SavedLocation warp = plugin.getWarpService().getWarp(args[0]);
        if (warp == null) {
            Chat.send(plugin, player, "&cUnknown warp &e" + args[0] + "&c.");
            return true;
        }

        Location destination = warp.toLocation();
        if (destination == null) {
            Chat.send(plugin, player, "&cThat warp points to a world that is not loaded.");
            return true;
        }

        teleport(player, destination, "&aWarped to &e" + args[0].toLowerCase(Locale.ROOT) + "&a.");
        return true;
    }

    private boolean handleSetWarp(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.admin")) {
            return true;
        }

        if (args.length == 0) {
            Chat.send(plugin, player, "&cUsage: /setwarp <name>");
            return true;
        }

        String warpName = args[0].toLowerCase(Locale.ROOT);
        plugin.getWarpService().setWarp(warpName, SavedLocation.fromLocation(player.getLocation()));
        Chat.send(plugin, player, "&aSaved warp &e" + warpName + "&a at &f" + LocationFormatter.human(player.getLocation()));
        return true;
    }

    private boolean handleDelWarp(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.admin")) {
            return true;
        }

        if (args.length == 0) {
            Chat.send(plugin, player, "&cUsage: /delwarp <name>");
            return true;
        }

        SavedLocation removed = plugin.getWarpService().removeWarp(args[0]);
        if (removed == null) {
            Chat.send(plugin, player, "&cUnknown warp &e" + args[0] + "&c.");
            return true;
        }

        Chat.send(plugin, player, "&aDeleted warp &e" + args[0].toLowerCase(Locale.ROOT) + "&a.");
        return true;
    }

    private boolean handleWarps(CommandSender sender) {
        if (!requirePermission(sender, "crossroads.warp")) {
            return true;
        }

        List<String> warps = plugin.getWarpService().getWarps().keySet().stream().sorted().toList();
        if (warps.isEmpty()) {
            Chat.send(plugin, sender, "&7No warps have been created yet.");
            return true;
        }

        Chat.send(plugin, sender, "&eWarps&7: &f" + String.join(", ", warps));
        return true;
    }

    private boolean handleSpawn(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.spawn")) {
            return true;
        }

        SavedLocation spawn = plugin.getSpawnService().getSpawn();
        if (spawn == null) {
            Chat.send(plugin, player, "&cSpawn has not been configured yet.");
            return true;
        }

        Location location = spawn.toLocation();
        if (location == null) {
            Chat.send(plugin, player, "&cSpawn points to a world that is not loaded.");
            return true;
        }

        teleport(player, location, "&aTeleported to spawn.");
        return true;
    }

    private boolean handleSetSpawn(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.admin")) {
            return true;
        }

        plugin.getSpawnService().setSpawn(SavedLocation.fromLocation(player.getLocation()));
        Chat.send(plugin, player, "&aSpawn updated to &f" + LocationFormatter.human(player.getLocation()));
        return true;
    }

    private boolean handleBack(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.back")) {
            return true;
        }

        SavedLocation backLocation = plugin.getBackService().get(player);
        if (backLocation == null) {
            Chat.send(plugin, player, "&cNo previous location is available yet.");
            return true;
        }

        Location location = backLocation.toLocation();
        if (location == null) {
            Chat.send(plugin, player, "&cYour back location points to a world that is not loaded.");
            return true;
        }

        teleport(player, location, "&aReturned to your previous location.");
        return true;
    }

    private boolean handleMessage(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.msg")) {
            return true;
        }

        if (args.length < 2) {
            Chat.send(plugin, player, "&cUsage: /msg <player> <message>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            Chat.send(plugin, player, "&cThat player is not online.");
            return true;
        }
        if (target.equals(player)) {
            Chat.send(plugin, player, "&cSending yourself a DM is a little too introspective even for Crossroads.");
            return true;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        plugin.getMessagingService().sendMessage(player, target, message);
        return true;
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.msg")) {
            return true;
        }

        if (args.length == 0) {
            Chat.send(plugin, player, "&cUsage: /reply <message>");
            return true;
        }

        Player target = plugin.getMessagingService().getReplyTarget(player);
        if (target == null) {
            Chat.send(plugin, player, "&cNobody has messaged you recently.");
            return true;
        }

        String message = String.join(" ", args);
        plugin.getMessagingService().sendMessage(player, target, message);
        return true;
    }

    private boolean handleIgnore(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.msg")) {
            return true;
        }

        if (args.length == 0) {
            Chat.send(plugin, player, "&cUsage: /ignore <player>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            Chat.send(plugin, player, "&cThat player is not online.");
            return true;
        }
        if (target.equals(player)) {
            Chat.send(plugin, player, "&cIgnoring yourself would be impressive, but not useful.");
            return true;
        }

        boolean ignored = plugin.getMessagingService().toggleIgnore(player, target);
        Chat.send(plugin, player, ignored
            ? "&aYou are now ignoring &e" + target.getName() + "&a."
            : "&aYou are no longer ignoring &e" + target.getName() + "&a.");
        return true;
    }

    private boolean handleFly(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.staff")) {
            return true;
        }

        boolean enabled = plugin.getStaffService().toggleFly(player);
        Chat.send(plugin, player, enabled ? "&aFlight enabled." : "&eFlight disabled.");
        return true;
    }

    private boolean handleVanish(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.staff")) {
            return true;
        }

        boolean enabled = plugin.getStaffService().toggleVanish(player);
        Chat.send(plugin, player, enabled ? "&aYou vanished from regular players." : "&eYou are visible again.");
        return true;
    }

    private boolean handleStaffMode(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.staff")) {
            return true;
        }

        boolean enabled = plugin.getStaffService().toggleStaffMode(player);
        Chat.send(plugin, player, enabled
            ? "&aStaff mode enabled. Your inventory was stored and your moderation toolkit is ready."
            : "&eStaff mode disabled. Your original inventory has been restored.");
        return true;
    }

    private boolean handleKit(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.kit")) {
            return true;
        }

        if (args.length == 0) {
            List<String> available = plugin.getKitService().getKits().stream()
                .filter(kit -> kit.getPermission() == null || kit.getPermission().isBlank() || player.hasPermission(kit.getPermission()))
                .map(KitDefinition::getKey)
                .sorted()
                .toList();

            if (available.isEmpty()) {
                Chat.send(plugin, player, "&7No kits are available for you right now.");
                return true;
            }

            Chat.send(plugin, player, "&eAvailable kits&7: &f" + String.join(", ", available));
            return true;
        }

        KitDefinition kit = plugin.getKitService().getKit(args[0]);
        if (kit == null) {
            Chat.send(plugin, player, "&cUnknown kit &e" + args[0] + "&c.");
            return true;
        }
        if (kit.getPermission() != null && !kit.getPermission().isBlank() && !player.hasPermission(kit.getPermission())) {
            Chat.send(plugin, player, "&cYou do not have access to that kit.");
            return true;
        }

        PlayerData data = plugin.getPlayerDataService().get(player);
        long now = System.currentTimeMillis();
        long nextUse = data.getKitCooldown(kit.getKey());
        if (nextUse > now) {
            long remaining = Math.max(1L, (nextUse - now) / 1000L);
            Chat.send(plugin, player, "&cThat kit is on cooldown for another &e" + remaining + "&cs.");
            return true;
        }

        for (ItemStack item : kit.getItems()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            leftovers.values().forEach(leftover -> Objects.requireNonNull(player.getWorld()).dropItemNaturally(player.getLocation(), leftover));
        }

        if (kit.getCooldownSeconds() > 0L) {
            data.setKitCooldown(kit.getKey(), now + (kit.getCooldownSeconds() * 1000L));
            plugin.getPlayerDataService().save(player);
        }

        Chat.send(plugin, player, "&aClaimed kit &e" + kit.getDisplayName() + "&a.");
        return true;
    }

    private boolean handleRules(CommandSender sender) {
        if (!requirePermission(sender, "crossroads.rules")) {
            return true;
        }

        List<String> rules = plugin.getConfig().getStringList("rules");
        if (rules.isEmpty()) {
            Chat.send(plugin, sender, "&7No rules have been configured yet.");
            return true;
        }

        Chat.send(plugin, sender, "&eServer Rules");
        for (String line : rules) {
            Chat.sendRaw(sender, Chat.color("&8- &7" + line));
        }
        return true;
    }

    private boolean handleCrossroads(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("about")) {
            Chat.send(plugin, sender, "&6Crossroads &7v" + plugin.getDescription().getVersion()
                + " &8| &fHomes, warps, spawn, kits, messaging, staff tools and backups.");
            return true;
        }

        if (!requirePermission(sender, "crossroads.admin")) {
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadCrossroads();
            Chat.send(plugin, sender, "&aConfiguration, warps, spawn, and kits have been reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("backup")) {
            if (args.length < 2 || !args[1].equalsIgnoreCase("create")) {
                Chat.send(plugin, sender, "&cUsage: /crossroads backup create");
                return true;
            }

            try {
                File backup = plugin.getBackupService().createBackup("manual");
                Chat.send(plugin, sender, "&aBackup created: &f" + backup.getName());
            } catch (Exception exception) {
                Chat.send(plugin, sender, "&cBackup failed. Check console for details.");
                plugin.getLogger().warning("Manual backup failed: " + exception.getMessage());
            }
            return true;
        }

        Chat.send(plugin, sender, "&cUnknown subcommand. Try &e/crossroads about&c, &e/crossroads reload&c or &e/crossroads backup create&c.");
        return true;
    }

    private void teleport(Player player, Location destination, String successMessage) {
        plugin.getBackService().record(player, player.getLocation());
        player.teleport(destination);
        Chat.send(plugin, player, successMessage);
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }

        Chat.send(plugin, sender, "&cThis command can only be used in-game.");
        return null;
    }

    private boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("crossroads.admin")) {
            return true;
        }

        Chat.send(plugin, sender, "&cYou do not have permission for that.");
        return false;
    }

    private List<String> homeNames(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        return plugin.getPlayerDataService().get(player).getHomes().keySet().stream().sorted().toList();
    }

    private List<String> onlineNames(CommandSender sender) {
        return plugin.getServer().getOnlinePlayers().stream()
            .filter(player -> !(sender instanceof Player senderPlayer) || !player.getUniqueId().equals(senderPlayer.getUniqueId()))
            .map(Player::getName)
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    private List<String> filterPrefix(List<String> values, String input) {
        String lowered = input.toLowerCase(Locale.ROOT);
        return values.stream()
            .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowered))
            .collect(Collectors.toList());
    }
}
