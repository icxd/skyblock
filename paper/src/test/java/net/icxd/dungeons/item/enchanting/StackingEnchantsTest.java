package net.icxd.dungeons.item.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mining.MiningTools;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Stacking enchantments' tiers and counts, and the numbers the drops, XP and mining effects read from enchantments'
 * text (a percent, "Gain" stats, Efficiency's Mining Speed).
 */
class StackingEnchantsTest {
    /** A made-up stacking enchantment (its numbers aren't Hypixel's): three tiers, at 100 and 1.5k. */
    private static final String TABLE = "{\"format\":1,\"order\":[\"compact\"],\"enchantments\":{\"compact\":{\"name\":\"Compact\","
            + "\"hypixel\":\"compact\",\"ultimate\":false,\"min\":1,\"max\":3,\"table\":null,\"xp\":[1,1,1],\"enchanting\":0,"
            + "\"applies\":[\"Pickaxe\"],\"conflicts\":[],\"levels\":{"
            + "\"1\":{\"text\":\"&7Gain &3+2☯ Mining Wisdom &7and a &a0.1% &7chance to drop an enchanted item.\",\"rarity\":\"COMMON\","
            + "\"tier_up\":\"&8100 blocks to tier up!\"},"
            + "\"2\":{\"text\":\"&7Gain &3+4☯ Mining Wisdom &7and a &a0.2% &7chance to drop an enchanted item.\",\"rarity\":\"COMMON\","
            + "\"tier_up\":\"&81.5k blocks to tier up!\"},"
            + "\"3\":{\"text\":\"&7Gain &3+6☯ Mining Wisdom &7and a &a0.3% &7chance to drop an enchanted item.\",\"rarity\":\"COMMON\"}}}}}";

    private static EnchantmentType useTable() {
        List<String> problems = new ArrayList<>();
        EnchantmentData.use(EnchantmentData.parse(JsonParser.parseString(TABLE).getAsJsonObject(), problems));
        assertEquals(List.of(), problems);
        return EnchantmentType.getByNamespace("compact");
    }

    @AfterEach
    void reset() {
        FakeEnchantments.reset();
    }

    /** "&8100 blocks to tier up!" is 100, "1.5k" 1,500, "2.5m" 2,500,000; nothing else is a tier. */
    @Test
    void nextTiers() {
        assertEquals(100, StackingEnchants.nextTier("&8100 blocks to tier up!"), 1e-9);
        assertEquals(1_500, StackingEnchants.nextTier("&81.5k blocks to tier up!"), 1e-9);
        assertEquals(2_500_000, StackingEnchants.nextTier("&82.5m Combat XP to tier up!"), 1e-9);
        assertEquals(2, StackingEnchants.nextTier("&82 S runs to tier up!"), 1e-9);
        assertEquals(-1, StackingEnchants.nextTier(null), 1e-9);
        assertEquals(-1, StackingEnchants.nextTier("&8Maxed!"), 1e-9);
        assertEquals("compact_blocks", StackingEnchants.counter("compact"));
        assertEquals("champion_combat_xp", StackingEnchants.counter("champion"));
        assertNull(StackingEnchants.counter("sharpness"));
    }

    /** A count takes it past each tier it has reached, at most to its last; never lower than it is. */
    @Test
    void tiers() {
        EnchantmentType compact = useTable();
        assertEquals(1, StackingEnchants.tier(compact, 1, 99));
        assertEquals(2, StackingEnchants.tier(compact, 1, 100));
        assertEquals(3, StackingEnchants.tier(compact, 1, 1_500));
        assertEquals(3, StackingEnchants.tier(compact, 1, 1_000_000));
        assertEquals(2, StackingEnchants.tier(compact, 2, 150));
        assertEquals(3, StackingEnchants.tier(compact, 3, 0));
        assertEquals("&8100 blocks to tier up!", compact.getTierUp(1));
        assertNull(compact.getTierUp(3));
    }

    /** Live lore: "&9Compact VIII &8202,861" while it has a count and a tier to go; nothing at its last tier or with no count. */
    @Test
    void countAfterTheName() {
        EnchantmentType compact = useTable();
        NBTTagCompound tag = new NBTTagCompound();
        assertEquals("", StackingEnchants.countSuffix(tag, new Enchantment(compact, 2)));
        tag.setDouble("compact_blocks", 202_861.7);
        assertEquals(" &8202,861", StackingEnchants.countSuffix(tag, new Enchantment(compact, 2)));
        assertEquals("", StackingEnchants.countSuffix(tag, new Enchantment(compact, 3)));
    }

    /** "&7Gain" stats count as "&7Grants" ones: Cultivating's two, and Compact's Mining Wisdom before its chance; not a stat "for 7s". */
    @Test
    void gainStats() {
        Stats compact = EnchantmentType.stats("&7Gain &3+8☯ Mining Wisdom &7and a &a0.44% &7chance to drop an enchanted item.");
        assertEquals(8, compact.get(Stat.MINING_WISDOM), 1e-9);
        assertEquals(new Stats().set(Stat.MINING_WISDOM, 8), compact);
        Stats cultivating = EnchantmentType.stats("&7Gain &3+3☯ Farming Wisdom &7and &6+6☘ Farming Fortune&7.");
        assertEquals(3, cultivating.get(Stat.FARMING_WISDOM), 1e-9);
        assertEquals(6, cultivating.get(Stat.FARMING_FORTUNE), 1e-9);
        assertEquals(new Stats(), EnchantmentType.stats("&7Gain &a+6❈ Defense &7for &a7s &7on the first hit from an enemy."));
        assertEquals(new Stats(), EnchantmentType.stats("&7Gain &a3% &7extra Combat XP."));
    }

    /**
     * Of two stats "while" or "when" something, the first counts, as live lore has Lapidary's Gemstone Fortune and
     * Expertise's Sea Creature Chance; one stat that only sometimes counts gives nothing.
     */
    @Test
    void firstOfTwo() {
        assertEquals(new Stats().set(Stat.GEMSTONE_FORTUNE, 30),
                EnchantmentType.stats("&7Grants &6+30☘ Gemstone Fortune &7and &6+60⸕ Mining Speed &7while mining Gemstones."));
        assertEquals(new Stats().set(Stat.SEA_CREATURE_CHANCE, 1.8),
                EnchantmentType.stats("&7Grants &3+1.8α Sea Creature Chance &7and &3+6☯ Fishing Wisdom &7when killing Sea Creatures."));
        assertEquals(new Stats(), EnchantmentType.stats("&7Grants &6+20☘ Farming Fortune &7when vacuuming &2 Pests&7."));
        assertEquals(new Stats(), EnchantmentType.stats("&7Grants &c+3❣ Health Regen &7while out of combat."));
    }

    /** On an item, an enchantment's stats are its own ({@link EnchantmentType#getStats(int)}'s) but for Efficiency's Mining Speed. */
    @Test
    void onAnItem() {
        EnchantmentType compact = useTable();
        assertEquals(compact.getStats(2), compact.getStats(null, 2));
        assertEquals(new Stats().set(Stat.MINING_WISDOM, 4), compact.getStats(null, 2));
    }

    /** Efficiency's text on a mining tool off the Hub is a stat the text reads: +110 Mining Speed at V. */
    @Test
    void efficiencyReads() {
        assertEquals("&7Grants &a+110 &6⸕ Mining Speed&7.", MiningTools.efficiencyText(5));
        assertEquals(new Stats().set(Stat.MINING_SPEED, 110), EnchantmentType.stats(MiningTools.efficiencyText(5)));
        assertEquals(new Stats().set(Stat.MINING_SPEED, 210), EnchantmentType.stats(MiningTools.efficiencyText(10)));
    }

    /** The first green percent: Looting's "by &a15%", Experience's "a &a12.5% chance", Compact's past its Wisdom. */
    @Test
    void percents() {
        assertEquals(15, EnchantmentType.percent("&7Increases the chance of a Monster dropping an item by &a15%&7."), 1e-9);
        assertEquals(35, EnchantmentType.percent("&7Increases the chance for Monsters to drop their armor by &a35%"), 1e-9);
        assertEquals(12.5, EnchantmentType.percent("&7Grants a &a12.5% &7chance for mobs and ores to drop double experience."), 1e-9);
        assertEquals(0.25, EnchantmentType.percent("&7Gain &3+1☯ Mining Wisdom &7and a &a0.25% &7chance to drop an enchanted item."), 1e-9);
        assertEquals(0, EnchantmentType.percent("&7Increases how quickly your tool breaks blocks."), 1e-9);
        assertEquals(0, EnchantmentType.percent(null), 1e-9);
    }

    // The private table as it is (skipped without it). Its numbers stay out of this repository: these check that each
    // text reads, not what it says.

    /** Every level's text gives what the effects read: Compact's Wisdom and ladder, the drop and XP percents. */
    @Test
    void theRealTexts() {
        EnchantmentData.use(FakeEnchantments.real());
        EnchantmentType compact = EnchantmentType.getByNamespace("compact");
        double wisdom = 0;
        double chance = 0;
        double ladder = 0;
        for (int level = 1; level <= compact.getMaxLevel(); level++) {
            Stats stats = compact.getStats(level);
            assertEquals(new Stats().set(Stat.MINING_WISDOM, stats.get(Stat.MINING_WISDOM)), stats, "Compact " + level + " grants only Mining Wisdom");
            assertTrue(stats.get(Stat.MINING_WISDOM) > wisdom, "Compact " + level);
            assertTrue(compact.percent(level) > chance, "Compact " + level);
            wisdom = stats.get(Stat.MINING_WISDOM);
            chance = compact.percent(level);
            if (level == compact.getMaxLevel()) break;
            assertTrue(StackingEnchants.nextTier(compact.getTierUp(level)) > ladder, "Compact " + level);
            ladder = StackingEnchants.nextTier(compact.getTierUp(level));
        }
        assertNull(compact.getTierUp(compact.getMaxLevel()));
        assertEquals(compact.getMaxLevel(), StackingEnchants.tier(compact, 1, ladder));
        assertEquals(compact.getMaxLevel() - 1, StackingEnchants.tier(compact, 1, ladder - 1));
        for (String id : List.of("looting", "chance", "experience", "luck")) {
            EnchantmentType type = EnchantmentType.getByNamespace(id);
            double before = 0;
            for (int level = 1; level <= type.getMaxLevel(); level++) {
                assertTrue(type.percent(level) > before, id + " " + level);
                before = type.percent(level);
            }
        }
    }
}
