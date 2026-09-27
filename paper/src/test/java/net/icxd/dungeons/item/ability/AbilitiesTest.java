package net.icxd.dungeons.item.ability;

import net.icxd.dungeons.item.ability.abilities.InstantlyShoots;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.data.ItemData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which of an item's blocks a click uses, and what it costs (made-up abilities). */
class AbilitiesTest {
    private static final Predicate<String> HANDLED = Set.of("Test Left", "Test Right", "Test Sneak Right", "Test Other Right",
            "Test Passive")::contains;

    private static ItemBlock ability(String name, String activation) {
        return new ItemBlock("ABILITY", name, "&6Ability: " + name, activation, List.of(), 0, 0, 0, 0, 0, 0, 0);
    }

    private static final ItemBlock LEFT = ability("Test Left", "LEFT_CLICK");
    private static final ItemBlock RIGHT = ability("Test Right", "RIGHT_CLICK");
    private static final ItemBlock SNEAK_RIGHT = ability("Test Sneak Right", "SHIFT_RIGHT_CLICK");
    private static final ItemBlock SHORTBOW = new ItemBlock("SHORTBOW", "Instantly shoots!", null, null, List.of(), 0, 0, 0, 0, 0, 0, 0);

    private static ItemBlock click(List<ItemBlock> blocks, boolean right, boolean sneaking) {
        return Abilities.forClick(blocks, right, sneaking, HANDLED);
    }

    @Test
    void theClickItsFor() {
        List<ItemBlock> blocks = List.of(LEFT, RIGHT);
        assertSame(LEFT, click(blocks, false, false));
        assertSame(RIGHT, click(blocks, true, false));
        // Without a sneak ability for the click, sneaking changes nothing.
        assertSame(RIGHT, click(blocks, true, true));
        assertSame(LEFT, click(blocks, false, true));
    }

    /** Sneaking, a sneak ability for the click takes it; not sneaking, only the plain one. */
    @Test
    void sneaking() {
        List<ItemBlock> blocks = List.of(RIGHT, SNEAK_RIGHT, LEFT);
        assertSame(SNEAK_RIGHT, click(blocks, true, true));
        assertSame(RIGHT, click(blocks, true, false));
        // A sneak ability for a right click leaves left clicks as they are.
        assertSame(LEFT, click(blocks, false, true));
        assertNull(click(List.of(SNEAK_RIGHT), true, false));
        // A sneak ability nothing handles still keeps the plain one from firing while sneaking.
        assertNull(click(List.of(RIGHT, ability("Test Unhandled", "SHIFT_RIGHT_CLICK")), true, true));
    }

    /** The first one for the click that something handles; passive, unknown and bonus blocks never. */
    @Test
    void firstHandled() {
        ItemBlock unhandled = ability("Test Unhandled", "RIGHT_CLICK");
        ItemBlock other = ability("Test Other Right", "RIGHT_CLICK");
        assertSame(other, click(List.of(unhandled, other, RIGHT), true, false));
        ItemBlock bonus = new ItemBlock("FULL_SET", "Test Right", "&6Full Set Bonus: Test", "RIGHT_CLICK", List.of(), 0, 0, 0, 0, 0, 0, 4);
        assertNull(click(List.of(ability("Test Passive", "PASSIVE"), ability("Test Right", null), bonus), true, false));
        assertNull(click(List.of(), false, false));
    }

    /** A shortbow shoots on either click, unless an ability for the click comes first. */
    @Test
    void shortbow() {
        ItemBlock unhandled = ability("Test Unhandled", "LEFT_CLICK");
        assertSame(SHORTBOW, click(List.of(unhandled, SHORTBOW), false, false));
        assertSame(SHORTBOW, click(List.of(unhandled, SHORTBOW), true, true));
        assertSame(LEFT, click(List.of(SHORTBOW, LEFT), false, false));
        assertSame(SHORTBOW, click(List.of(SHORTBOW, LEFT), true, false));
    }

