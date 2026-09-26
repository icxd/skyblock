package net.icxd.dungeons.proxy;

import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Messages in Hypixel's style, written with section-sign colour codes like the Paper plugin's. */
public final class Chat {
    /** The blue rule above and below party messages. */
    public static final String RULE = "§9§m-----------------------------------------------------";

    private Chat() {
    }

    public static Component text(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }

    public static void send(CommandSource to, String legacy) {
        to.sendMessage(text(legacy));
    }

    /** Between two rules, one line each. */
    public static Component framed(Component... lines) {
        Component out = text(RULE);
        for (Component line : lines) out = out.append(Component.newline()).append(line);
        return out.append(Component.newline()).append(text(RULE));
    }

    public static Component framed(String... lines) {
        Component[] components = new Component[lines.length];
        for (int i = 0; i < lines.length; i++) components[i] = text(lines[i]);
        return framed(components);
    }
}
