package net.icxd.dungeons.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

class ChatTest {
    /** Each piece of text with the style it's shown in, its parents' included. */
    private static Map<String, Style> shown(Component component) {
        Map<String, Style> out = new LinkedHashMap<>();
        walk(component, Style.empty(), out);
        return out;
    }

    private static void walk(Component component, Style inherited, Map<String, Style> out) {
        Style style = component.style().merge(inherited, Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
        if (component instanceof TextComponent text && !text.content().isBlank()) out.put(text.content().trim(), style);
        for (Component child : component.children()) walk(child, style, out);
    }

    @Test
    void onlyTheRulesAreStruckThrough() {
        Map<String, Style> shown = shown(Chat.framed("§eFirst line", "§bSecond §7line"));
        for (Map.Entry<String, Style> piece : shown.entrySet()) {
            boolean rule = piece.getKey().startsWith("---");
            assertEquals(rule, piece.getValue().decoration(TextDecoration.STRIKETHROUGH) == TextDecoration.State.TRUE, piece.getKey());
        }
        assertTrue(shown.containsKey("First line"));
        assertEquals(NamedTextColor.GRAY, shown.get("line").color());
    }

    @Test
    void joinedPartsKeepTheirOwnStyle() {
        Map<String, Style> shown = shown(Chat.join(Chat.text("§9Party §8> §7Bob§f: "), Component.text("hi §cthere", NamedTextColor.WHITE)));
        assertEquals(NamedTextColor.WHITE, shown.get("hi §cthere").color());
        assertFalse(shown.get("Party").hasDecoration(TextDecoration.STRIKETHROUGH));
    }
}
