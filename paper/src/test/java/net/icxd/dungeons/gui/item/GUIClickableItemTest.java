package net.icxd.dungeons.gui.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

class GUIClickableItemTest {
    /** A menu's buttons answer a left or right click, shift or not, and nothing else (a double click would be a second press). */
    @Test
    void buttonClicks() {
        Set<ClickType> pressed = EnumSet.noneOf(ClickType.class);
        for (ClickType click : ClickType.values()) {
            if (GUIClickableItem.pressed(click)) pressed.add(click);
        }
        assertEquals(EnumSet.of(ClickType.LEFT, ClickType.RIGHT, ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT), pressed);
    }
}
