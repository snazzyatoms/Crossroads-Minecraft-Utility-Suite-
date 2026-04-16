package dev.crossroadsmc.crossroads.command;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.api.event.HomeTeleportEvent;
import dev.crossroadsmc.crossroads.model.KitDefinition;
import dev.crossroadsmc.crossroads.model.ModerationLogEntry;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import dev.crossroadsmc.crossroads.util.Chat;
import dev.crossroadsmc.crossroads.util.LocationFormatter;
import dev.crossroadsmc.crossroads.util.TimeFormatter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class CrossroadsCommandRouter implements CommandExecutor, TabCompleter {
    private final CrossroadsPlugin plugin;

    public CrossroadsCommandRouter(CrossroadsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        String feature = featureForCommand(name);
        if (feature != null && !requireFeature(sender, feature)) {
            return true;
        }

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
            case "socialspy" -> handleSocialSpy(sender);
            case "invsee" -> handleInvSee(sender, args);
            case "endersee" -> handleEnderSee(sender, args);
            case "freeze" -> handleFreeze(sender, args);
            case "unfreeze" -> handleUnfreeze(sender, args);
            case "mute" -> handleMute(sender, args);
            case "unmute" -> handleUnmute(sender, args);
            case "warn" -> handleWarn(sender, args);
            case "stafflog", "history" -> handleStaffLog(sender, args);
            case "seen" -> handleSeen(sender, args);
            case "kit" -> handleKit(sender, args);
            case "rules" -> handleRules(sender);
            case "crossroads" -> handleCrossroads(sender, args);
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        String feature = featureForCommand(name);
        if (feature != null && !plugin.isFeatureEnabled(feature)) {
            return Collections.emptyList();
        }

        return switch (name) {
            case "home", "delhome" -> args.length == 1 ? filterPrefix(homeNames(sender), args[0]) : Collections.emptyList();
            case "warp", "delwarp" -> args.length == 1 ? filterPrefix(new ArrayList<>(plugin.getWarpService().getWarps().keySet()), args[0]) : Collections.emptyList();
            case "msg", "ignore", "invsee", "endersee", "freeze", "mute" ->
                args.length == 1 ? filterPrefix(onlineNames(sender), args[0]) : Collections.emptyList();
            case "unfreeze", "unmute", "warn", "stafflog", "history", "seen" ->
                args.length == 1 ? filterPrefix(knownNames(), args[0]) : Collections.emptyList();
            case "kit" -> args.length == 1 ? filterPrefix(plugin.getKitService().getKits().stream().map(KitDefinition::getKey).toList(), args[0]) : Collections.emptyList();
            case "crossroads" -> {
                if (args.length == 1) {
                    yield filterPrefix(List.of("reload", "backup", "about", "modules"), args[0]);
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
            Chat.send(plugin, player, "<error>You do not have a home named <warn>" + homeName + "<error>.");
            return true;
        }

        Location destination = savedLocation.toLocation();
        if (destination == null) {
            Chat.send(plugin, player, "<error>That home points to a world that is not loaded.");
            return true;
        }

        HomeTeleportEvent event = new HomeTeleportEvent(player, homeName, destination);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getDestination() == null) {
            Chat.send(plugin, player, "<error>Your home teleport was cancelled.");
            return true;
        }

        teleport(player, event.getDestination(), "<success>Welcome back to home <warn>" + homeName + "<success>.");
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
            Chat.send(plugin, player, "<error>You have reached your home limit of <warn>" + homeLimit + "<error>.");
            return true;
        }

        data.setHome(homeName, SavedLocation.fromLocation(player.getLocation()));
        plugin.getPlayerDataService().save(player);
        Chat.send(plugin, player, "<success>Saved home <warn>" + homeName + "<success> at <text>" + LocationFormatter.human(player.getLocation()));
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
            Chat.send(plugin, player, "<error>No home named <warn>" + homeName + "<error> exists.");
            return true;
        }

        plugin.getPlayerDataService().save(player);
        Chat.send(plugin, player, "<success>Deleted home <warn>" + homeName + "<success>.");
        return true;
    }

    private boolean handleHomes(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.home")) {
            return true;
        }

        Map<String, SavedLocation> homes = plugin.getPlayerDataService().get(player).getHomes();
        if (homes.isEmpty()) {
            Chat.send(plugin, player, "<subtle>You have not set any homes yet.");
            return true;
        }

        Chat.send(plugin, player, "<warn>Your homes<subtle>: <text>" + String.join(", ", homes.keySet()));
        return true;
    }

    private boolean handleWarp(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.warp")) {
            return true;
        }

        if (args.length == 0) {
            Chat.send(plugin, player, "<info>Available warps<subtle>: <text>" + String.join(", ", plugin.getWarpService().getWarps().keySet()));
            return true;
        }

        SavedLocation warp = plugin.getWarpService().getWarp(args[0]);
        if (warp == null) {
            Chat.send(plugin, player, "<error>Unknown warp <warn>" + args[0] + "<error>.");
            return true;
        }

        Location destination = warp.toLocation();
        if (destination == null) {
            Chat.send(plugin, player, "<error>That warp points to a world that is not loaded.");
            return true;
        }

        teleport(player, destination, "<success>Warped to <warn>" + args[0].toLowerCase(Locale.ROOT) + "<success>.");
        return true;
    }

    private boolean handleSetWarp(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.admin")) {
            return true;
        }

        if (args.length == 0) {
            Chat.send(plugin, player, "<error>Usage: <warn>/setwarp <name>");
            return true;
        }

        String warpName = args[0].toLowerCase(Locale.ROOT);
        plugin.getWarpService().setWarp(warpName, SavedLocation.fromLocation(player.getLocation()));
        Chat.send(plugin, player, "<success>Saved warp <warn>" + warpName + "<success> at <text>" + LocationFormatter.human(player.getLocation()));
        return true;
    }

    private boolean handleDelWarp(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.admin")) {
            return true;
        }

        if (args.length == 0) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/delwarp <name>");
            return true;
        }

        SavedLocation removed = plugin.getWarpService().removeWarp(args[0]);
        if (removed == null) {
            Chat.send(plugin, sender, "<error>Unknown warp <warn>" + args[0] + "<error>.");
            return true;
        }

        Chat.send(plugin, sender, "<success>Deleted warp <warn>" + args[0].toLowerCase(Locale.ROOT) + "<success>.");
        return true;
    }

    private boolean handleWarps(CommandSender sender) {
        if (!requirePermission(sender, "crossroads.warp")) {
            return true;
        }

        List<String> warps = plugin.getWarpService().getWarps().keySet().stream().sorted().toList();
        if (warps.isEmpty()) {
            Chat.send(plugin, sender, "<subtle>No warps have been created yet.");
            return true;
        }

        Chat.send(plugin, sender, "<warn>Warps<subtle>: <text>" + String.join(", ", warps));
        return true;
    }

    private boolean handleSpawn(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.spawn")) {
            return true;
        }

        SavedLocation spawn = plugin.getSpawnService().getSpawn();
        if (spawn == null) {
            Chat.send(plugin, player, "<error>Spawn has not been configured yet.");
            return true;
        }

        Location location = spawn.toLocation();
        if (location == null) {
            Chat.send(plugin, player, "<error>Spawn points to a world that is not loaded.");
            return true;
        }

        teleport(player, location, "<success>Teleported to spawn.");
        return true;
    }

    private boolean handleSetSpawn(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.admin")) {
            return true;
        }

        plugin.getSpawnService().setSpawn(SavedLocation.fromLocation(player.getLocation()));
        Chat.send(plugin, player, "<success>Spawn updated to <text>" + LocationFormatter.human(player.getLocation()));
        return true;
    }

    private boolean handleBack(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.back")) {
            return true;
        }

        SavedLocation backLocation = plugin.getBackService().get(player);
        if (backLocation == null) {
            Chat.send(plugin, player, "<error>No previous location is available yet.");
            return true;
        }

        Location location = backLocation.toLocation();
        if (location == null) {
            Chat.send(plugin, player, "<error>Your back location points to a world that is not loaded.");
            return true;
        }

        teleport(player, location, "<success>Returned to your previous location.");
        return true;
    }

    private boolean handleMessage(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.msg")) {
            return true;
        }

        if (args.length < 2) {
            Chat.send(plugin, player, "<error>Usage: <warn>/msg <player> <message>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            Chat.send(plugin, player, "<error>That player is not online.");
            return true;
        }
        if (target.equals(player)) {
            Chat.send(plugin, player, "<error>Sending yourself a DM is a little too introspective even for Crossroads.");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getMessagingService().sendMessage(player, target, message);
        return true;
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.msg")) {
            return true;
        }

        if (args.length == 0) {
            Chat.send(plugin, player, "<error>Usage: <warn>/reply <message>");
            return true;
        }

        Player target = plugin.getMessagingService().getReplyTarget(player);
        if (target == null) {
            Chat.send(plugin, player, "<error>Nobody has messaged you recently.");
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
            Chat.send(plugin, player, "<error>Usage: <warn>/ignore <player>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            Chat.send(plugin, player, "<error>That player is not online.");
            return true;
        }
        if (target.equals(player)) {
            Chat.send(plugin, player, "<error>Ignoring yourself would be impressive, but not useful.");
            return true;
        }

        boolean ignored = plugin.getMessagingService().toggleIgnore(player, target);
        Chat.send(plugin, player, ignored
            ? "<success>You are now ignoring <warn>" + target.getName() + "<success>."
            : "<success>You are no longer ignoring <warn>" + target.getName() + "<success>.");
        return true;
    }

    private boolean handleFly(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.staff")) {
            return true;
        }

        boolean enabled = plugin.getStaffService().toggleFly(player);
        Chat.send(plugin, player, enabled ? "<success>Flight enabled." : "<warn>Flight disabled.");
        return true;
    }

    private boolean handleVanish(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.staff")) {
            return true;
        }

        boolean enabled = plugin.getStaffService().toggleVanish(player);
        Chat.send(plugin, player, enabled ? "<success>You vanished from regular players." : "<warn>You are visible again.");
        return true;
    }

    private boolean handleStaffMode(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.staff")) {
            return true;
        }

        boolean enabled = plugin.getStaffService().toggleStaffMode(player);
        Chat.send(plugin, player, enabled
            ? "<success>Staff mode enabled. Your inventory was stored and your moderation toolkit is ready."
            : "<warn>Staff mode disabled. Your original inventory has been restored.");
        return true;
    }

    private boolean handleSocialSpy(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.staff")) {
            return true;
        }

        boolean enabled = plugin.getStaffService().toggleSocialSpy(player);
        Chat.send(plugin, player, enabled ? "<success>SocialSpy enabled." : "<warn>SocialSpy disabled.");
        return true;
    }

    private boolean handleInvSee(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.inspect")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, player, "<error>Usage: <warn>/invsee <player>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            Chat.send(plugin, player, "<error>That player is not online.");
            return true;
        }

        player.openInventory(target.getInventory());
        Chat.send(plugin, player, "<success>Inspecting <warn>" + target.getName() + "<success>'s inventory.");
        return true;
    }

    private boolean handleEnderSee(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.inspect")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, player, "<error>Usage: <warn>/endersee <player>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            Chat.send(plugin, player, "<error>That player is not online.");
            return true;
        }

        Inventory inventory = Bukkit.createInventory(target, target.getEnderChest().getSize(), target.getName() + "'s Ender Chest");
        inventory.setContents(target.getEnderChest().getContents());
        player.openInventory(inventory);
        Chat.send(plugin, player, "<success>Inspecting <warn>" + target.getName() + "<success>'s ender chest.");
        return true;
    }

    private boolean handleFreeze(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/freeze <player> [reason]");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            Chat.send(plugin, sender, "<error>That player is not online.");
            return true;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Under staff review";
        plugin.getModerationService().freeze(sender, target, reason);
        Chat.send(plugin, sender, "<success>Frozen <warn>" + target.getName() + "<success>.");
        Chat.send(plugin, target, "<error>You have been frozen by staff. <subtle>" + reason);
        return true;
    }

    private boolean handleUnfreeze(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/unfreeze <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.getModerationService().unfreeze(sender, target);
        if (target.isOnline() && target.getPlayer() != null) {
            Chat.send(plugin, target.getPlayer(), "<success>You are no longer frozen.");
        }
        Chat.send(plugin, sender, "<success>Unfroze <warn>" + target.getName() + "<success>.");
        return true;
    }

    private boolean handleMute(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length < 2) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/mute <player> <minutes> [reason]");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            Chat.send(plugin, sender, "<error>That player is not online.");
            return true;
        }

        long minutes;
        try {
            minutes = Long.parseLong(args[1]);
        } catch (NumberFormatException exception) {
            Chat.send(plugin, sender, "<error>Minutes must be a number.");
            return true;
        }

        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Muted by staff";
        plugin.getModerationService().mute(sender, target, minutes * 60L, reason);
        Chat.send(plugin, sender, "<success>Muted <warn>" + target.getName() + "<success> for <warn>" + minutes + "<success>m.");
        Chat.send(plugin, target, "<error>You have been muted for <warn>" + minutes + "<error>m. Reason: <text>" + reason);
        return true;
    }

    private boolean handleUnmute(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/unmute <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.getModerationService().unmute(sender, target);
        if (target.isOnline() && target.getPlayer() != null) {
            Chat.send(plugin, target.getPlayer(), "<success>You have been unmuted.");
        }
        Chat.send(plugin, sender, "<success>Unmuted <warn>" + target.getName() + "<success>.");
        return true;
    }

    private boolean handleWarn(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length < 2) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/warn <player> <reason>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getModerationService().warn(sender, target, reason);
        if (target.isOnline() && target.getPlayer() != null) {
            Chat.send(plugin, target.getPlayer(), "<error>Warning issued: <text>" + reason);
        }
        Chat.send(plugin, sender, "<success>Warning logged for <warn>" + target.getName() + "<success>.");
        return true;
    }

    private boolean handleStaffLog(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/stafflog <player> [limit]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                Chat.send(plugin, sender, "<error>Limit must be a number.");
                return true;
            }
        }

        List<ModerationLogEntry> entries = plugin.getModerationService().getLogs(target, Math.max(1, Math.min(limit, 20)));
        if (entries.isEmpty()) {
            Chat.send(plugin, sender, "<subtle>No moderation history found for <text>" + target.getName() + "<subtle>.");
            return true;
        }

        Chat.send(plugin, sender, "<warn>Moderation log for <text>" + target.getName());
        for (ModerationLogEntry entry : entries) {
            Chat.sendRaw(plugin, sender, entry.toDisplayLine());
        }
        return true;
    }

    private boolean handleSeen(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.inspect")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/seen <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.isOnline()) {
            Chat.send(plugin, sender, "<warn>" + target.getName() + " <success>is online right now.");
            return true;
        }

        PlayerData data = plugin.getModerationService().getPlayerData(target.getUniqueId());
        if (data.getLastQuitAt() <= 0L) {
            Chat.send(plugin, sender, "<subtle>No recorded activity for <text>" + args[0] + "<subtle> yet.");
            return true;
        }

        long seconds = Math.max(1L, (System.currentTimeMillis() - data.getLastQuitAt()) / 1000L);
        Chat.send(plugin, sender, "<warn>" + data.getLastKnownName() + " <subtle>was last seen <text>" + TimeFormatter.duration(seconds) + " <subtle>ago.");
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
                Chat.send(plugin, player, "<subtle>No kits are available for you right now.");
                return true;
            }

            Chat.send(plugin, player, "<info>Available kits<subtle>: <text>" + String.join(", ", available));
            return true;
        }

        KitDefinition kit = plugin.getKitService().getKit(args[0]);
        if (kit == null) {
            Chat.send(plugin, player, "<error>Unknown kit <warn>" + args[0] + "<error>.");
            return true;
        }
        if (kit.getPermission() != null && !kit.getPermission().isBlank() && !player.hasPermission(kit.getPermission())) {
            Chat.send(plugin, player, "<error>You do not have access to that kit.");
            return true;
        }

        PlayerData data = plugin.getPlayerDataService().get(player);
        long now = System.currentTimeMillis();
        long nextUse = data.getKitCooldown(kit.getKey());
        if (nextUse > now) {
            long remaining = Math.max(1L, (nextUse - now) / 1000L);
            Chat.send(plugin, player, "<error>That kit is on cooldown for another <warn>" + TimeFormatter.duration(remaining) + "<error>.");
            return true;
        }

        for (ItemStack item : kit.getItems()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            leftovers.values().forEach(leftover -> Objects.requireNonNull(player.getWorld()).dropItemNaturally(player.getLocation(), leftover));
        }

        for (String rawCommand : kit.getCommands()) {
            String dispatch = rawCommand.replace("%player%", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), dispatch);
        }

        if (kit.getCooldownSeconds() > 0L) {
            data.setKitCooldown(kit.getKey(), now + (kit.getCooldownSeconds() * 1000L));
            plugin.getPlayerDataService().save(player);
        }

        Chat.send(plugin, player, "<success>Claimed kit <warn>" + kit.getDisplayName() + "<success>.");
        return true;
    }

    private boolean handleRules(CommandSender sender) {
        if (!requirePermission(sender, "crossroads.rules")) {
            return true;
        }

        List<String> rules = plugin.getConfig().getStringList("rules");
        if (rules.isEmpty()) {
            Chat.send(plugin, sender, "<subtle>No rules have been configured yet.");
            return true;
        }

        Chat.send(plugin, sender, "<warn>Server Rules");
        for (String line : rules) {
            Chat.sendRaw(plugin, sender, "<muted>- <subtle>" + line);
        }
        return true;
    }

    private boolean handleCrossroads(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("about")) {
            Chat.send(plugin, sender, "<accent>Crossroads <subtle>v<text>" + plugin.getDescription().getVersion()
                + " <muted>| <text>Storage: <warn>" + plugin.getStorageManager().getProvider().getType()
                + " <muted>| <text>Modules: <warn>" + plugin.getModuleManager().getModules().size());
            return true;
        }

        if (!requirePermission(sender, "crossroads.admin")) {
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadCrossroads();
            Chat.send(plugin, sender, "<success>Configuration, kits, warps, spawn, and welcome profiles have been reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("modules")) {
            if (!requireFeature(sender, "modules")) {
                return true;
            }
            String modules = plugin.getModuleManager().getModules().stream()
                .map(module -> module.getId())
                .sorted()
                .collect(Collectors.joining(", "));
            Chat.send(plugin, sender, modules.isBlank() ? "<subtle>No external modules are currently loaded." : "<warn>Modules<subtle>: <text>" + modules);
            return true;
        }

        if (args[0].equalsIgnoreCase("backup")) {
            if (!requireFeature(sender, "backups")) {
                return true;
            }
            if (args.length < 2 || !args[1].equalsIgnoreCase("create")) {
                Chat.send(plugin, sender, "<error>Usage: <warn>/crossroads backup create");
                return true;
            }

            try {
                File backup = plugin.getBackupService().createBackup("manual");
                Chat.send(plugin, sender, "<success>Backup created: <text>" + backup.getName());
            } catch (Exception exception) {
                Chat.send(plugin, sender, "<error>Backup failed. Check console for details.");
                plugin.getLogger().warning("Manual backup failed: " + exception.getMessage());
            }
            return true;
        }

        Chat.send(plugin, sender, "<error>Unknown subcommand. Try <warn>/crossroads about<error>, <warn>/crossroads reload<error>, <warn>/crossroads modules<error> or <warn>/crossroads backup create<error>.");
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

        Chat.send(plugin, sender, "<error>This command can only be used in-game.");
        return null;
    }

    private boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("crossroads.admin")) {
            return true;
        }

        Chat.send(plugin, sender, "<error>You do not have permission for that.");
        return false;
    }

    private boolean requireFeature(CommandSender sender, String featureKey) {
        if (plugin.isFeatureEnabled(featureKey)) {
            return true;
        }

        Chat.send(plugin, sender, "<error>That feature is disabled in the Crossroads config.");
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

    private List<String> knownNames() {
        return Bukkit.getOfflinePlayers().length == 0
            ? Collections.emptyList()
            : Arrays.stream(Bukkit.getOfflinePlayers())
                .map(OfflinePlayer::getName)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<String> filterPrefix(Collection<String> values, String input) {
        String lowered = input.toLowerCase(Locale.ROOT);
        return values.stream()
            .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowered))
            .collect(Collectors.toList());
    }

    private String featureForCommand(String commandName) {
        return switch (commandName) {
            case "home", "sethome", "delhome", "homes" -> "homes";
            case "warp", "setwarp", "delwarp", "warps" -> "warps";
            case "spawn", "setspawn" -> "spawn";
            case "back" -> "back";
            case "msg", "reply", "ignore" -> "messaging";
            case "fly", "vanish", "staffmode", "socialspy" -> "staff-tools";
            case "invsee", "endersee", "seen" -> "inspection";
            case "freeze", "unfreeze", "mute", "unmute", "warn", "stafflog", "history" -> "moderation";
            case "kit" -> "kits";
            case "rules" -> "rules";
            default -> null;
        };
    }
}
