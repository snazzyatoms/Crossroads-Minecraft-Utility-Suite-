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
    private final Map<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();

    public MessagingService(CrossroadsPlugin plugin, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
    }

    public boolean sendMessage(Player sender, Player recipient, String message) {
        PlayerData recipientData = playerDataService.get(recipient);
        if (recipientData.isIgnoring(sender.getUniqueId())) {
            Chat.send(plugin, sender, "&cThat player is ignoring you.");
            return false;
        }

        String incoming = plugin.getConfig().getString("messages.direct.incoming", "&d[from %player%] &f%message%");
            String outgoing = plugin.getConfig().getString("messages.direct.outgoing", "&d[to %player%] &f%message%");

        recipient.sendMessage(Chat.color(incoming
            .replace("%player%", sender.getName())
            .replace("%message%", message)));
        sender.sendMessage(Chat.color(outgoing
            .replace("%player%", recipient.getName())
            .replace("%message%", message)));

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
