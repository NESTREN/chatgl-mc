package ru.nestren.chatgl.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import ru.nestren.chatgl.ChatGlPlugin;

public final class ChatListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final ChatGlPlugin plugin;

    public ChatListener(ChatGlPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!(event.getPlayer() instanceof Player sender)) {
            return;
        }

        String message = PLAIN_TEXT.serialize(event.message());
        event.setCancelled(true);

        Bukkit.getScheduler().runTask(plugin, () -> plugin.handleChatMessage(sender, message));
    }
}
