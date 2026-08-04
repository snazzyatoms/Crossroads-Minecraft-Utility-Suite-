package dev.crossroadsmc.crossroads.command;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.api.event.HomeTeleportEvent;
import dev.crossroadsmc.crossroads.model.KitDefinition;
import dev.crossroadsmc.crossroads.model.MailMessage;
import dev.crossroadsmc.crossroads.model.ModerationLogEntry;
import dev.crossroadsmc.crossroads.model.PermissionGroup;
import dev.crossroadsmc.crossroads.model.PermissionNode;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import dev.crossroadsmc.crossroads.model.TeleportRequest;
import dev.crossroadsmc.crossroads.service.EconomyService;
import dev.crossroadsmc.crossroads.util.Chat;
import dev.crossroadsmc.crossroads.util.DurationParser;
import dev.crossroadsmc.crossroads.util.LocationFormatter;
import dev.crossroadsmc.crossroads.util.TimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
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
            case "setspawn" -> handleSetSpawn(sender, args);
            case "back" -> handleBack(sender);
            case "msg" -> handleMessage(sender, args);
            case "reply" -> handleReply(sender, args);
            case "ignore" -> handleIgnore(sender, args);
            case "mail" -> handleMail(sender, args);
            case "tpa" -> handleTeleportRequest(sender, args, false);
            case "tpahere" -> handleTeleportRequest(sender, args, true);
            case "tpaccept" -> handleTeleportAccept(sender);
            case "tpdeny" -> handleTeleportDeny(sender);
            case "tpacancel" -> handleTeleportCancel(sender);
            case "rtp" -> handleRandomTeleport(sender);
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
            case "kick" -> handleKick(sender, args);
            case "tempban" -> handleTempBan(sender, args);
            case "unban" -> handleUnban(sender, args);
            case "jail" -> handleJail(sender, args);
            case "unjail" -> handleUnjail(sender, args);
            case "setjail" -> handleSetJail(sender, args);
            case "shadowmute" -> handleShadowMute(sender, args);
            case "staffnote" -> handleStaffNote(sender, args);
            case "kit" -> handleKit(sender, args);
            case "rules" -> handleRules(sender);
            case "motd" -> handleMotd(sender);
            case "help" -> handleHelp(sender, args);
            case "info" -> handleInfo(sender, args);
            case "nick" -> handleNick(sender, args);
            case "language" -> handleLanguage(sender, args);
            case "balance" -> handleBalance(sender, args);
            case "pay" -> handlePay(sender, args);
            case "baltop" -> handleBalTop(sender, args);
            case "eco" -> handleEco(sender, args);
            case "crperms", "cperm" -> handleCrPerms(sender, args);
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
            case "home", "delhome" -> args.length == 1 ? filterPrefix(accessibleHomeNames(sender), args[0]) : Collections.emptyList();
            case "warp", "delwarp" -> args.length == 1 ? filterPrefix(accessibleWarpNames(sender), args[0]) : Collections.emptyList();
            case "msg", "ignore", "tpa", "tpahere", "kick", "freeze", "mute", "jail", "shadowmute", "pay" ->
                args.length == 1 ? filterPrefix(onlineNames(sender), args[0]) : Collections.emptyList();
            case "reply", "tpaccept", "tpdeny", "tpacancel", "fly", "vanish", "staffmode", "socialspy", "back", "rtp", "motd", "rules", "baltop" ->
                Collections.emptyList();
            case "unfreeze", "unmute", "warn", "stafflog", "history", "seen", "unban", "unjail", "staffnote", "balance" ->
                args.length == 1 ? filterPrefix(knownNames(), args[0]) : Collections.emptyList();
            case "setjail" -> Collections.emptyList();
            case "mail" -> {
                if (args.length == 1) {
                    yield filterPrefix(List.of("send", "read", "clear", "inbox"), args[0]);
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("send")) {
                    yield filterPrefix(knownNames(), args[1]);
                }
                yield Collections.emptyList();
            }
            case "kit" -> args.length == 1
                ? filterPrefix(plugin.getKitService().getKits().stream().map(KitDefinition::getKey).toList(), args[0])
                : Collections.emptyList();
            case "help" -> args.length == 1 ? filterPrefix(new ArrayList<>(plugin.getTextPageService().availableHelpPages()), args[0]) : Collections.emptyList();
            case "info" -> args.length == 1 ? filterPrefix(new ArrayList<>(plugin.getTextPageService().availableInfoPages()), args[0]) : Collections.emptyList();
            case "language" -> tabCompleteLanguage(args);
            case "eco" -> tabCompleteEco(args);
            case "crperms", "cperm" -> tabCompleteCrPerms(args);
            case "crossroads" -> tabCompleteCrossroads(args);
            default -> Collections.emptyList();
        };
    }

    private List<String> tabCompleteCrossroads(String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("reload", "backup", "about", "modules", "import", "language", "perms"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("backup")) {
            return filterPrefix(List.of("create"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("import")) {
            return filterPrefix(List.of("essentials"), args[1]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("language")) {
            return tabCompleteLanguage(Arrays.copyOfRange(args, 1, args.length));
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("perms")) {
            return tabCompleteCrPerms(Arrays.copyOfRange(args, 1, args.length));
        }
        return Collections.emptyList();
    }

    private List<String> tabCompleteLanguage(String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("list");
            options.add("sync");
            options.addAll(plugin.getLanguageService().getAvailableLanguages());
            return filterPrefix(options, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> tabCompleteEco(String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("give", "take", "set"), args[0]);
        }
        if (args.length == 2) {
            return filterPrefix(knownNames(), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> tabCompleteCrPerms(String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("user", "group", "list", "check", "import"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("user")) {
            return filterPrefix(knownNames(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("user")) {
            return filterPrefix(List.of("info", "permission", "parent", "group"), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("user") && args[2].equalsIgnoreCase("permission")) {
            return filterPrefix(List.of("set", "unset"), args[3]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("user") && args[2].equalsIgnoreCase("parent")) {
            return filterPrefix(List.of("add", "remove"), args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("user") && args[2].equalsIgnoreCase("parent")) {
            return filterPrefix(plugin.getPermissionService().getGroups().keySet(), args[4]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("user") && args[2].equalsIgnoreCase("group")) {
            return filterPrefix(plugin.getPermissionService().getGroups().keySet(), args[3]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("group")) {
            return filterPrefix(plugin.getPermissionService().getGroups().keySet(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("group")) {
            return filterPrefix(List.of("create", "delete", "info", "permission", "parent", "meta"), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("group") && args[2].equalsIgnoreCase("permission")) {
            return filterPrefix(List.of("set", "unset"), args[3]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("group") && args[2].equalsIgnoreCase("parent")) {
            return filterPrefix(List.of("add", "remove"), args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("group") && args[2].equalsIgnoreCase("parent")) {
            return filterPrefix(plugin.getPermissionService().getGroups().keySet(), args[4]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("group") && args[2].equalsIgnoreCase("meta")) {
            return filterPrefix(List.of("prefix", "suffix", "weight"), args[3]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            return filterPrefix(knownNames(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("import")) {
            return filterPrefix(List.of("luckperms"), args[1]);
        }
        return Collections.emptyList();
    }

    private boolean handleHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.home")) {
            return true;
        }
        if (!checkCooldown(player, "home")) {
            return true;
        }

        ScopedName scopedName = parseScopedName(player, args.length > 0 ? args[0] : defaultHomeName(), defaultHomeName());
        SavedLocation savedLocation = plugin.getPlayerDataService().get(player).getHome(scopedName.scope(), scopedName.name());
        if (savedLocation == null && !PlayerData.GLOBAL_SCOPE.equals(scopedName.scope())) {
            savedLocation = plugin.getPlayerDataService().get(player).getHome(PlayerData.GLOBAL_SCOPE, scopedName.name());
            if (savedLocation != null) {
                scopedName = new ScopedName(PlayerData.GLOBAL_SCOPE, scopedName.name());
            }
        }
        if (savedLocation == null) {
            msg(player, "travel.home.not-found", "%home%", scopedName.raw());
            return true;
        }

        Location destination = savedLocation.toLocation();
        if (destination == null) {
            msg(player, "travel.home.world-unloaded");
            return true;
        }

        HomeTeleportEvent event = new HomeTeleportEvent(player, scopedName.name(), destination);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getDestination() == null) {
            msg(player, "travel.home.cancelled");
            return true;
        }

        String failure = chargeTeleportCost(player, "home");
        if (failure != null) {
            sendFailure(player, failure);
            return true;
        }

        if (teleport(player, event.getDestination(), "travel.home.teleported", "%home%", scopedName.raw())) {
            applyCooldown(player, "home");
        }
        return true;
    }

    private boolean handleSetHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.home")) {
            return true;
        }

        ScopedName scopedName = parseScopedName(player, args.length > 0 ? args[0] : defaultHomeName(), defaultHomeName());
        PlayerData data = plugin.getPlayerDataService().get(player);
        int homeLimit = plugin.getConfig().getInt("players.homes.default-limit", 5);
        if (!data.hasHome(scopedName.scope(), scopedName.name())
            && !player.hasPermission("crossroads.home.unlimited")
            && data.getHomeCount(scopedName.scope()) >= homeLimit) {
            msg(player, "travel.home.limit", "%limit%", String.valueOf(homeLimit), "%scope%", scopedName.scope());
            return true;
        }

        data.setHome(scopedName.scope(), scopedName.name(), SavedLocation.fromLocation(player.getLocation()));
        plugin.getPlayerDataService().save(player);
        msg(player, "travel.home.saved", "%home%", scopedName.raw(), "%location%", LocationFormatter.human(player.getLocation()));
        return true;
    }

    private boolean handleDelHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.home")) {
            return true;
        }

        ScopedName scopedName = parseScopedName(player, args.length > 0 ? args[0] : defaultHomeName(), defaultHomeName());
        SavedLocation removed = plugin.getPlayerDataService().get(player).removeHome(scopedName.scope(), scopedName.name());
        if (removed == null) {
            msg(player, "travel.home.deleted-missing", "%home%", scopedName.raw());
            return true;
        }

        plugin.getPlayerDataService().save(player);
        msg(player, "travel.home.deleted", "%home%", scopedName.raw());
        return true;
    }

    private boolean handleHomes(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.home")) {
            return true;
        }

        List<String> names = accessibleHomeNames(player);
        if (names.isEmpty()) {
            msg(player, "travel.home.none");
            return true;
        }

        msg(player, "travel.home.list", "%homes%", String.join(", ", names));
        return true;
    }

    private boolean handleWarp(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.warp")) {
            return true;
        }

        String profile = currentProfile(player);
        if (args.length == 0) {
            if (plugin.getConfig().getBoolean("gui.warps.enabled", true)) {
                plugin.getMenuService().openWarpMenu(player);
                return true;
            }
            List<String> warps = plugin.getWarpService().getAvailableWarpNames(profile);
            if (warps.isEmpty()) {
                msg(player, "travel.warp.none-available");
                return true;
            }
            msg(player, "travel.warp.available", "%warps%", String.join(", ", warps));
            return true;
        }

        if (!checkCooldown(player, "warp")) {
            return true;
        }

        ScopedName scopedName = parseScopedName(player, args[0], args[0]);
        SavedLocation warp = plugin.getWarpService().getWarp(scopedName.scope(), scopedName.name());
        if (warp == null) {
            msg(player, "travel.warp.unknown", "%warp%", scopedName.raw());
            return true;
        }

        Location destination = warp.toLocation();
        if (destination == null) {
            msg(player, "travel.warp.world-unloaded");
            return true;
        }

        String failure = chargeTeleportCost(player, "warp");
        if (failure != null) {
            sendFailure(player, failure);
            return true;
        }

        if (teleport(player, destination, "travel.warp.teleported", "%warp%", scopedName.raw())) {
            applyCooldown(player, "warp");
        }
        return true;
    }

    private boolean handleSetWarp(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.admin")) {
            return true;
        }
        if (args.length == 0) {
            msg(player, "travel.warp.usage-set");
            return true;
        }

        ScopedName scopedName = parseScopedName(player, args[0], args[0]);
        plugin.getWarpService().setWarp(scopedName.scope(), scopedName.name(), SavedLocation.fromLocation(player.getLocation()));
        msg(player, "travel.warp.saved", "%warp%", scopedName.raw(), "%location%", LocationFormatter.human(player.getLocation()));
        return true;
    }

    private boolean handleDelWarp(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.admin")) {
            return true;
        }
        if (args.length == 0) {
            msg(player, "travel.warp.usage-del");
            return true;
        }

        ScopedName scopedName = parseScopedName(player, args[0], args[0]);
        SavedLocation removed = plugin.getWarpService().removeWarp(scopedName.scope(), scopedName.name());
        if (removed == null) {
            msg(player, "travel.warp.unknown", "%warp%", scopedName.raw());
            return true;
        }

        msg(player, "travel.warp.deleted", "%warp%", scopedName.raw());
        return true;
    }

    private boolean handleWarps(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.warp")) {
            return true;
        }

        List<String> warps = plugin.getWarpService().getAvailableWarpNames(currentProfile(player));
        if (warps.isEmpty()) {
            msg(player, "travel.warp.none-created");
            return true;
        }

        msg(player, "travel.warp.list", "%warps%", String.join(", ", warps));
        return true;
    }

    private boolean handleSpawn(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.spawn")) {
            return true;
        }
        if (!checkCooldown(player, "spawn")) {
            return true;
        }

        SavedLocation spawn = plugin.getSpawnService().getSpawn(currentProfile(player));
        if (spawn == null) {
            msg(player, "travel.spawn.missing");
            return true;
        }

        Location location = spawn.toLocation();
        if (location == null) {
            msg(player, "travel.spawn.world-unloaded");
            return true;
        }

        String failure = chargeTeleportCost(player, "spawn");
        if (failure != null) {
            sendFailure(player, failure);
            return true;
        }

        if (teleport(player, location, "travel.spawn.teleported")) {
            applyCooldown(player, "spawn");
        }
        return true;
    }

    private boolean handleSetSpawn(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.admin")) {
            return true;
        }

        String profile = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : currentProfile(player);
        plugin.getSpawnService().setSpawn(profile, SavedLocation.fromLocation(player.getLocation()));
        msg(player, "travel.spawn.updated", "%profile%", profile, "%location%", LocationFormatter.human(player.getLocation()));
        return true;
    }

    private boolean handleBack(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.back")) {
            return true;
        }
        if (!checkCooldown(player, "back")) {
            return true;
        }

        SavedLocation backLocation = plugin.getBackService().get(player);
        if (backLocation == null) {
            msg(player, "travel.back.missing");
            return true;
        }

        Location location = backLocation.toLocation();
        if (location == null) {
            msg(player, "travel.back.world-unloaded");
            return true;
        }

        String failure = chargeTeleportCost(player, "back");
        if (failure != null) {
            sendFailure(player, failure);
            return true;
        }

        if (teleport(player, location, "travel.back.teleported")) {
            applyCooldown(player, "back");
        }
        return true;
    }

    private boolean handleMessage(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.msg")) {
            return true;
        }
        if (args.length < 2) {
            msg(player, "social.msg.usage");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            msg(player, "common.player-not-online");
            return true;
        }
        if (target.equals(player)) {
            msg(player, "social.msg.self");
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
            msg(player, "social.reply.usage");
            return true;
        }

        Player target = plugin.getMessagingService().getReplyTarget(player);
        if (target == null) {
            msg(player, "social.reply.none");
            return true;
        }

        plugin.getMessagingService().sendMessage(player, target, String.join(" ", args));
        return true;
    }

    private boolean handleIgnore(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.msg")) {
            return true;
        }
        if (args.length == 0) {
            msg(player, "social.ignore.usage");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            msg(player, "common.player-not-online");
            return true;
        }
        if (target.equals(player)) {
            msg(player, "social.ignore.self");
            return true;
        }

        boolean ignored = plugin.getMessagingService().toggleIgnore(player, target);
        msg(player, ignored ? "social.ignore.enabled" : "social.ignore.disabled", "%player%", target.getName());
        return true;
    }

    private boolean handleMail(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.msg")) {
            return true;
        }

        PlayerData data = plugin.getPlayerDataService().get(player);
        if (args.length == 0 || args[0].equalsIgnoreCase("inbox")) {
            List<MailMessage> mail = data.getMailbox();
            if (mail.isEmpty()) {
                msg(player, "social.mail.empty");
                return true;
            }
            msg(player, "social.mail.header");
            for (int index = 0; index < mail.size(); index++) {
                MailMessage message = mail.get(index);
                String marker = message.isRead() ? "<muted>" : "<success>";
                msgRaw(player, "social.mail.line",
                    "%marker%", marker,
                    "%id%", String.valueOf(index + 1),
                    "%sender%", message.getSenderName(),
                    "%body%", message.getBody());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("send")) {
            if (args.length < 3) {
                msg(player, "social.mail.send-usage");
                return true;
            }
            OfflinePlayer target = requireKnownOfflinePlayer(player, args[1]);
            if (target == null) {
                return true;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                msg(player, "social.mail.self");
                return true;
            }
            PlayerData targetData = plugin.getPlayerDataService().get(target.getUniqueId());
            targetData.addMail(new MailMessage(System.currentTimeMillis(), player.getUniqueId(), player.getName(),
                String.join(" ", Arrays.copyOfRange(args, 2, args.length)), false));
            plugin.getPlayerDataService().save(target.getUniqueId());
            if (target.isOnline() && target.getPlayer() != null) {
                msg(target.getPlayer(), "social.mail.received", "%player%", player.getName());
            }
            msg(player, "social.mail.sent", "%player%", target.getName() == null ? args[1] : target.getName());
            return true;
        }

        if (args[0].equalsIgnoreCase("read")) {
            if (args.length < 2) {
                msg(player, "social.mail.read-usage");
                return true;
            }
            int index = parseIndex(args[1]);
            MailMessage message = data.getMail(index);
            if (message == null) {
                msg(player, "social.mail.missing");
                return true;
            }
            data.markMailRead(index);
            plugin.getPlayerDataService().save(player);
            msg(player, "social.mail.from", "%sender%", message.getSenderName());
            msgRaw(player, "social.mail.body", "%body%", message.getBody());
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (args.length < 2) {
                msg(player, "social.mail.clear-usage");
                return true;
            }
            if (args[1].equalsIgnoreCase("all")) {
                data.clearMail();
                plugin.getPlayerDataService().save(player);
                msg(player, "social.mail.cleared");
                return true;
            }
            int index = parseIndex(args[1]);
            MailMessage removed = data.removeMail(index);
            if (removed == null) {
                msg(player, "social.mail.missing");
                return true;
            }
            plugin.getPlayerDataService().save(player);
            msg(player, "social.mail.removed", "%sender%", removed.getSenderName());
            return true;
        }

        msg(player, "social.mail.usage");
        return true;
    }

    private boolean handleTeleportRequest(CommandSender sender, String[] args, boolean hereRequest) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.spawn")) {
            return true;
        }
        if (args.length == 0) {
            msg(player, hereRequest ? "travel.tpahere.usage" : "travel.tpa.usage");
            return true;
        }
        if (!checkCooldown(player, hereRequest ? "tpahere" : "tpa")) {
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            msg(player, "common.player-not-online");
            return true;
        }
        if (target.equals(player)) {
            msg(player, "travel.tpa.self");
            return true;
        }

        String failure = chargeTeleportCost(player, hereRequest ? "tpahere" : "tpa");
        if (failure != null) {
            sendFailure(player, failure);
            return true;
        }

        plugin.getTeleportRequestService().create(player, target, hereRequest);
        applyCooldown(player, hereRequest ? "tpahere" : "tpa");
        msg(player, "travel.tpa.sent", "%player%", target.getName());
        msg(target, hereRequest ? "travel.tpahere.incoming" : "travel.tpa.incoming", "%player%", player.getName());
        return true;
    }

    private boolean handleTeleportAccept(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        TeleportRequest request = plugin.getTeleportRequestService().accept(player);
        if (request == null) {
            msg(player, "travel.tpa.none");
            return true;
        }

        Player requester = plugin.getServer().getPlayer(request.getRequesterUuid());
        if (requester == null || !requester.isOnline()) {
            msg(player, "travel.tpa.requester-offline");
            return true;
        }

        if (request.isHereRequest()) {
            if (teleport(player, requester.getLocation(), "travel.tpa.accepted-target")) {
                msg(requester, "travel.tpa.accepted-requester", "%player%", player.getName());
            }
        } else {
            if (teleport(requester, player.getLocation(), "travel.tpa.accepted-requester", "%player%", player.getName())) {
                msg(player, "travel.tpa.accepted-target");
            }
        }
        return true;
    }

    private boolean handleTeleportDeny(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        TeleportRequest request = plugin.getTeleportRequestService().deny(player);
        if (request == null) {
            msg(player, "travel.tpa.none");
            return true;
        }
        Player requester = plugin.getServer().getPlayer(request.getRequesterUuid());
        if (requester != null && requester.isOnline()) {
            msg(requester, "travel.tpa.denied-requester", "%player%", player.getName());
        }
        msg(player, "travel.tpa.denied-target");
        return true;
    }

    private boolean handleTeleportCancel(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!plugin.getTeleportRequestService().cancel(player)) {
            msg(player, "travel.tpa.cancel-none");
            return true;
        }
        msg(player, "travel.tpa.cancelled");
        return true;
    }

    private boolean handleRandomTeleport(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.spawn")) {
            return true;
        }
        if (!checkCooldown(player, "rtp")) {
            return true;
        }
        if (plugin.getConfig().getStringList("rtp.disabled-worlds").contains(player.getWorld().getName())) {
            msg(player, "travel.rtp.disabled-world");
            return true;
        }

        String failure = chargeTeleportCost(player, "rtp");
        if (failure != null) {
            sendFailure(player, failure);
            return true;
        }

        World world = player.getWorld();
        int minRadius = Math.max(0, plugin.getConfig().getInt("rtp.min-radius", 64));
        int maxRadius = Math.max(minRadius + 1, plugin.getConfig().getInt("rtp.max-radius", 1500));
        int attempts = Math.max(1, plugin.getConfig().getInt("rtp.max-attempts", 12));

        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = randomSigned(minRadius, maxRadius);
            int z = randomSigned(minRadius, maxRadius);
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location target = new Location(world, x + 0.5D, Math.max(64, y), z + 0.5D);
            Block feet = target.getBlock();
            Block below = target.clone().add(0, -1, 0).getBlock();
            if (feet.getType().isSolid() || below.getType() == Material.LAVA || below.getType() == Material.WATER || below.getType() == Material.AIR) {
                continue;
            }
            if (plugin.getProtectionCompatibilityService().shouldAvoidProtectedRtp()
                && plugin.getProtectionCompatibilityService().isProtected(target)) {
                continue;
            }
            if (teleport(player, target, "travel.rtp.teleported")) {
                applyCooldown(player, "rtp");
                return true;
            }
        }

        msg(player, "travel.rtp.failed");
        return true;
    }

    private boolean handleFly(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.staff")) {
            return true;
        }
        boolean enabled = plugin.getStaffService().toggleFly(player);
        msg(player, enabled ? "staff.fly.enabled" : "staff.fly.disabled");
        return true;
    }

    private boolean handleVanish(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.staff")) {
            return true;
        }
        boolean enabled = plugin.getStaffService().toggleVanish(player);
        msg(player, enabled ? "staff.vanish.enabled" : "staff.vanish.disabled");
        return true;
    }

    private boolean handleStaffMode(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.staff")) {
            return true;
        }
        boolean enabled = plugin.getStaffService().toggleStaffMode(player);
        msg(player, enabled ? "staff.staffmode.enabled" : "staff.staffmode.disabled");
        return true;
    }

    private boolean handleSocialSpy(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.staff")) {
            return true;
        }
        boolean enabled = plugin.getStaffService().toggleSocialSpy(player);
        msg(player, enabled ? "staff.socialspy.enabled" : "staff.socialspy.disabled");
        return true;
    }

    private boolean handleInvSee(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.inspect")) {
            return true;
        }
        if (args.length == 0) {
            msg(player, "staff.invsee.usage");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            msg(player, "common.player-not-online");
            return true;
        }
        player.openInventory(target.getInventory());
        msg(player, "staff.invsee.inspecting", "%player%", target.getName());
        return true;
    }

    private boolean handleEnderSee(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.inspect")) {
            return true;
        }
        if (args.length == 0) {
            msg(player, "staff.endersee.usage");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            msg(player, "common.player-not-online");
            return true;
        }
        Inventory inventory = Bukkit.createInventory(target, target.getEnderChest().getSize(), target.getName() + "'s Ender Chest");
        inventory.setContents(target.getEnderChest().getContents());
        player.openInventory(inventory);
        msg(player, "staff.endersee.inspecting", "%player%", target.getName());
        return true;
    }

    private boolean handleFreeze(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            msg(sender, "moderation.freeze.usage");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            msg(sender, "common.player-not-online");
            return true;
        }
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Under staff review";
        plugin.getModerationService().freeze(sender, target, reason);
        msg(sender, "moderation.freeze.done", "%player%", target.getName());
        msg(target, "moderation.freeze.notify", "%reason%", reason);
        return true;
    }

    private boolean handleUnfreeze(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            msg(sender, "moderation.unfreeze.usage");
            return true;
        }
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[0]);
        if (target == null) {
            return true;
        }
        plugin.getModerationService().unfreeze(sender, target);
        if (target.isOnline() && target.getPlayer() != null) {
            msg(target.getPlayer(), "moderation.unfreeze.notify");
        }
        msg(sender, "moderation.unfreeze.done", "%player%", target.getName());
        return true;
    }

    private boolean handleMute(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length < 2) {
            msg(sender, "moderation.mute.usage");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            msg(sender, "common.player-not-online");
            return true;
        }
        long seconds = DurationParser.parseToSeconds(args[1]);
        if (seconds < 0L) {
            msg(sender, "moderation.mute.usage");
            return true;
        }
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Muted by staff";
        String durationText = formatDurationLabel(seconds);
        plugin.getModerationService().mute(sender, target, seconds, reason);
        msg(sender, "moderation.mute.done", "%player%", target.getName(), "%duration%", durationText);
        msg(target, "moderation.mute.notify", "%duration%", durationText, "%reason%", reason);
        return true;
    }

    private boolean handleUnmute(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            msg(sender, "moderation.unmute.usage");
            return true;
        }
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[0]);
        if (target == null) {
            return true;
        }
        plugin.getModerationService().unmute(sender, target);
        if (target.isOnline() && target.getPlayer() != null) {
            msg(target.getPlayer(), "moderation.unmute.notify");
        }
        msg(sender, "moderation.unmute.done", "%player%", target.getName());
        return true;
    }

    private boolean handleWarn(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length < 2) {
            msg(sender, "moderation.warn.usage");
            return true;
        }
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[0]);
        if (target == null) {
            return true;
        }
        String category = args.length > 2 ? args[1] : "general";
        String reason = args.length > 2
            ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
            : String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getModerationService().warn(sender, target, "[" + category + "] " + reason);
        if (target.isOnline() && target.getPlayer() != null) {
            msg(target.getPlayer(), "moderation.warn.notify", "%reason%", reason);
        }
        msg(sender, "moderation.warn.done", "%player%", target.getName());
        return true;
    }

    private boolean handleStaffLog(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            msg(sender, "moderation.stafflog.usage");
            return true;
        }
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[0]);
        if (target == null) {
            return true;
        }
        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                msg(sender, "moderation.stafflog.usage");
                return true;
            }
        }
        List<ModerationLogEntry> entries = plugin.getModerationService().getLogs(target, Math.max(1, Math.min(limit, 25)));
        if (entries.isEmpty()) {
            msg(sender, "moderation.stafflog.empty", "%player%", target.getName());
            return true;
        }
        msg(sender, "moderation.stafflog.header", "%player%", target.getName());
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
            msg(sender, "staff.seen.usage");
            return true;
        }

        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[0]);
        if (target == null) {
            return true;
        }
        if (target.isOnline()) {
            msg(sender, "staff.seen.online", "%player%", target.getName());
            return true;
        }

        PlayerData data = plugin.getModerationService().getPlayerData(target.getUniqueId());
        if (data.getLastQuitAt() <= 0L) {
            msg(sender, "staff.seen.never", "%player%", args[0]);
            return true;
        }
        long seconds = Math.max(1L, (System.currentTimeMillis() - data.getLastQuitAt()) / 1000L);
        msg(sender, "staff.seen.offline", "%player%", data.getLastKnownName(), "%time%", TimeFormatter.duration(seconds));
        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            msg(sender, "moderation.kick.usage");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            msg(sender, "common.player-not-online");
            return true;
        }
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Removed by staff";
        String kickMessage = plugin.getLanguageService().colorize(target, "moderation.kick.message", "%reason%", reason);
        plugin.getModerationService().kick(sender, target, kickMessage);
        msg(sender, "moderation.kick.done", "%player%", target.getName());
        return true;
    }

    private boolean handleTempBan(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length < 2) {
            msg(sender, "moderation.tempban.usage");
            return true;
        }
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[0]);
        if (target == null) {
            return true;
        }
        long seconds = DurationParser.parseToSeconds(args[1]);
        if (seconds < 0L) {
            msg(sender, "moderation.tempban.usage");
            return true;
        }
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Banned by staff";
        String durationText = formatDurationLabel(seconds);
        CommandSender styleSender = target.isOnline() && target.getPlayer() != null ? target.getPlayer() : sender;
        String banMessage = plugin.getLanguageService().colorize(styleSender, "moderation.tempban.message",
            "%duration%", durationText, "%reason%", reason);
        plugin.getModerationService().tempBan(sender, target, seconds, banMessage);
        msg(sender, "moderation.tempban.done", "%player%", target.getName(), "%duration%", durationText);
        return true;
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            msg(sender, "moderation.unban.usage");
            return true;
        }
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[0]);
        if (target == null) {
            return true;
        }
        plugin.getModerationService().unban(sender, target);
        msg(sender, "moderation.unban.done", "%player%", target.getName());
        return true;
    }

    private boolean handleJail(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length < 2) {
            msg(sender, "moderation.jail.usage");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            msg(sender, "common.player-not-online");
            return true;
        }
        long seconds = 1800L;
        int reasonStart = 2;
        if (args.length > 2) {
            long parsed = DurationParser.parseToSeconds(args[2]);
            if (parsed >= 0L) {
                seconds = parsed;
                reasonStart = 3;
            }
        }
        String reason = args.length > reasonStart ? String.join(" ", Arrays.copyOfRange(args, reasonStart, args.length)) : "Jailed by staff";
        try {
            plugin.getModerationService().jail(sender, target, args[1], seconds, reason);
        } catch (IllegalArgumentException exception) {
            msg(sender, "moderation.jail.usage");
            return true;
        }
        msg(sender, "moderation.jail.done", "%player%", target.getName(), "%jail%", args[1]);
        msg(target, "moderation.jail.notify", "%reason%", reason);
        return true;
    }

    private boolean handleUnjail(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            msg(sender, "moderation.unjail.usage");
            return true;
        }
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[0]);
        if (target == null) {
            return true;
        }
        plugin.getModerationService().unjail(sender, target);
        if (target.isOnline() && target.getPlayer() != null) {
            msg(target.getPlayer(), "moderation.unjail.notify");
        }
        msg(sender, "moderation.unjail.done", "%player%", target.getName());
        return true;
    }

    private boolean handleSetJail(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.admin")) {
            return true;
        }
        if (args.length == 0) {
            msg(player, "moderation.setjail.usage");
            return true;
        }
        plugin.getJailService().setJail(args[0], SavedLocation.fromLocation(player.getLocation()));
        msg(player, "moderation.setjail.done",
            "%jail%", args[0].toLowerCase(Locale.ROOT),
            "%location%", LocationFormatter.human(player.getLocation()));
        return true;
    }

    private boolean handleShadowMute(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            msg(sender, "moderation.shadowmute.usage");
            return true;
        }
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[0]);
        if (target == null) {
            return true;
        }
        boolean enabled = !plugin.getModerationService().isShadowMuted(target.getUniqueId());
        plugin.getModerationService().shadowMute(sender, target, enabled);
        msg(sender, enabled ? "moderation.shadowmute.enabled" : "moderation.shadowmute.disabled", "%player%", target.getName());
        return true;
    }

    private boolean handleStaffNote(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length < 2) {
            msg(sender, "moderation.staffnote.usage");
            return true;
        }
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[0]);
        if (target == null) {
            return true;
        }
        String note = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getModerationService().addStaffNote(sender, target, note);
        msg(sender, "moderation.staffnote.done", "%player%", target.getName());
        return true;
    }

    private boolean handleKit(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.kit")) {
            return true;
        }
        String profile = currentProfile(player);

        if (args.length == 0) {
            if (plugin.getConfig().getBoolean("gui.kits.enabled", true)) {
                plugin.getMenuService().openKitMenu(player);
                return true;
            }
            List<String> available = plugin.getKitService().getAvailableKits(player, profile).stream()
                .map(KitDefinition::getKey)
                .sorted()
                .toList();
            if (available.isEmpty()) {
                msg(player, "kits.usage");
                return true;
            }
            msg(player, "kits.list-header", "%kits%", String.join(", ", available));
            return true;
        }

        KitDefinition kit = plugin.getKitService().getKit(args[0]);
        if (kit == null) {
            msg(player, "kits.unknown", "%kit%", args[0]);
            return true;
        }
        if (kit.getPermission() != null && !kit.getPermission().isBlank() && !player.hasPermission(kit.getPermission())) {
            msg(player, "kits.no-permission", "%kit%", kit.getKey());
            return true;
        }
        if (!kit.isAvailableIn(profile)) {
            msg(player, "kits.unavailable-profile");
            return true;
        }

        PlayerData data = plugin.getPlayerDataService().get(player);
        long now = System.currentTimeMillis();
        long nextUse = data.getKitCooldown(profile + ":" + kit.getKey());
        if (nextUse > now) {
            long remaining = Math.max(1L, (nextUse - now) / 1000L);
            msg(player, "kits.cooldown", "%kit%", kit.getKey(), "%duration%", TimeFormatter.duration(remaining));
            return true;
        }
        if (kit.getCost() > 0.0D) {
            String failure = plugin.getEconomyService().charge(player, kit.getCost(), "Crossroads kit " + kit.getKey());
            if (failure != null) {
                String reason = isMessageKey(failure)
                    ? plugin.getLanguageService().get(player, failure)
                    : failure;
                msg(player, "kits.cannot-afford", "%reason%", reason);
                return true;
            }
        }

        for (ItemStack item : kit.getItems()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            leftovers.values().forEach(leftover -> Objects.requireNonNull(player.getWorld()).dropItemNaturally(player.getLocation(), leftover));
        }
        for (String rawCommand : kit.getCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), rawCommand.replace("%player%", player.getName()));
        }
        if (kit.getCooldownSeconds() > 0L) {
            data.setKitCooldown(profile + ":" + kit.getKey(), now + (kit.getCooldownSeconds() * 1000L));
            plugin.getPlayerDataService().save(player);
        }
        msg(player, "kits.claimed", "%kit%", kit.getDisplayName());
        return true;
    }

    private boolean handleRules(CommandSender sender) {
        if (!requirePermission(sender, "crossroads.rules")) {
            return true;
        }
        List<String> rules = plugin.getConfig().getStringList("rules");
        if (rules.isEmpty()) {
            msg(sender, "common.feature-disabled");
            return true;
        }
        for (String line : rules) {
            Chat.sendRaw(plugin, sender, "<muted>- <subtle>" + line);
        }
        return true;
    }

    private boolean handleMotd(CommandSender sender) {
        plugin.getTextPageService().sendMotd(sender, sender instanceof Player player ? player : null);
        return true;
    }

    private boolean handleHelp(CommandSender sender, String[] args) {
        plugin.getTextPageService().sendHelp(sender, sender instanceof Player player ? player : null, args.length > 0 ? args[0] : null);
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        plugin.getTextPageService().sendInfo(sender, sender instanceof Player player ? player : null, args.length > 0 ? args[0] : null);
        return true;
    }

    private boolean handleNick(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.msg")) {
            return true;
        }
        if (args.length == 0) {
            msg(player, "social.nick.usage");
            return true;
        }
        PlayerData data = plugin.getPlayerDataService().get(player);
        if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("reset")) {
            data.setNickname("");
            player.setDisplayName(player.getName());
            player.setCustomName(null);
            plugin.getPlayerDataService().save(player);
            msg(player, "social.nick.cleared");
            return true;
        }
        String nickname = String.join(" ", args);
        data.setNickname(nickname);
        player.setDisplayName(Chat.color(plugin, nickname));
        player.setCustomName(Chat.color(plugin, nickname));
        plugin.getPlayerDataService().save(player);
        msg(player, "social.nick.set", "%nick%", nickname);
        return true;
    }

    private boolean handleLanguage(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.language")) {
            return true;
        }

        if (args.length == 0) {
            msg(sender, "admin.language.current", "%language%", plugin.getLanguageService().resolveStyle(sender));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            msg(sender, "admin.language.list-header",
                "%languages%", String.join(", ", plugin.getLanguageService().getAvailableLanguages()));
            return true;
        }

        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        boolean allowPlayerLanguage = plugin.getConfig().getBoolean("localization.allow_player_language", true);
        if (args[0].equalsIgnoreCase("sync")) {
            if (!allowPlayerLanguage) {
                msg(player, "admin.language.disabled");
                return true;
            }
            plugin.getLanguageService().clearPlayerLanguage(player);
            msg(player, "admin.language.synced");
            return true;
        }

        if (!allowPlayerLanguage) {
            msg(player, "admin.language.disabled");
            return true;
        }

        if (!plugin.getLanguageService().setPlayerLanguage(player, args[0])) {
            msg(player, "admin.language.unknown", "%language%", args[0]);
            return true;
        }
        msg(player, "admin.language.set", "%language%", plugin.getLanguageService().resolveStyle(player));
        return true;
    }

    private boolean handleBalance(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.economy.balance")) {
            return true;
        }
        if (!plugin.getEconomyService().isAvailable()) {
            msg(sender, "economy.unavailable");
            return true;
        }

        if (args.length == 0) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            msg(player, "economy.balance.self",
                "%balance%", plugin.getEconomyService().format(plugin.getEconomyService().getBalance(player)));
            return true;
        }

        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[0]);
        if (target == null) {
            return true;
        }
        msg(sender, "economy.balance.other",
            "%player%", target.getName() == null ? args[0] : target.getName(),
            "%balance%", plugin.getEconomyService().format(plugin.getEconomyService().getBalance(target)));
        return true;
    }

    private boolean handlePay(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.economy.pay")) {
            return true;
        }
        if (!plugin.getEconomyService().isAvailable()) {
            msg(player, "economy.unavailable");
            return true;
        }
        if (args.length < 2) {
            msg(player, "economy.pay.usage");
            return true;
        }

        OfflinePlayer target = requireKnownOfflinePlayer(player, args[0]);
        if (target == null) {
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            msg(player, "economy.pay.self");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            msg(player, "economy.invalid-amount");
            return true;
        }
        if (amount <= 0.0D) {
            msg(player, "economy.invalid-amount");
            return true;
        }

        String failure = plugin.getEconomyService().transfer(player, target, amount);
        if (failure != null) {
            sendFailure(player, failure);
            return true;
        }

        String formatted = plugin.getEconomyService().format(amount);
        msg(player, "economy.pay.sent", "%balance%", formatted, "%player%", target.getName() == null ? args[0] : target.getName());
        if (target.isOnline() && target.getPlayer() != null) {
            msg(target.getPlayer(), "economy.pay.received", "%balance%", formatted, "%player%", player.getName());
        }
        return true;
    }

    private boolean handleBalTop(CommandSender sender, String[] args) {
        if (!(sender.hasPermission("crossroads.economy.baltop")
            || sender.hasPermission("crossroads.economy.balance")
            || sender.hasPermission("crossroads.admin"))) {
            msg(sender, "common.no-permission");
            return true;
        }
        if (!plugin.getEconomyService().isAvailable()) {
            msg(sender, "economy.unavailable");
            return true;
        }

        int limit = 10;
        if (args.length > 0) {
            try {
                limit = Integer.parseInt(args[0]);
            } catch (NumberFormatException exception) {
                limit = 10;
            }
        }
        List<EconomyService.BalanceEntry> entries = plugin.getEconomyService().topBalances(Math.max(1, Math.min(limit, 50)));
        if (entries.isEmpty()) {
            msg(sender, "economy.baltop.empty");
            return true;
        }
        msg(sender, "economy.baltop.header");
        int rank = 1;
        for (EconomyService.BalanceEntry entry : entries) {
            msgRaw(sender, "economy.baltop.line",
                "%rank%", String.valueOf(rank++),
                "%player%", entry.name() == null || entry.name().isBlank() ? entry.uuid().toString() : entry.name(),
                "%balance%", plugin.getEconomyService().format(entry.balance()));
        }
        return true;
    }

    private boolean handleEco(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.economy.admin")) {
            return true;
        }
        if (!plugin.getEconomyService().isAvailable()) {
            msg(sender, "economy.unavailable");
            return true;
        }
        if (args.length < 3) {
            msg(sender, "economy.eco.usage");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[1]);
        if (target == null) {
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException exception) {
            msg(sender, "economy.invalid-amount");
            return true;
        }
        if (amount < 0.0D || (("give".equals(action) || "take".equals(action)) && amount <= 0.0D)) {
            msg(sender, "economy.invalid-amount");
            return true;
        }

        String formatted = plugin.getEconomyService().format(amount);
        String playerName = target.getName() == null ? args[1] : target.getName();
        String failure = switch (action) {
            case "give" -> plugin.getEconomyService().deposit(target, amount, "eco give");
            case "take" -> {
                if (target.isOnline() && target.getPlayer() != null) {
                    yield plugin.getEconomyService().charge(target.getPlayer(), amount, "eco take");
                }
                double current = plugin.getEconomyService().getBalance(target);
                if (current < amount) {
                    yield "economy.insufficient-funds";
                }
                yield plugin.getEconomyService().setBalance(target, current - amount);
            }
            case "set" -> plugin.getEconomyService().setBalance(target, amount);
            default -> {
                msg(sender, "economy.eco.usage");
                yield "__usage__";
            }
        };
        if ("__usage__".equals(failure)) {
            return true;
        }
        if (failure != null) {
            sendFailure(sender, failure);
            return true;
        }

        String key = switch (action) {
            case "give" -> "economy.eco.give";
            case "take" -> "economy.eco.take";
            default -> "economy.eco.set";
        };
        msg(sender, key, "%balance%", formatted, "%player%", playerName);
        return true;
    }

    private boolean handleCrPerms(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.perms.admin")) {
            return true;
        }
        if (!plugin.getPermissionService().isEnabled()) {
            msg(sender, "permissions.disabled");
            return true;
        }
        if (args.length == 0) {
            msg(sender, "permissions.usage");
            return true;
        }

        String root = args[0].toLowerCase(Locale.ROOT);
        return switch (root) {
            case "user" -> handleCrPermsUser(sender, args);
            case "group" -> handleCrPermsGroup(sender, args);
            case "list" -> {
                String groups = plugin.getPermissionService().getGroups().keySet().stream().sorted().collect(Collectors.joining(", "));
                msg(sender, "permissions.list.header", "%groups%", groups.isBlank() ? "-" : groups);
                yield true;
            }
            case "check" -> handleCrPermsCheck(sender, args);
            case "import" -> handleCrPermsImport(sender, args);
            default -> {
                msg(sender, "permissions.usage");
                yield true;
            }
        };
    }

    private boolean handleCrPermsUser(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msg(sender, "permissions.user.usage");
            return true;
        }
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[1]);
        if (target == null) {
            return true;
        }
        PlayerData data = plugin.getPlayerDataService().get(target.getUniqueId());
        plugin.getPermissionService().ensurePlayerDefaults(data);
        String action = args[2].toLowerCase(Locale.ROOT);
        String playerName = target.getName() == null ? args[1] : target.getName();

        if (action.equals("info")) {
            msg(sender, "permissions.user.info",
                "%player%", playerName,
                "%primary%", data.getPrimaryGroup().isBlank() ? plugin.getPermissionService().getDefaultGroup() : data.getPrimaryGroup(),
                "%groups%", String.join(", ", data.getPermissionGroups()));
            return true;
        }

        if (action.equals("permission")) {
            if (args.length < 5) {
                msg(sender, "permissions.user.usage");
                return true;
            }
            String op = args[3].toLowerCase(Locale.ROOT);
            String permission = args[4];
            if (op.equals("set")) {
                boolean value = args.length < 6 || Boolean.parseBoolean(args[5]);
                data.setPermissionNode(PermissionNode.permanent(permission, value));
                plugin.getPlayerDataService().save(target.getUniqueId());
                refreshPermissions(target);
                msg(sender, "permissions.user.permission-set",
                    "%permission%", permission, "%player%", playerName, "%value%", String.valueOf(value));
                return true;
            }
            if (op.equals("unset")) {
                data.unsetPermissionNode(permission, "", "");
                plugin.getPlayerDataService().save(target.getUniqueId());
                refreshPermissions(target);
                msg(sender, "permissions.user.permission-unset", "%permission%", permission, "%player%", playerName);
                return true;
            }
            msg(sender, "permissions.user.usage");
            return true;
        }

        if (action.equals("parent")) {
            if (args.length < 5) {
                msg(sender, "permissions.user.usage");
                return true;
            }
            String op = args[3].toLowerCase(Locale.ROOT);
            String group = args[4];
            plugin.getPermissionService().createGroup(group);
            if (op.equals("add")) {
                data.addPermissionGroup(group);
                plugin.getPlayerDataService().save(target.getUniqueId());
                refreshPermissions(target);
                msg(sender, "permissions.user.parent-add", "%group%", group.toLowerCase(Locale.ROOT), "%player%", playerName);
                return true;
            }
            if (op.equals("remove")) {
                data.removePermissionGroup(group);
                plugin.getPlayerDataService().save(target.getUniqueId());
                refreshPermissions(target);
                msg(sender, "permissions.user.parent-remove", "%group%", group.toLowerCase(Locale.ROOT), "%player%", playerName);
                return true;
            }
            msg(sender, "permissions.user.usage");
            return true;
        }

        if (action.equals("group")) {
            if (args.length < 4) {
                msg(sender, "permissions.user.usage");
                return true;
            }
            String group = args[3];
            plugin.getPermissionService().createGroup(group);
            data.setPrimaryGroup(group);
            data.addPermissionGroup(group);
            plugin.getPlayerDataService().save(target.getUniqueId());
            refreshPermissions(target);
            msg(sender, "permissions.user.group-set", "%player%", playerName, "%group%", group.toLowerCase(Locale.ROOT));
            return true;
        }

        msg(sender, "permissions.user.usage");
        return true;
    }

    private boolean handleCrPermsGroup(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msg(sender, "permissions.group.usage");
            return true;
        }
        String groupName = args[1].toLowerCase(Locale.ROOT);
        String action = args[2].toLowerCase(Locale.ROOT);

        if (action.equals("create")) {
            if (plugin.getPermissionService().getGroup(groupName) != null) {
                msg(sender, "permissions.group.exists", "%group%", groupName);
                return true;
            }
            plugin.getPermissionService().createGroup(groupName);
            msg(sender, "permissions.group.created", "%group%", groupName);
            return true;
        }

        if (action.equals("delete")) {
            if (!plugin.getPermissionService().deleteGroup(groupName)) {
                msg(sender, "permissions.group.missing", "%group%", groupName);
                return true;
            }
            msg(sender, "permissions.group.deleted", "%group%", groupName);
            return true;
        }

        PermissionGroup group = plugin.getPermissionService().getGroup(groupName);
        if (group == null && !action.equals("create")) {
            msg(sender, "permissions.group.missing", "%group%", groupName);
            return true;
        }

        if (action.equals("info")) {
            msg(sender, "permissions.group.info",
                "%group%", group.getName(),
                "%weight%", String.valueOf(group.getWeight()),
                "%parents%", String.join(", ", group.getParents()));
            return true;
        }

        if (action.equals("permission")) {
            if (args.length < 5) {
                msg(sender, "permissions.group.usage");
                return true;
            }
            String op = args[3].toLowerCase(Locale.ROOT);
            String permission = args[4];
            if (op.equals("set")) {
                boolean value = args.length < 6 || Boolean.parseBoolean(args[5]);
                group.setNode(PermissionNode.permanent(permission, value));
                plugin.getPermissionService().saveGroups();
                plugin.getPermissionService().refreshOnlinePlayers();
                msg(sender, "permissions.group.permission-set",
                    "%permission%", permission, "%group%", groupName, "%value%", String.valueOf(value));
                return true;
            }
            if (op.equals("unset")) {
                group.unsetNode(permission, "", "");
                plugin.getPermissionService().saveGroups();
                plugin.getPermissionService().refreshOnlinePlayers();
                msg(sender, "permissions.group.permission-unset", "%permission%", permission, "%group%", groupName);
                return true;
            }
            msg(sender, "permissions.group.usage");
            return true;
        }

        if (action.equals("parent")) {
            if (args.length < 5) {
                msg(sender, "permissions.group.usage");
                return true;
            }
            String op = args[3].toLowerCase(Locale.ROOT);
            String parent = args[4];
            if (op.equals("add")) {
                group.addParent(parent);
                plugin.getPermissionService().saveGroups();
                plugin.getPermissionService().refreshOnlinePlayers();
                msg(sender, "permissions.group.parent-add", "%parent%", parent.toLowerCase(Locale.ROOT), "%group%", groupName);
                return true;
            }
            if (op.equals("remove")) {
                group.removeParent(parent);
                plugin.getPermissionService().saveGroups();
                plugin.getPermissionService().refreshOnlinePlayers();
                msg(sender, "permissions.group.parent-remove", "%parent%", parent.toLowerCase(Locale.ROOT), "%group%", groupName);
                return true;
            }
            msg(sender, "permissions.group.usage");
            return true;
        }

        if (action.equals("meta")) {
            if (args.length < 5) {
                msg(sender, "permissions.group.usage");
                return true;
            }
            String field = args[3].toLowerCase(Locale.ROOT);
            String value = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
            switch (field) {
                case "prefix" -> group.setPrefix(value);
                case "suffix" -> group.setSuffix(value);
                case "weight" -> {
                    try {
                        group.setWeight(Integer.parseInt(value));
                    } catch (NumberFormatException exception) {
                        msg(sender, "permissions.group.usage");
                        return true;
                    }
                }
                default -> {
                    msg(sender, "permissions.group.usage");
                    return true;
                }
            }
            plugin.getPermissionService().saveGroups();
            plugin.getPermissionService().refreshOnlinePlayers();
            msg(sender, "permissions.group.meta", "%group%", groupName, "%field%", field, "%value%", value);
            return true;
        }

        msg(sender, "permissions.group.usage");
        return true;
    }

    private boolean handleCrPermsCheck(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msg(sender, "permissions.usage");
            return true;
        }
        OfflinePlayer target = requireKnownOfflinePlayer(sender, args[1]);
        if (target == null) {
            return true;
        }
        PlayerData data = plugin.getPlayerDataService().get(target.getUniqueId());
        String world = target.isOnline() && target.getPlayer() != null
            ? target.getPlayer().getWorld().getName()
            : "";
        boolean value = plugin.getPermissionService().hasPermission(data, args[2], world);
        if (target.isOnline() && target.getPlayer() != null) {
            value = target.getPlayer().hasPermission(args[2]);
        }
        msg(sender, "permissions.check.result",
            "%player%", target.getName() == null ? args[1] : target.getName(),
            "%permission%", args[2],
            "%value%", String.valueOf(value));
        return true;
    }

    private boolean handleCrPermsImport(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("luckperms")) {
            msg(sender, "permissions.usage");
            return true;
        }
        int imported = plugin.getPermissionService().importLuckPermsYaml();
        if (imported <= 0) {
            msg(sender, "permissions.import.none");
            return true;
        }
        msg(sender, "permissions.import.done", "%count%", String.valueOf(imported));
        return true;
    }

    private void refreshPermissions(OfflinePlayer target) {
        if (target.isOnline() && target.getPlayer() != null) {
            plugin.getPermissionService().applyAttachments(target.getPlayer());
        }
    }

    private boolean handleCrossroads(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("about")) {
            msg(sender, "admin.about",
                "%version%", plugin.getDescription().getVersion(),
                "%storage%", String.valueOf(plugin.getStorageManager().getProvider().getType()),
                "%economy%", plugin.getEconomyService().getProviderName(),
                "%language%", plugin.getLanguageService().resolveStyle(sender));
            return true;
        }

        if (args[0].equalsIgnoreCase("language")) {
            return handleLanguage(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        if (args[0].equalsIgnoreCase("perms")) {
            return handleCrPerms(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        if (!requirePermission(sender, "crossroads.admin")) {
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadCrossroads();
            msg(sender, "admin.reloaded");
            return true;
        }

        if (args[0].equalsIgnoreCase("modules")) {
            if (!requireFeature(sender, "modules")) {
                return true;
            }
            var modules = plugin.getModuleManager().getModules();
            if (modules.isEmpty()) {
                msg(sender, "admin.modules.none");
                return true;
            }
            msg(sender, "admin.modules.header");
            modules.stream()
                .sorted(Comparator.comparing(module -> module.getId()))
                .forEach(module -> msgRaw(sender, "admin.modules.line",
                    "%name%", module.getId(),
                    "%version%", plugin.getDescription().getVersion()));
            return true;
        }

        if (args[0].equalsIgnoreCase("backup")) {
            if (!requireFeature(sender, "backups")) {
                return true;
            }
            if (args.length < 2 || !args[1].equalsIgnoreCase("create")) {
                msg(sender, "admin.backup.usage");
                return true;
            }
            try {
                java.io.File backup = plugin.getBackupService().createBackup("manual");
                msg(sender, "admin.backup.created", "%file%", backup.getName());
            } catch (Exception exception) {
                msg(sender, "admin.backup.failed", "%reason%", exception.getMessage() == null ? "unknown" : exception.getMessage());
                plugin.getLogger().warning("Manual backup failed: " + exception.getMessage());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("import")) {
            if (args.length < 2 || !args[1].equalsIgnoreCase("essentials")) {
                msg(sender, "admin.import.usage");
                return true;
            }
            var result = plugin.getImportService().importEssentials();
            if (!result.found()) {
                msg(sender, "admin.import.failed", "%reason%", "No Essentials folder was found to import.");
                return true;
            }
            String summary = result.homes() + " homes, " + result.warps() + " warps, " + result.nicknames() + " nicknames";
            msg(sender, "admin.import.done", "%summary%", summary);
            return true;
        }

        msg(sender, "admin.usage");
        return true;
    }

    private boolean teleport(Player player, Location destination, String successKey, String... replacements) {
        String protectionFailure = plugin.getProtectionCompatibilityService().checkTeleportAccess(player, destination);
        if (protectionFailure != null) {
            sendFailure(player, protectionFailure);
            return false;
        }
        plugin.getBackService().record(player, player.getLocation());
        player.teleport(destination);
        msg(player, successKey, replacements);
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        msg(sender, "common.players-only");
        return null;
    }

    private boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("crossroads.admin")) {
            return true;
        }
        msg(sender, "common.no-permission");
        return false;
    }

    private boolean requireFeature(CommandSender sender, String featureKey) {
        if (plugin.isFeatureEnabled(featureKey)) {
            return true;
        }
        msg(sender, "common.feature-disabled");
        return false;
    }

    private boolean checkCooldown(Player player, String key) {
        if (player.hasPermission("crossroads.cooldown.bypass")) {
            return true;
        }
        long seconds = plugin.getConfig().getLong("cooldowns.commands." + key, 0L);
        if (seconds <= 0L) {
            return true;
        }
        long nextUse = plugin.getPlayerDataService().get(player).getCommandCooldown(key);
        if (nextUse <= System.currentTimeMillis()) {
            return true;
        }
        long remaining = Math.max(1L, (nextUse - System.currentTimeMillis()) / 1000L);
        msg(player, "common.cooldown", "%duration%", TimeFormatter.duration(remaining));
        return false;
    }

    private void applyCooldown(Player player, String key) {
        long seconds = plugin.getConfig().getLong("cooldowns.commands." + key, 0L);
        if (seconds <= 0L) {
            return;
        }
        PlayerData data = plugin.getPlayerDataService().get(player);
        data.setCommandCooldown(key, System.currentTimeMillis() + (seconds * 1000L));
        plugin.getPlayerDataService().save(player);
    }

    private String chargeTeleportCost(Player player, String key) {
        if (!plugin.isFeatureEnabled("economy")) {
            return null;
        }
        double cost = plugin.getConfig().getDouble("economy.costs." + key, 0.0D);
        return plugin.getEconomyService().charge(player, cost, "Crossroads " + key);
    }

    private List<String> accessibleHomeNames(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        String profile = currentProfile(player);
        plugin.getPlayerDataService().get(player).getHomes(profile).keySet().stream().sorted().forEach(names::add);
        if (!PlayerData.GLOBAL_SCOPE.equals(profile)) {
            plugin.getPlayerDataService().get(player).getHomes(PlayerData.GLOBAL_SCOPE).keySet().stream()
                .sorted()
                .map(name -> PlayerData.GLOBAL_SCOPE + ":" + name)
                .forEach(names::add);
        }
        return names;
    }

    private List<String> accessibleWarpNames(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        return plugin.getWarpService().getAvailableWarpNames(currentProfile(player));
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

    private OfflinePlayer requireKnownOfflinePlayer(CommandSender sender, String input) {
        OfflinePlayer target = resolveKnownOfflinePlayer(input);
        if (target != null) {
            return target;
        }
        msg(sender, "common.player-not-found", "%player%", input);
        return null;
    }

    private OfflinePlayer resolveKnownOfflinePlayer(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        Player online = plugin.getServer().getPlayerExact(input);
        if (online != null) {
            return online;
        }

        String lowered = input.toLowerCase(Locale.ROOT);
        return Arrays.stream(Bukkit.getOfflinePlayers())
            .filter(player -> player.getName() != null)
            .filter(player -> player.getName().equalsIgnoreCase(input)
                || player.getName().toLowerCase(Locale.ROOT).startsWith(lowered))
            .sorted(Comparator.comparingInt(player -> player.getName().equalsIgnoreCase(input) ? 0 : 1))
            .findFirst()
            .orElse(null);
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
            case "spawn", "setspawn", "tpa", "tpahere", "tpaccept", "tpdeny", "tpacancel", "rtp" -> "teleports";
            case "back" -> "back";
            case "msg", "reply", "ignore", "mail", "nick" -> "messaging";
            case "fly", "vanish", "staffmode", "socialspy" -> "staff-tools";
            case "invsee", "endersee", "seen" -> "inspection";
            case "freeze", "unfreeze", "mute", "unmute", "warn", "stafflog", "history", "kick", "tempban", "unban", "jail", "unjail", "setjail", "shadowmute", "staffnote" -> "moderation";
            case "kit" -> "kits";
            case "motd", "help", "info", "rules" -> "text-pages";
            case "language" -> null;
            case "balance", "pay", "baltop", "eco" -> "economy";
            case "crperms", "cperm" -> "permissions";
            default -> null;
        };
    }

    private String defaultHomeName() {
        return plugin.getConfig().getString("players.homes.default-name", "home").toLowerCase(Locale.ROOT);
    }

    private String currentProfile(Player player) {
        return plugin.getWorldProfileService().resolveProfile(player.getWorld());
    }

    private int parseIndex(String raw) {
        try {
            return Math.max(0, Integer.parseInt(raw) - 1);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private int randomSigned(int minRadius, int maxRadius) {
        int value = ThreadLocalRandom.current().nextInt(minRadius, maxRadius + 1);
        return ThreadLocalRandom.current().nextBoolean() ? value : -value;
    }

    private ScopedName parseScopedName(Player player, String raw, String fallbackName) {
        if (raw == null || raw.isBlank()) {
            return new ScopedName(currentProfile(player), fallbackName.toLowerCase(Locale.ROOT));
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        if (separator <= 0 || separator >= normalized.length() - 1) {
            return new ScopedName(currentProfile(player), normalized);
        }
        return new ScopedName(normalized.substring(0, separator), normalized.substring(separator + 1));
    }

    private String formatDurationLabel(long seconds) {
        if (seconds == Long.MAX_VALUE) {
            return "permanent";
        }
        return TimeFormatter.duration(seconds);
    }

    private boolean isMessageKey(String value) {
        return value != null && !value.isBlank() && !value.contains(" ") && (value.contains(".") || value.startsWith("economy."));
    }

    private void sendFailure(CommandSender sender, String failure) {
        if (failure == null) {
            return;
        }
        if (isMessageKey(failure)) {
            msg(sender, failure);
            return;
        }
        Chat.send(plugin, sender, "<error>" + failure);
    }

    private void msg(CommandSender sender, String key, String... replacements) {
        plugin.getLanguageService().send(sender, key, replacements);
    }

    private void msgRaw(CommandSender sender, String key, String... replacements) {
        plugin.getLanguageService().sendRaw(sender, key, replacements);
    }

    private record ScopedName(String scope, String name) {
        private String raw() {
            return PlayerData.GLOBAL_SCOPE.equals(scope) ? scope + ":" + name : scope + ":" + name;
        }
    }
}
