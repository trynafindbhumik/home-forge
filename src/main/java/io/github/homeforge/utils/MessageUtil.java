package io.github.homeforge.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {}

    public static Component colorize(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return LEGACY.deserialize(text);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    public static void send(CommandSender sender, String message, String... placeholders) {
        send(sender, replace(message, placeholders));
    }

    public static String replace(String text, String... pairs) {
        if (text == null) return "";
        for (int i = 0; i < pairs.length - 1; i += 2) {
            text = text.replace(pairs[i], pairs[i + 1]);
        }
        return text;
    }

    public static String strip(String text) {
        return LEGACY.serialize(colorize(text)).replaceAll("§.", "");
    }
}
