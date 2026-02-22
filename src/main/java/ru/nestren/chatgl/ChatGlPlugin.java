package ru.nestren.chatgl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.nestren.chatgl.listener.ChatListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ChatGlPlugin extends JavaPlugin {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private double localRadius;
    private String globalPrefix;
    private String localFormat;
    private String globalFormat;
    private String noListenersMessage;
    private PlaceholderApiBridge placeholderApiBridge;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();

        this.placeholderApiBridge = PlaceholderApiBridge.create();
        if (placeholderApiBridge.isEnabled()) {
            getLogger().info("PlaceholderAPI найдена: поддержка сторонних плейсхолдеров включена.");
        } else {
            getLogger().info("PlaceholderAPI не найдена: используются только внутренние %player% и %message%.");
        }

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
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            Component formatted = format(globalFormat, sender, recipient, message);
            recipient.sendMessage(formatted);
        }
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

        for (Player recipient : recipients) {
            Component formatted = format(localFormat, sender, recipient, message);
            recipient.sendMessage(formatted);
        }

        boolean heardByOthers = recipients.stream().anyMatch(player -> !player.getUniqueId().equals(sender.getUniqueId()));
        if (!heardByOthers) {
            String text = noListenersMessage.replace("%radius%", String.valueOf((int) localRadius));
            sender.sendMessage(MINI_MESSAGE.deserialize(applyPlaceholders(sender, text)));
        }
    }

    private Component format(String template, Player sender, Player recipient, String message) {
        String text = template
                .replace("%player%", sender.getName())
                .replace("%message%", message);

        text = applyPlaceholders(recipient, text);
        return MINI_MESSAGE.deserialize(text);
    }

    private String applyPlaceholders(Player viewer, String text) {
        return placeholderApiBridge.apply(viewer, text);
    }

    private static final class PlaceholderApiBridge {
        private final Method setPlaceholders;

        private PlaceholderApiBridge(Method setPlaceholders) {
            this.setPlaceholders = setPlaceholders;
        }

        static PlaceholderApiBridge create() {
            try {
                Class<?> hook = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Method method = hook.getMethod("setPlaceholders", Player.class, String.class);
                return new PlaceholderApiBridge(method);
            } catch (ReflectiveOperationException ignored) {
                return new PlaceholderApiBridge(null);
            }
        }

        boolean isEnabled() {
            return setPlaceholders != null;
        }

        String apply(Player player, String text) {
            if (setPlaceholders == null) {
                return text;
            }

            Plugin plugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
            if (plugin == null || !plugin.isEnabled()) {
                return text;
            }

            try {
                Object resolved = setPlaceholders.invoke(null, player, text);
                return Objects.toString(resolved, text);
            } catch (ReflectiveOperationException ex) {
                return text;
            }
        }
    }
}
