package net.icxd.dungeons.item.ability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** How abilities are used besides a plain click, and what changes what they cost (made-up abilities). */
class ActivationsTest {
    private static final Predicate<String> ALL = name -> true;
    /** The health cost factor the test's players have now (a stand-in for Berserker Armor's). */
    private static final double[] HEALTH_FACTOR = {1};

    static {
        // Registered once for the whole run: they change nothing unless a test asks.
        Abilities.addItemManaCostFactor((player, tag) -> tag.hasKey("test_wise") ? 1 - 0.1 * tag.getInt("test_wise") : 1);
        Abilities.addCooldownFactor((player, block) -> "Test Quick".equals(block.name()) ? 0.75 : 1);
        Abilities.addCooldownFactor((player, block) -> "Test Quick".equals(block.name()) ? 0.8 : 1);
        Abilities.addHealthCostFactor(player -> HEALTH_FACTOR[0]);
    }

    private static ItemBlock ability(String name, String activation) {
        return new ItemBlock("ABILITY", name, "&6Ability: " + name, activation, List.of(), 0, 0, 0, 0, 0, 0, 0);
    }

    /** Every activation items.json has is one the plugin knows, so none is dropped when items load. */
    @Test
    void everyActivationInTheData() throws IOException {
        String property = System.getProperty("items.file");
        Path items = property != null ? Path.of(property)
                : Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent()
                .resolveSibling("skyblock-dungeon-data/items/items.json");
        assumeTrue(Files.exists(items), "no " + items);
        Set<String> activations = new TreeSet<>();
        try (Reader reader = Files.newBufferedReader(items, StandardCharsets.UTF_8)) {
            JsonObject all = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("items");
            for (var item : all.entrySet()) {
                JsonElement abilities = item.getValue().getAsJsonObject().get("abilities");
                if (abilities == null || !abilities.isJsonArray()) continue;
                for (JsonElement block : abilities.getAsJsonArray()) {
                    JsonElement activation = block.getAsJsonObject().get("activation");
                    if (activation != null) activations.add(activation.getAsString());
                }
            }
        }
        assertTrue(activations.containsAll(Set.of("SNEAK", "HOLD_RIGHT_CLICK", "ON_SHOOT", "LEFT_RIGHT_CLICK", "DIG", "CLICK")), activations.toString());
        for (String activation : activations) assertNotNull(AbilityActivation.of(activation), activation);
    }

    /** The loader keeps them now: a SNEAK ability's activation is its block's. */
    @Test
    void loaded() throws IOException {
        List<ItemBlock> blocks = ItemData.load(new StringReader("""
                {"format":1,"items":{"TEST_HELMET":{"abilities":[{"activation":"SNEAK","kind":"ABILITY","mana":20,"name":"Test Burst"},\
                {"activation":"ON_SHOOT","kind":"ABILITY","name":"Test Infusion"},{"activation":"TEST_CLICK","kind":"ABILITY","name":"Test Odd"}],\
                "material":"STONE","name":"Test Helmet"}}}""")).items().get("TEST_HELMET").blocks();
        assertEquals("SNEAK", blocks.get(0).activation());
        assertEquals("ON_SHOOT", blocks.get(1).activation());
        assertNull(blocks.get(2).activation());
        assertSame(AbilityActivation.SNEAK, AbilityActivation.of("SNEAK"));
        assertNull(AbilityActivation.of("TEST_CLICK"));
        assertNull(AbilityActivation.of(null));
    }

    /** LEFT_RIGHT_CLICK and CLICK are either click's, HOLD_RIGHT_CLICK a right click's; SNEAK, ON_SHOOT and DIG no click's. */
    @Test
    void clicks() {
        ItemBlock both = ability("Test Both", "LEFT_RIGHT_CLICK");
        ItemBlock any = ability("Test Any", "CLICK");
        ItemBlock hold = ability("Test Hold", "HOLD_RIGHT_CLICK");
        assertSame(both, Abilities.forClick(List.of(both), true, false, ALL));
        assertSame(both, Abilities.forClick(List.of(both), false, false, ALL));
        assertSame(any, Abilities.forClick(List.of(any), false, true, ALL));
        assertSame(hold, Abilities.forClick(List.of(hold), true, false, ALL));
        assertNull(Abilities.forClick(List.of(hold), false, false, ALL));
        for (String activation : new String[] {"SNEAK", "ON_SHOOT", "DIG", "PASSIVE"}) {
            assertNull(Abilities.forClick(List.of(ability("Test " + activation, activation)), true, false, ALL), activation);
            assertNull(Abilities.forClick(List.of(ability("Test " + activation, activation)), false, true, ALL), activation);
        }
        // A sneak ability for the click still comes first while sneaking.
        ItemBlock shift = ability("Test Shift", "SHIFT_RIGHT_CLICK");
        assertSame(shift, Abilities.forClick(List.of(both, shift), true, true, ALL));
        assertNull(Abilities.forClick(List.of(both), true, false, name -> false));
    }

