package net.icxd.dungeons.utils;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTest {
    @Test
    void numbers() {
        assertEquals("1,800", Text.number(1800));
        assertEquals("0.05", Text.number(0.05));
        assertEquals("1,294.56", Text.number(1294.56));
        assertEquals("+48.5", Text.signed(48.5));
        assertEquals("-18", Text.signed(-18));
    }

    @Test
    void linesAreNotItalic() {
        var line = Text.line("&7Damage: &c+120");
        assertEquals("Damage: +120", PlainTextComponentSerializer.plainText().serialize(line));
        assertEquals(TextDecoration.State.FALSE, line.decoration(TextDecoration.ITALIC));
        assertEquals(NamedTextColor.GRAY, line.children().isEmpty() ? line.color() : line.children().get(0).color());
    }

    @Test
    void italicWhenItSaysSo() {
        assertEquals(TextDecoration.State.TRUE, Text.line("&8&oThat thing was too big").decoration(TextDecoration.ITALIC));
    }

    @Test
    void widthIgnoresCodes() {
        assertEquals(Text.width("Hi"), Text.width("&cHi"));
        assertEquals(6 + 2, Text.width("Hi"));
        assertEquals(Text.width("Hi") + 2, Text.width("&lHi"));
    }

    /** Hypixel's own breaks: Sharpness V and Ultimate Wise V on items (live auctions, the recordings). */
    @Test
    void wrapsLikeHypixel() {
        assertEquals(List.of("&7Increases melee damage dealt by &a30%&7."),
                Text.wrap("&7Increases melee damage dealt by &a30%&7.", Text.LORE_WIDTH));
        assertEquals(List.of("&7Reduces the ability mana cost of this", "&7item by &a50%&7."),
                Text.wrap("&7Reduces the ability mana cost of this item by &a50%&7.", Text.LORE_WIDTH));
        assertEquals(List.of("&7A slightly better version of", "&5Amethyst&7, but it could still use some", "&7work."),
                Text.wrap("&7A slightly better version of &5Amethyst&7, but it could still use some work.", 188));
    }
}