    @Test
    void manaCost() throws IOException {
        List<ItemBlock> blocks = ItemData.load(new StringReader("""
                {"format":1,"items":{"TEST_ORB":{"abilities":[{"activation":"RIGHT_CLICK","kind":"ABILITY","mana":45,"name":"Test Flat"},\
                {"activation":"RIGHT_CLICK","kind":"ABILITY","mana_percent":10,"name":"Test Share"},\
                {"activation":"RIGHT_CLICK","kind":"ABILITY","mana":20,"mana_percent":50,"name":"Test Both"}],\
                "material":"STONE","name":"Test Orb"}}}""")).items().get("TEST_ORB").blocks();
        assertEquals(45, Abilities.manaCost(blocks.get(0), 500));
        assertEquals(50, Abilities.manaCost(blocks.get(1), 500));
        // Rounded to the nearest: 53.1 is 53, 53.5 is 54.
        assertEquals(53, Abilities.manaCost(blocks.get(1), 531));
        assertEquals(54, Abilities.manaCost(blocks.get(1), 535));
        assertEquals(20 + 250, Abilities.manaCost(blocks.get(2), 500));
    }

    /** The Terminator shoots 3 arrows at once, whatever the case of its id in the data; other shortbows 1. */
    @Test
    void arrows() throws IOException {
        Map<String, DataItem> bows = ItemData.load(new StringReader("{\"format\":1,\"items\":{"
                + "\"TERMINATOR\":{\"material\":\"BOW\",\"name\":\"Test Bow\"},\"TEST_BOW\":{\"material\":\"BOW\",\"name\":\"Test Bow\"}}}")).items();
        assertEquals(3, InstantlyShoots.arrows(bows.get("TERMINATOR")));
        assertEquals(1, InstantlyShoots.arrows(bows.get("TEST_BOW")));
        DataItem lowerCase = ItemData.load(new StringReader(
                "{\"format\":1,\"items\":{\"terminator\":{\"material\":\"BOW\",\"name\":\"Test Bow\"}}}")).items().get("terminator");
        assertEquals(3, InstantlyShoots.arrows(lowerCase));
    }

    /** "This ability is on cooldown for 17s." with 16.9 seconds left. */
    @Test
    void cooldownSeconds() {
        assertEquals(17, Abilities.cooldownSeconds(16_900));
        assertEquals(10, Abilities.cooldownSeconds(10_000));
        assertEquals(1, Abilities.cooldownSeconds(1));
        assertEquals(0, Abilities.cooldownSeconds(-5));
    }

    @Test
    void handlers() {
        assertNotNull(Abilities.get("Instant Transmission"));
        assertNull(Abilities.get("Test Unhandled"));
        assertNull(Abilities.get(null));
        assertSame(Abilities.handler(SHORTBOW), Abilities.handler(SHORTBOW));
        assertNotNull(Abilities.handler(SHORTBOW));
        assertNull(Abilities.handler(ability("Test Unhandled", "RIGHT_CLICK")));
    }

    /**
     * Too little Vitality stops a cast unless its handler says its Vitality part is optional (Wither
     * Impact without its shield); a handler that doesn't care whether it was paid does its one use.
     */
    @Test
    void vitalityPart() {
        int[] uses = {0};
        AbilityHandler plain = (player, item, tag, block) -> uses[0]++;
        assertFalse(plain.vitalityOptional());
        plain.use(null, null, null, RIGHT, true);
        plain.use(null, null, null, RIGHT, false);
        assertEquals(2, uses[0]);

        boolean[] shield = {false};
        AbilityHandler optional = new AbilityHandler() {
            @Override
            public void use(org.bukkit.entity.Player player, net.icxd.dungeons.item.SkyBlockItem item,
                            net.icxd.dungeons.item.nbt.NBTTagCompound tag, ItemBlock block) {
                use(player, item, tag, block, true);
            }

            @Override
            public void use(org.bukkit.entity.Player player, net.icxd.dungeons.item.SkyBlockItem item,
                            net.icxd.dungeons.item.nbt.NBTTagCompound tag, ItemBlock block, boolean vitalityPaid) {
                shield[0] = vitalityPaid;
            }

            @Override
            public boolean vitalityOptional() {
                return true;
            }
        };
        assertTrue(optional.vitalityOptional());
        optional.use(null, null, null, RIGHT, false);
        assertFalse(shield[0]);
        optional.use(null, null, null, RIGHT, true);
        assertTrue(shield[0]);
    }
}
