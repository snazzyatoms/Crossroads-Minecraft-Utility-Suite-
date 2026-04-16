package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.util.Chat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class MessagingService {
    private final CrossroadsPlugin plugin;
    private final PlayerDataService playerDataService;
    private final StaffService staffService;
    private final ModerationService moderationService;
    private final Map<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();

    public MessagingService(
        CrossroadsPlugin plugin,
        PlayerDataService playerDataService,
        StaffService staffService,
        ModerationService moderationService
    ) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.staffService = staffService;
        this.moderationService = moderationService;
    }

    public boolean sendMessage(Player sender, Player recipient, String message) {
        if (moderationService.isMuted(sender.getUniqueId())) {
            Chat.send(plugin, sender, "<error>You are muted and cannot send private messages right now.");
            return false;
        }

        PlayerData recipientData = playerDataService.get(recipient);
        if (recipientData.isIgnoring(sender.getUniqueId())) {
            Chat.send(plugin, sender, "<error>That player is ignoring you.");
            return false;
        }

        String incoming = plugin.getConfig().getString("messages.direct.incoming", "<highlight>[from %player%] <text>%message%");
        String outgoing = plugin.getConfig().getString("messages.direct.outgoing", "<highlight>[to %player%] <text>%message%");
        String spyFormat = plugin.getConfig().getString("messages.direct.spy", "<muted>[Spy] <highlight>%from% <subtle>-> <highlight>%to%<subtle>: <text>%message%");

        recipient.sendMessage(Chat.color(plugin, incoming
            .replace("%player%", sender.getName())
            .replace("%message%", message)));
        sender.sendMessage(Chat.color(plugin, outgoing
            .replace("%player%", recipient.getName())
            .replace("%message%", message)));

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.equals(sender) || online.equals(recipient)) {
                continue;
            }
            if (staffService.isSocialSpyEnabled(online)) {
                online.sendMessage(Chat.color(plugin, spyFormat
                    .replace("%from%", sender.getName())
                    .replace("%to%", recipient.getName())
                    .replace("%message%", message)));
            }
        }

        lastMessaged.put(sender.getUniqueId(), recipient.getUniqueId());
        lastMessaged.put(recipient.getUniqueId(), sender.getUniqueId());

        if (plugin.getConfig().getBoolean("messages.direct.play-sound", true)) {
            recipient.playSound(recipient.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
        }
        return true;
    }

    public Player getReplyTarget(Player sender) {
        UUID targetId = lastMessaged.get(sender.getUniqueId());
        if (targetId == null) {
            return null;
        }
        return plugin.getServer().getPlayer(targetId);
    }

    public boolean toggleIgnore(Player player, Player target) {
        PlayerData data = playerDataService.get(player);
        boolean ignored = data.toggleIgnore(target.getUniqueId());
        playerDataService.save(player);
        return ignored;
    }
}
