package net.icxd.dungeons.item.ability.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.instance.RunItems;
import net.icxd.dungeons.item.data.ItemBlock;

/** Dungeon ghosts' abilities: their rules. */
class GhostAbilitiesTest {
    private static final double EPS = 1e-9;

    private static ItemBlock block(String name, double cooldown, String... text) {
        return new ItemBlock("ABILITY", name, null, "RIGHT_CLICK", List.of(text), 0, 0, cooldown, 0, 0, 0, 0);
    }

    /** "200 HP worth of absorption", and "+10 Absorption from Ghost Absorption Potion" a Tank level: 700 at 50. */
    @Test
    void absorptionGrowsWithTheTankLevel() {
        assertEquals(200, GhostAbilities.absorption(200, 0), EPS);
        assertEquals(700, GhostAbilities.absorption(200, 50), EPS);
    }

    @Test
    void radiusFromTheText() {
        assertEquals(10, GhostAbilities.radius(block("Stun Potion", 20, "&7Throw a potion which temporarily stuns all",
                "&7monsters in a &a10 &7block radius.")), EPS);
        assertEquals(10, GhostAbilities.radius(block("Stun Potion", 20, "&7nothing")), EPS);
    }

    /** The Spirit Shortbow's Spirit Bomb has no cooldown of its own: the Spirit Sword's 10 s. */
    @Test
    void spiritBombAlwaysHasACooldown() {
        ItemBlock sword = block("Spirit Bomb", 10);
        assertSame(sword, GhostAbilities.withCooldown(sword));
        assertEquals(GhostAbilities.SPIRIT_BOMB_COOLDOWN, GhostAbilities.withCooldown(block("Spirit Bomb", 0)).cooldown(), EPS);
    }

    @Test
    void ghostItems() {
        assertTrue(RunItems.ghostItem("HAUNT_ABILITY"));
        assertTrue(RunItems.ghostItem("TANK_DUNGEON_ABILITY_2"));
        assertFalse(RunItems.ghostItem("SPIRIT_SWORD"));
    }
}
