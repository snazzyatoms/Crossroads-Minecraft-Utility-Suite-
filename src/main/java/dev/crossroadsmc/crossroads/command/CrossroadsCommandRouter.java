package dev.crossroadsmc.crossroads.command;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.api.event.HomeTeleportEvent;
import dev.crossroadsmc.crossroads.model.KitDefinition;
import dev.crossroadsmc.crossroads.model.MailMessage;
import dev.crossroadsmc.crossroads.model.ModerationLogEntry;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import dev.crossroadsmc.crossroads.model.TeleportRequest;
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
import java.util.Set;
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
            case "msg", "ignore", "tpa", "tpahere", "kick", "freeze", "mute", "jail", "shadowmute" ->
                args.length == 1 ? filterPrefix(onlineNames(sender), args[0]) : Collections.emptyList();
            case "reply", "tpaccept", "tpdeny", "tpacancel", "fly", "vanish", "staffmode", "socialspy", "back", "rtp", "motd", "rules" ->
                Collections.emptyList();
            case "unfreeze", "unmute", "warn", "stafflog", "history", "seen", "unban", "unjail", "staffnote" ->
                args.length == 1 ? filterPrefix(knownNames(), args[0]) : Collections.emptyList();
            case "setjail" -> args.length == 1 ? Collections.emptyList() : Collections.emptyList();
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
            case "crossroads" -> tabCompleteCrossroads(args);
            default -> Collections.emptyList();
        };
    }

    private List<String> tabCompleteCrossroads(String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("reload", "backup", "about", "modules", "import"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("backup")) {
            return filterPrefix(List.of("create"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("import")) {
            return filterPrefix(List.of("essentials"), args[1]);
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
            Chat.send(plugin, player, "<error>You do not have a home named <warn>" + scopedName.raw() + "<error>.");
            return true;
        }

        Location destination = savedLocation.toLocation();
        if (destination == null) {
            Chat.send(plugin, player, "<error>That home points to a world that is not loaded.");
            return true;
        }

        HomeTeleportEvent event = new HomeTeleportEvent(player, scopedName.name(), destination);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getDestination() == null) {
            Chat.send(plugin, player, "<error>Your home teleport was cancelled.");
            return true;
        }

        String failure = chargeTeleportCost(player, "home");
        if (failure != null) {
            Chat.send(plugin, player, "<error>" + failure);
            return true;
        }

        if (teleport(player, event.getDestination(), "<success>Welcome back to home <warn>" + scopedName.raw() + "<success>.")) {
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
            Chat.send(plugin, player, "<error>You have reached your home limit of <warn>" + homeLimit + "<error> in <warn>" + scopedName.scope() + "<error>.");
            return true;
        }

        data.setHome(scopedName.scope(), scopedName.name(), SavedLocation.fromLocation(player.getLocation()));
        plugin.getPlayerDataService().save(player);
        Chat.send(plugin, player, "<success>Saved home <warn>" + scopedName.raw() + "<success> at <text>" + LocationFormatter.human(player.getLocation()));
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
            Chat.send(plugin, player, "<error>No home named <warn>" + scopedName.raw() + "<error> exists.");
            return true;
        }

        plugin.getPlayerDataService().save(player);
        Chat.send(plugin, player, "<success>Deleted home <warn>" + scopedName.raw() + "<success>.");
        return true;
    }

    private boolean handleHomes(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.home")) {
            return true;
        }

        List<String> names = accessibleHomeNames(player);
        if (names.isEmpty()) {
            Chat.send(plugin, player, "<subtle>You have not set any homes yet.");
            return true;
        }

        Chat.send(plugin, player, "<warn>Your homes<subtle>: <text>" + String.join(", ", names));
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
                Chat.send(plugin, player, "<subtle>No warps are available right now.");
                return true;
            }
            Chat.send(plugin, player, "<info>Available warps<subtle>: <text>" + String.join(", ", warps));
            return true;
        }

        if (!checkCooldown(player, "warp")) {
            return true;
        }

        ScopedName scopedName = parseScopedName(player, args[0], args[0]);
        SavedLocation warp = plugin.getWarpService().getWarp(scopedName.scope(), scopedName.name());
        if (warp == null) {
            Chat.send(plugin, player, "<error>Unknown warp <warn>" + scopedName.raw() + "<error>.");
            return true;
        }

        Location destination = warp.toLocation();
        if (destination == null) {
            Chat.send(plugin, player, "<error>That warp points to a world that is not loaded.");
            return true;
        }

        String failure = chargeTeleportCost(player, "warp");
        if (failure != null) {
            Chat.send(plugin, player, "<error>" + failure);
            return true;
        }

        if (teleport(player, destination, "<success>Warped to <warn>" + scopedName.raw() + "<success>.")) {
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
            Chat.send(plugin, player, "<error>Usage: <warn>/setwarp [profile:]<name>");
            return true;
        }

        ScopedName scopedName = parseScopedName(player, args[0], args[0]);
        plugin.getWarpService().setWarp(scopedName.scope(), scopedName.name(), SavedLocation.fromLocation(player.getLocation()));
        Chat.send(plugin, player, "<success>Saved warp <warn>" + scopedName.raw() + "<success> at <text>" + LocationFormatter.human(player.getLocation()));
        return true;
    }

    private boolean handleDelWarp(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.admin")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, player, "<error>Usage: <warn>/delwarp [profile:]<name>");
            return true;
        }

        ScopedName scopedName = parseScopedName(player, args[0], args[0]);
        SavedLocation removed = plugin.getWarpService().removeWarp(scopedName.scope(), scopedName.name());
        if (removed == null) {
            Chat.send(plugin, player, "<error>Unknown warp <warn>" + scopedName.raw() + "<error>.");
            return true;
        }

        Chat.send(plugin, player, "<success>Deleted warp <warn>" + scopedName.raw() + "<success>.");
        return true;
    }

    private boolean handleWarps(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.warp")) {
            return true;
        }

        List<String> warps = plugin.getWarpService().getAvailableWarpNames(currentProfile(player));
        if (warps.isEmpty()) {
            Chat.send(plugin, player, "<subtle>No warps have been created yet.");
            return true;
        }

        Chat.send(plugin, player, "<warn>Warps<subtle>: <text>" + String.join(", ", warps));
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
            Chat.send(plugin, player, "<error>Spawn has not been configured yet.");
            return true;
        }

        Location location = spawn.toLocation();
        if (location == null) {
            Chat.send(plugin, player, "<error>Spawn points to a world that is not loaded.");
            return true;
        }

        String failure = chargeTeleportCost(player, "spawn");
        if (failure != null) {
            Chat.send(plugin, player, "<error>" + failure);
            return true;
        }

        if (teleport(player, location, "<success>Teleported to spawn.")) {
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
        Chat.send(plugin, player, "<success>Spawn profile <warn>" + profile + "<success> updated to <text>" + LocationFormatter.human(player.getLocation()));
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
            Chat.send(plugin, player, "<error>No previous location is available yet.");
            return true;
        }

        Location location = backLocation.toLocation();
        if (location == null) {
            Chat.send(plugin, player, "<error>Your back location points to a world that is not loaded.");
            return true;
        }

        String failure = chargeTeleportCost(player, "back");
        if (failure != null) {
            Chat.send(plugin, player, "<error>" + failure);
            return true;
        }

        if (teleport(player, location, "<success>Returned to your previous location.")) {
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
            Chat.send(plugin, player, "<error>Usage: <warn>/msg <player> <message>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            Chat.send(plugin, player, "<error>That player is not online.");
            return true;
        }
        if (target.equals(player)) {
            Chat.send(plugin, player, "<error>Sending yourself a DM is not especially productive.");
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

        plugin.getMessagingService().sendMessage(player, target, String.join(" ", args));
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
        if (target == null) {
            Chat.send(plugin, player, "<error>That player is not online.");
            return true;
        }
        if (target.equals(player)) {
            Chat.send(plugin, player, "<error>Ignoring yourself is not supported.");
            return true;
        }

        boolean ignored = plugin.getMessagingService().toggleIgnore(player, target);
        Chat.send(plugin, player, ignored
            ? "<success>You are now ignoring <warn>" + target.getName() + "<success>."
            : "<success>You are no longer ignoring <warn>" + target.getName() + "<success>.");
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
                Chat.send(plugin, player, "<subtle>Your mailbox is empty.");
                return true;
            }
            Chat.send(plugin, player, "<warn>Your Mail");
            for (int index = 0; index < mail.size(); index++) {
                MailMessage message = mail.get(index);
                String marker = message.isRead() ? "<muted>" : "<success>";
                Chat.sendRaw(plugin, player, marker + "#" + (index + 1) + " <text>" + message.getSenderName() + "<subtle>: " + message.getBody());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("send")) {
            if (args.length < 3) {
                Chat.send(plugin, player, "<error>Usage: <warn>/mail send <player> <message>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target.getUniqueId().equals(player.getUniqueId())) {
                Chat.send(plugin, player, "<error>You cannot mail yourself.");
                return true;
            }
            PlayerData targetData = plugin.getPlayerDataService().get(target.getUniqueId());
            targetData.addMail(new MailMessage(System.currentTimeMillis(), player.getUniqueId(), player.getName(),
                String.join(" ", Arrays.copyOfRange(args, 2, args.length)), false));
            plugin.getPlayerDataService().save(target.getUniqueId());
            if (target.isOnline() && target.getPlayer() != null) {
                Chat.send(plugin, target.getPlayer(), "<info>You received new mail from <warn>" + player.getName() + "<info>.");
            }
            Chat.send(plugin, player, "<success>Mail sent to <warn>" + (target.getName() == null ? args[1] : target.getName()) + "<success>.");
            return true;
        }

        if (args[0].equalsIgnoreCase("read")) {
            if (args.length < 2) {
                Chat.send(plugin, player, "<error>Usage: <warn>/mail read <id>");
                return true;
            }
            int index = parseIndex(args[1]);
            MailMessage message = data.getMail(index);
            if (message == null) {
                Chat.send(plugin, player, "<error>No mail entry exists with that id.");
                return true;
            }
            data.markMailRead(index);
            plugin.getPlayerDataService().save(player);
            Chat.send(plugin, player, "<warn>Mail from <text>" + message.getSenderName());
            Chat.sendRaw(plugin, player, "<subtle>" + message.getBody());
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (args.length < 2) {
                Chat.send(plugin, player, "<error>Usage: <warn>/mail clear <id|all>");
                return true;
            }
            if (args[1].equalsIgnoreCase("all")) {
                data.clearMail();
                plugin.getPlayerDataService().save(player);
                Chat.send(plugin, player, "<success>Your mailbox has been cleared.");
                return true;
            }
            int index = parseIndex(args[1]);
            MailMessage removed = data.removeMail(index);
            if (removed == null) {
                Chat.send(plugin, player, "<error>No mail entry exists with that id.");
                return true;
            }
            plugin.getPlayerDataService().save(player);
            Chat.send(plugin, player, "<success>Removed mail from <warn>" + removed.getSenderName() + "<success>.");
            return true;
        }

        Chat.send(plugin, player, "<error>Usage: <warn>/mail <send|read|clear|inbox>");
        return true;
    }

    private boolean handleTeleportRequest(CommandSender sender, String[] args, boolean hereRequest) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.spawn")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, player, hereRequest ? "<error>Usage: <warn>/tpahere <player>" : "<error>Usage: <warn>/tpa <player>");
            return true;
        }
        if (!checkCooldown(player, hereRequest ? "tpahere" : "tpa")) {
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            Chat.send(plugin, player, "<error>That player is not online.");
            return true;
        }
        if (target.equals(player)) {
            Chat.send(plugin, player, "<error>You cannot send a teleport request to yourself.");
            return true;
        }

        String failure = chargeTeleportCost(player, hereRequest ? "tpahere" : "tpa");
        if (failure != null) {
            Chat.send(plugin, player, "<error>" + failure);
            return true;
        }

        plugin.getTeleportRequestService().create(player, target, hereRequest);
        applyCooldown(player, hereRequest ? "tpahere" : "tpa");
        Chat.send(plugin, player, "<success>Teleport request sent to <warn>" + target.getName() + "<success>.");
        Chat.send(plugin, target, hereRequest
            ? "<info><warn>" + player.getName() + "<info> wants you to teleport to them. Use <warn>/tpaccept<info> or <warn>/tpdeny<info>."
            : "<info><warn>" + player.getName() + "<info> wants to teleport to you. Use <warn>/tpaccept<info> or <warn>/tpdeny<info>.");
        return true;
    }

    private boolean handleTeleportAccept(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        TeleportRequest request = plugin.getTeleportRequestService().accept(player);
        if (request == null) {
            Chat.send(plugin, player, "<error>You do not have any pending teleport requests.");
            return true;
        }

        Player requester = plugin.getServer().getPlayer(request.getRequesterUuid());
        if (requester == null || !requester.isOnline()) {
            Chat.send(plugin, player, "<error>The requester is no longer online.");
            return true;
        }

        if (request.isHereRequest()) {
            if (teleport(player, requester.getLocation(), "<success>Teleport request accepted.")) {
                Chat.send(plugin, requester, "<success><warn>" + player.getName() + "<success> accepted your request.");
            }
        } else {
            if (teleport(requester, player.getLocation(), "<success><warn>" + player.getName() + "<success> accepted your request.")) {
                Chat.send(plugin, player, "<success>Teleport request accepted.");
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
            Chat.send(plugin, player, "<error>You do not have any pending teleport requests.");
            return true;
        }
        Player requester = plugin.getServer().getPlayer(request.getRequesterUuid());
        if (requester != null && requester.isOnline()) {
            Chat.send(plugin, requester, "<warn>" + player.getName() + "<error> denied your teleport request.");
        }
        Chat.send(plugin, player, "<success>Teleport request denied.");
        return true;
    }

    private boolean handleTeleportCancel(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!plugin.getTeleportRequestService().cancel(player)) {
            Chat.send(plugin, player, "<error>You do not have a pending teleport request to cancel.");
            return true;
        }
        Chat.send(plugin, player, "<success>Teleport request cancelled.");
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
            Chat.send(plugin, player, "<error>Random teleport is disabled in this world.");
            return true;
        }

        String failure = chargeTeleportCost(player, "rtp");
        if (failure != null) {
            Chat.send(plugin, player, "<error>" + failure);
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
            if (teleport(player, target, "<success>Random teleport complete.")) {
                applyCooldown(player, "rtp");
                return true;
            }
        }

        Chat.send(plugin, player, "<error>Random teleport could not find a safe destination.");
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
            ? "<success>Staff mode enabled. Your moderation toolkit is ready."
            : "<warn>Staff mode disabled. Your normal inventory has been restored.");
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
            Chat.send(plugin, sender, "<error>Usage: <warn>/mute <player> <duration> [reason]");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            Chat.send(plugin, sender, "<error>That player is not online.");
            return true;
        }
        long seconds = DurationParser.parseToSeconds(args[1]);
        if (seconds < 0L) {
            Chat.send(plugin, sender, "<error>Invalid duration. Use values like <warn>30m<error>, <warn>6h<error>, or <warn>perm<error>.");
            return true;
        }
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Muted by staff";
        plugin.getModerationService().mute(sender, target, seconds, reason);
        Chat.send(plugin, sender, "<success>Muted <warn>" + target.getName() + "<success>.");
        Chat.send(plugin, target, "<error>You have been muted. Reason: <text>" + reason);
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
            Chat.send(plugin, sender, "<error>Usage: <warn>/warn <player> [category] <reason>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String category = args.length > 2 ? args[1] : "general";
        String reason = args.length > 2
            ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
            : String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getModerationService().warn(sender, target, "[" + category + "] " + reason);
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
        List<ModerationLogEntry> entries = plugin.getModerationService().getLogs(target, Math.max(1, Math.min(limit, 25)));
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

    private boolean handleKick(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/kick <player> [reason]");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            Chat.send(plugin, sender, "<error>That player is not online.");
            return true;
        }
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Removed by staff";
        plugin.getModerationService().kick(sender, target, reason);
        Chat.send(plugin, sender, "<success>Kicked <warn>" + target.getName() + "<success>.");
        return true;
    }

    private boolean handleTempBan(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length < 2) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/tempban <player> <duration> [reason]");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        long seconds = DurationParser.parseToSeconds(args[1]);
        if (seconds < 0L) {
            Chat.send(plugin, sender, "<error>Invalid duration.");
            return true;
        }
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Banned by staff";
        plugin.getModerationService().tempBan(sender, target, seconds, reason);
        Chat.send(plugin, sender, "<success>Banned <warn>" + target.getName() + "<success>.");
        return true;
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/unban <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.getModerationService().unban(sender, target);
        Chat.send(plugin, sender, "<success>Unbanned <warn>" + target.getName() + "<success>.");
        return true;
    }

    private boolean handleJail(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length < 2) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/jail <player> <jail> [duration] [reason]");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            Chat.send(plugin, sender, "<error>That player is not online.");
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
            Chat.send(plugin, sender, "<error>" + exception.getMessage());
            return true;
        }
        Chat.send(plugin, sender, "<success>Jailed <warn>" + target.getName() + "<success> in <warn>" + args[1] + "<success>.");
        return true;
    }

    private boolean handleUnjail(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/unjail <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.getModerationService().unjail(sender, target);
        if (target.isOnline() && target.getPlayer() != null) {
            Chat.send(plugin, target.getPlayer(), "<success>You have been released from jail.");
        }
        Chat.send(plugin, sender, "<success>Released <warn>" + target.getName() + "<success> from jail.");
        return true;
    }

    private boolean handleSetJail(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "crossroads.admin")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, player, "<error>Usage: <warn>/setjail <name>");
            return true;
        }
        plugin.getJailService().setJail(args[0], SavedLocation.fromLocation(player.getLocation()));
        Chat.send(plugin, player, "<success>Saved jail <warn>" + args[0].toLowerCase(Locale.ROOT) + "<success>.");
        return true;
    }

    private boolean handleShadowMute(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length == 0) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/shadowmute <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        boolean enabled = !plugin.getModerationService().isShadowMuted(target.getUniqueId());
        plugin.getModerationService().shadowMute(sender, target, enabled);
        Chat.send(plugin, sender, enabled
            ? "<success>Shadow mute enabled for <warn>" + target.getName() + "<success>."
            : "<warn>Shadow mute disabled for <text>" + target.getName() + "<warn>.");
        return true;
    }

    private boolean handleStaffNote(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "crossroads.moderation")) {
            return true;
        }
        if (args.length < 2) {
            Chat.send(plugin, sender, "<error>Usage: <warn>/staffnote <player> <note>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String note = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getModerationService().addStaffNote(sender, target, note);
        Chat.send(plugin, sender, "<success>Staff note added for <warn>" + target.getName() + "<success>.");
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
        if (!kit.isAvailableIn(profile)) {
            Chat.send(plugin, player, "<error>That kit is not available in this world profile.");
            return true;
        }

        PlayerData data = plugin.getPlayerDataService().get(player);
        long now = System.currentTimeMillis();
        long nextUse = data.getKitCooldown(profile + ":" + kit.getKey());
        if (nextUse > now) {
            long remaining = Math.max(1L, (nextUse - now) / 1000L);
            Chat.send(plugin, player, "<error>That kit is on cooldown for another <warn>" + TimeFormatter.duration(remaining) + "<error>.");
            return true;
        }
        if (kit.getCost() > 0.0D) {
            String failure = plugin.getEconomyService().charge(player, kit.getCost(), "Crossroads kit " + kit.getKey());
            if (failure != null) {
                Chat.send(plugin, player, "<error>" + failure);
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
            Chat.send(plugin, player, "<error>Usage: <warn>/nick <name|off>");
            return true;
        }
        PlayerData data = plugin.getPlayerDataService().get(player);
        if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("reset")) {
            data.setNickname("");
            player.setDisplayName(player.getName());
            player.setCustomName(null);
            plugin.getPlayerDataService().save(player);
            Chat.send(plugin, player, "<success>Nickname cleared.");
            return true;
        }
        String nickname = String.join(" ", args);
        data.setNickname(nickname);
        player.setDisplayName(Chat.color(plugin, nickname));
        player.setCustomName(Chat.color(plugin, nickname));
        plugin.getPlayerDataService().save(player);
        Chat.send(plugin, player, "<success>Nickname updated to <text>" + nickname);
        return true;
    }

    private boolean handleCrossroads(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("about")) {
            Chat.send(plugin, sender, "<accent>Crossroads <subtle>v<text>" + plugin.getDescription().getVersion()
                + " <muted>| <text>Storage: <warn>" + plugin.getStorageManager().getProvider().getType()
                + " <muted>| <text>Economy: <warn>" + plugin.getEconomyService().getProviderName()
                + " <muted>| <text>AegisGuard: <warn>" + (plugin.getAegisGuardHookService().isAvailable() ? plugin.getAegisGuardHookService().getVersion() : "off")
                + " <muted>| <text>Protections: <warn>" + plugin.getProtectionCompatibilityService().getActiveProviderSummary()
                + " <muted>| <text>Modules: <warn>" + plugin.getModuleManager().getModules().size());
            return true;
        }

        if (!requirePermission(sender, "crossroads.admin")) {
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadCrossroads();
            Chat.send(plugin, sender, "<success>Crossroads has been reloaded.");
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
                java.io.File backup = plugin.getBackupService().createBackup("manual");
                Chat.send(plugin, sender, "<success>Backup created: <text>" + backup.getName());
            } catch (Exception exception) {
                Chat.send(plugin, sender, "<error>Backup failed. Check console for details.");
                plugin.getLogger().warning("Manual backup failed: " + exception.getMessage());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("import")) {
            if (args.length < 2 || !args[1].equalsIgnoreCase("essentials")) {
                Chat.send(plugin, sender, "<error>Usage: <warn>/crossroads import essentials");
                return true;
            }
            var result = plugin.getImportService().importEssentials();
            if (!result.found()) {
                Chat.send(plugin, sender, "<error>No Essentials folder was found to import.");
                return true;
            }
            Chat.send(plugin, sender, "<success>Imported <warn>" + result.homes() + "<success> homes, <warn>" + result.warps()
                + "<success> warps, and <warn>" + result.nicknames() + "<success> nicknames from Essentials.");
            return true;
        }

        Chat.send(plugin, sender, "<error>Unknown subcommand. Try <warn>/crossroads about<error>, <warn>/crossroads reload<error>, <warn>/crossroads modules<error>, <warn>/crossroads backup create<error>, or <warn>/crossroads import essentials<error>.");
        return true;
    }

    private boolean teleport(Player player, Location destination, String successMessage) {
        String protectionFailure = plugin.getProtectionCompatibilityService().checkTeleportAccess(player, destination);
        if (protectionFailure != null) {
            Chat.send(plugin, player, "<error>" + protectionFailure);
            return false;
        }
        plugin.getBackService().record(player, player.getLocation());
        player.teleport(destination);
        Chat.send(plugin, player, successMessage);
        return true;
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
        Chat.send(plugin, player, "<error>That command is on cooldown for another <warn>" + TimeFormatter.duration(remaining) + "<error>.");
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

    private record ScopedName(String scope, String name) {
        private String raw() {
            return PlayerData.GLOBAL_SCOPE.equals(scope) ? scope + ":" + name : scope + ":" + name;
        }
    }
}
