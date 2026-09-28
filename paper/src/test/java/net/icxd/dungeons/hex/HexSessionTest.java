package net.icxd.dungeons.hex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.profile.SandboxDrops;

/** The item's life in the Hex: saved with every save and nowhere twice, dropped when they die, and the chat lines. */
class HexSessionTest {
    @Test
    void eachSaveReplacesTheLastOnesCopy() {
        // The profile's overflow had two things; the first save puts the item after them.
        String a = "a";
        String b = "b";
        String first = new String("sword");
        List<Object> saved = HexSession.overflow(List.of(a, b), null, first);
        assertEquals(List.of(a, b, first), saved);
        // The next save puts its copy in place of that one, which it tells apart from an equal one of another's.
        String equal = new String("sword");
        String second = new String("sword");
        List<Object> next = HexSession.overflow(List.of(a, equal, first, b), first, second);
        assertEquals(4, next.size());
        assertSame(equal, next.get(1));
        assertSame(second, next.get(3));
        assertFalse(next.stream().anyMatch(o -> o == first));
        // The item left the Hex (they took it out, or it went back to them): its copy goes.
        assertEquals(List.of(a, b), HexSession.overflow(List.of(a, second, b), second, null));
        // Nothing saved before, nothing now: as it was.
        assertEquals(List.of(a, b), HexSession.overflow(List.of(a, b), null, null));
    }

    @Test
    void aDeathDropsTheItemWhenTheInventoryIsEmptied() {
        assertTrue(HexSession.dropped(false, false));
        // keepInventory: the menu gives it back as it closes, into the inventory that's kept.
        assertFalse(HexSession.dropped(false, true));
        // Called off (a death in a dungeon run): the menu stays open with it.
        assertFalse(HexSession.dropped(true, false));
        assertFalse(HexSession.dropped(true, true));
    }

    @Test
    void theItemGoesInTheDropsBeforeSandboxDropsMarksThem() throws NoSuchMethodException {
        EventHandler dying = HexListener.class.getMethod("onDeath", PlayerDeathEvent.class).getAnnotation(EventHandler.class);
        EventHandler died = HexListener.class.getMethod("afterDeath", PlayerDeathEvent.class).getAnnotation(EventHandler.class);
        Method marks = SandboxDrops.class.getMethod("onDeath", PlayerDeathEvent.class);
        EventHandler marking = marks.getAnnotation(EventHandler.class);
        assertTrue(dying.priority().getSlot() < marking.priority().getSlot());
        assertEquals(EventPriority.MONITOR, died.priority());
        // Both hear a death that's been called off, to keep the item then.
        assertFalse(dying.ignoreCancelled());
        assertFalse(died.ignoreCancelled());
    }

    @Test
    void pullingAFastOne() {
        assertTrue(HexSession.fastOne(100, 100));
        assertFalse(HexSession.fastOne(100, 101));
        assertFalse(HexSession.fastOne(-1, 100));
    }

    @Test
    void applied() {
        // SkyHanni's test line, "You applied a Recombobulator 3000 to your Heroic Dreadlord Sword!".
        assertEquals("&aYou applied a &6Recombobulator 3000 &ato your &5Heroic Dreadlord Sword&a!",
                HexSession.appliedLine("&6Recombobulator 3000", "&5Heroic Dreadlord Sword"));
        assertEquals("&aYou applied an &5Enchanted Book &ato your &6Hyperion&a!", HexSession.appliedLine("&5Enchanted Book", "&6Hyperion"));
    }
}
