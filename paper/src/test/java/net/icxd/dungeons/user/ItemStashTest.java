package net.icxd.dungeons.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** The stash's messages as recorded (research critic.md 3.4, R1 02:06.8 and the reminders a minute apart). */
class ItemStashTest {
    private static String text(Component component) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }

    @Test
    void stashedMessage() {
        Component message = ItemStash.stashedMessage();
        assertEquals("&eOne or more items didn't fit in your inventory and were added to your item stash! &6Click here &eto pick them up!",
                text(message));
        assertEquals(ClickEvent.runCommand("/pickupstash"), message.clickEvent());
    }

    @Test
    void reminder() {
        List<Component> lines = ItemStash.reminder(7);
        assertEquals(4, lines.size());
        assertEquals(" ", text(lines.get(0)));
        assertEquals("&f                     &7You have &a7 &7items stashed away!", text(lines.get(1)));
        // As recorded, "&6&l>>> &6&lCLICK HERE": the second &6&l changes nothing, so it's not written back out.
        assertEquals("&f                &6&l>>> CLICK HERE&e to pick them up! &6&l<<<", text(lines.get(2)));
        assertEquals("  ", text(lines.get(3)));
        assertEquals(ClickEvent.runCommand("/viewstash item"), lines.get(1).clickEvent());
        assertEquals(ClickEvent.runCommand("/viewstash item"), lines.get(2).clickEvent());
    }

    @Test
    void anEmptyStash() {
        assertTrue(ItemStash.items(new Document()).isEmpty());
        assertEquals(0, ItemStash.count(List.of()));
    }
}