    /** An item's blocks with an activation, handled ones only, in order. */
    @Test
    void withActivation() {
        ItemBlock first = ability("Test Missiles", "SNEAK");
        ItemBlock second = ability("Test Pound", "SNEAK");
        ItemBlock unhandled = ability("Test Unhandled", "SNEAK");
        ItemBlock bonus = new ItemBlock("FULL_SET", "Test Set", "&6Full Set Bonus: Test", "SNEAK", List.of(), 0, 0, 0, 0, 0, 0, 4);
        List<ItemBlock> blocks = List.of(first, ability("Test Right", "RIGHT_CLICK"), unhandled, second, bonus);
        assertEquals(List.of(first, second), Abilities.withActivation(blocks, AbilityActivation.SNEAK, name -> !name.equals("Test Unhandled")));
        assertTrue(Abilities.withActivation(blocks, AbilityActivation.DIG, ALL).isEmpty());
    }

    /** An item's own factor (Ultimate Wise's 10% a level) multiplies with the rest. */
    @Test
    void itemManaCostFactor() {
        ItemBlock block = new ItemBlock("ABILITY", "Test Cost", null, "RIGHT_CLICK", List.of(), 50, 0, 0, 0, 0, 0, 0);
        NBTTagCompound wise = new NBTTagCompound();
        wise.setInt("test_wise", 5);
        assertEquals(50, Abilities.manaCost(block, 500, null, new NBTTagCompound()));
        assertEquals(25, Abilities.manaCost(block, 500, null, wise));
        assertEquals(50, Abilities.manaCost(block, 500, null, null));
    }

    /** Cooldown factors multiply; none leaves the block's seconds. */
    @Test
    void cooldowns() {
        ItemBlock quick = new ItemBlock("ABILITY", "Test Quick", null, "RIGHT_CLICK", List.of(), 0, 0, 10, 0, 0, 0, 0);
        ItemBlock slow = new ItemBlock("ABILITY", "Test Slow", null, "RIGHT_CLICK", List.of(), 0, 0, 10, 0, 0, 0, 0);
        assertEquals(6_000, Abilities.cooldownMillis(quick, null));
        assertEquals(10_000, Abilities.cooldownMillis(slow, null));
        assertEquals(1_500, Abilities.cooldownMillis(2, 0.75));
        assertEquals(0, Abilities.cooldownMillis(-2, 1));
        assertEquals(0, Abilities.cooldownMillis(2, -1));
    }

    /**
     * A health cost is its handler's (the block's, or a share of max health), times what lowers it; it can't
     * take the last of their health.
     */
    @Test
    void healthCosts() {
        ItemBlock block = new ItemBlock("ABILITY", "Test Blood", null, "RIGHT_CLICK", List.of(), 0, 0, 0, 0, 100, 0, 0);
        AbilityHandler plain = (player, item, tag, b) -> { };
        AbilityHandler share = new AbilityHandler() {
            @Override
            public void use(org.bukkit.entity.Player player, net.icxd.dungeons.item.SkyBlockItem item, NBTTagCompound tag, ItemBlock b) {
            }

            @Override
            public double healthCost(org.bukkit.entity.Player player, net.icxd.dungeons.item.SkyBlockItem item, NBTTagCompound tag, ItemBlock b) {
                return 0.1 * 5_000;
            }
        };
        assertEquals(100, Abilities.healthCost(plain, null, null, null, block), 1e-9);
        assertEquals(500, Abilities.healthCost(share, null, null, null, block), 1e-9);
        assertEquals(100, Abilities.healthCost(null, null, null, null, block), 1e-9);
        HEALTH_FACTOR[0] = 0.4;
        try {
            assertEquals(40, Abilities.healthCost(plain, null, null, null, block), 1e-9);
        } finally {
            HEALTH_FACTOR[0] = 1;
        }
        assertTrue(Abilities.canPayHealth(101, 100));
        assertFalse(Abilities.canPayHealth(100, 100));
        assertTrue(Abilities.canPayHealth(1, 0));
    }
}
