package ru.nestren.chatgl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.nestren.chatgl.listener.ChatListener;

import java.util.ArrayList;
import java.util.List;

public final class ChatGlPlugin extends JavaPlugin {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private double localRadius;
    private String globalPrefix;
    private String localFormat;
    private String globalFormat;
    private String noListenersMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        getLogger().info("ChatGL запущен.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        this.localRadius = getConfig().getDouble("chat.local-radius", 50.0D);
        this.globalPrefix = getConfig().getString("chat.global-prefix", "!");
        this.localFormat = getConfig().getString("chat.formats.local", "<gray>[L]</gray> <white>%player%</white>: <yellow>%message%</yellow>");
        this.globalFormat = getConfig().getString("chat.formats.global", "<gold>[G]</gold> <white>%player%</white>: <white>%message%</white>");
        this.noListenersMessage = getConfig().getString("chat.messages.no-listeners", "<red>Вас никто не услышал, нет игроков в радиусе %radius%м.</red>");
    }

    public void handleChatMessage(Player sender, String rawMessage) {
        if (rawMessage.startsWith(globalPrefix)) {
            String globalMessage = rawMessage.substring(globalPrefix.length()).trim();
            if (!globalMessage.isEmpty()) {
                broadcastGlobal(sender, globalMessage);
            }
            return;
        }

        broadcastLocal(sender, rawMessage);
    }

    private void broadcastGlobal(Player sender, String message) {
        Component formatted = format(globalFormat, sender.getName(), message);
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(formatted));
    }

    private void broadcastLocal(Player sender, String message) {
        Location origin = sender.getLocation();
        double radiusSquared = localRadius * localRadius;

        List<Player> recipients = new ArrayList<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.getWorld().equals(sender.getWorld())) {
                continue;
            }
            if (target.getLocation().distanceSquared(origin) <= radiusSquared) {
                recipients.add(target);
            }
        }

        Component formatted = format(localFormat, sender.getName(), message);
        recipients.forEach(player -> player.sendMessage(formatted));

        boolean heardByOthers = recipients.stream().anyMatch(player -> !player.getUniqueId().equals(sender.getUniqueId()));
        if (!heardByOthers) {
            sender.sendMessage(MINI_MESSAGE.deserialize(noListenersMessage.replace("%radius%", String.valueOf((int) localRadius))));
        }
    }

    private Component format(String template, String playerName, String message) {
        return MINI_MESSAGE.deserialize(template
                .replace("%player%", playerName)
                .replace("%message%", message));
    }
}
