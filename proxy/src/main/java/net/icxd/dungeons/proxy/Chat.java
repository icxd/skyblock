package net.icxd.dungeons.proxy;

import java.util.ArrayList;
import java.util.List;

import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Messages in Hypixel's style, written with section-sign colour codes like the Paper plugin's. */
public final class Chat {
    /** The blue rule above and below party messages. */
    public static final String RULE = "§9§m-----------------------------------------------------";
    /** The shorter one around "X entered The Catacombs, Entrance!". */
    public static final String SHORT_RULE = "§9§m-----------------------------";

    private Chat() {
    }

    public static Component text(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }

    public static void send(CommandSource to, String legacy) {
        to.sendMessage(text(legacy));
    }

    /**
     * Parts side by side. Appending to a component makes the new part its child, and children take
     * on their parent's style unless they set their own: the rule's strikethrough ran through every
     * line after it that way. These are siblings under an unstyled root instead.
     */
    public static Component join(Component... parts) {
        return Component.textOfChildren(parts);
    }

    /** Between two rules, one line each. */
    public static Component framed(Component... lines) {
        return between(RULE, lines);
    }

    public static Component between(String rule, Component... lines) {
        List<Component> all = new ArrayList<>(List.of(lines));
        all.add(0, text(rule));
        all.add(text(rule));
        return Component.join(JoinConfiguration.newlines(), all);
    }

    public static Component framed(String... lines) {
        Component[] components = new Component[lines.length];
        for (int i = 0; i < lines.length; i++) components[i] = text(lines[i]);
        return framed(components);
    }
}
