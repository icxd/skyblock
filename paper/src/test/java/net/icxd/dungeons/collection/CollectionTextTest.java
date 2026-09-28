package net.icxd.dungeons.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.collection.CollectionData.Reward;

/** How rewards and progress are written: the recorded lines (Blaze Rod, String, Bonzo), and what wraps. */
class CollectionTextTest {
    private static Reward reward(Reward.Type type, String name, String item, long amount) {
        return new Reward(type, item, name, null, amount, 0, null, null, null);
    }

    @Test
    void rewardLines() {
        assertEquals("&8+&b4 SkyBlock XP", CollectionText.line(reward(Reward.Type.SKYBLOCK_XP, null, null, 4)));
        assertEquals("&8+&310,000 &7Combat Experience",
                CollectionText.line(new Reward(Reward.Type.SKILL_XP, null, null, "COMBAT", 10_000, 0, null, null, null)));
        assertEquals("&9Fire Aspect &7Exp Discount &a(-25%)",
                CollectionText.line(new Reward(Reward.Type.EXP_DISCOUNT, null, "Fire Aspect", null, 0, 25, null, null, null)));
        assertEquals("&8+&79 &aQuiver &7Slots", CollectionText.line(reward(Reward.Type.SLOTS, "Quiver", null, 9)));
        assertEquals("&7[Lvl 1] &fBlaze &7Recipe", CollectionText.line(reward(Reward.Type.PET_RECIPE, "Blaze", null, 0)));
        assertEquals("&aQuiver", CollectionText.line(new Reward(Reward.Type.UNLOCK, null, null, null, 0, 0, null, "Quiver", null)));
        assertEquals("&a+1☘ Mining Fortune",
                CollectionText.line(new Reward(Reward.Type.STAT, null, "MINING_FORTUNE", null, 1, 0, null, "+1☘ Mining Fortune", null)));
        assertEquals("&dGold Essence &8x250", CollectionText.line(new Reward(Reward.Type.ESSENCE, null, null, null, 250, 0, "GOLD", null, null)));
        assertEquals("&dDiamond Essence &8x1000",
                CollectionText.line(new Reward(Reward.Type.ESSENCE, null, null, null, 1000, 0, "DIAMOND", null, null)));
        // An item the plugin doesn't have: white (a minion's blue), as the wiki's Collection UI colours them.
        assertEquals("&fAdrenaline Potion &7Recipe", CollectionText.line(reward(Reward.Type.RECIPE, "Adrenaline Potion", null, 0)));
        assertEquals("&9Blaze Minion &7Recipes", CollectionText.line(reward(Reward.Type.MINION_RECIPES, "Blaze", null, 0)));
    }

    /** Two spaces in; a line too wide goes on, two spaces in, in the colour it had (UNKNOWN: none recorded). */
    @Test
    void rewardLinesWrap() {
        CollectionData.Tier tier = new CollectionData.Tier(10, List.of(
                new Reward(Reward.Type.EXP_DISCOUNT, null, "Fire Protection", null, 0, 25, null, null, null),
                reward(Reward.Type.RECIPE, "Enchanted Fermented Spider Eye", null, 0),
                reward(Reward.Type.SKYBLOCK_XP, null, null, 4)));
        assertEquals(List.of("  &9Fire Protection &7Exp Discount &a(-25%)", "  &fEnchanted Fermented Spider Eye", "  &7Recipe",
                "  &8+&b4 SkyBlock XP"), CollectionText.rewardLines(tier, true));
        assertEquals(List.of("  &9Fire Protection &7Exp Discount &a(-25%)", "  &fEnchanted Fermented Spider Eye", "  &7Recipe"),
                CollectionText.rewardLines(tier, false));
        assertEquals("Reward:", CollectionText.rewardsWord(List.of("  &9Red Nose")));
        assertEquals("Rewards:", CollectionText.rewardsWord(List.of("  &9Red Nose", "  &8+&b15 SkyBlock XP")));
    }

    @Test
    void progress() {
        assertEquals("&e70&6%", CollectionText.progress(0.7));
        assertEquals("&a100%", CollectionText.progress(1));
        assertEquals("&e0.4&6%", CollectionText.progress(35 / 10_000.0));
        assertEquals("&e10.8&6%", CollectionText.progress(1_075 / 10_000.0));
        assertEquals("&a100&6%", CollectionText.unlocked(1));
        assertEquals("&e0&6%", CollectionText.unlocked(0));
        assertEquals("&f&l&m                         &r &e0&6/&e18", CollectionText.bar(0, 18));
        assertEquals("&2&l&m                         &r &e17&6/&e17", CollectionText.bar(17, 17));
        assertEquals("&2&l&m                         &r &e1,075&6/&e50", CollectionText.bar(1_075, 50));
    }

    /** The level-up message: SkyHanni's header and rule, the rest laid out as the skills' (UNKNOWN). */
    @Test
    void levelUp() {
        CollectionData.Collection blaze = CollectionsTest.data().collection("BLAZE_ROD");
        assertEquals(List.of(CollectionText.RULE, "  &6&lCOLLECTION LEVEL UP &eBlaze Rod &eI", "", "  &a&lREWARDS",
                "    &9Blaze Minion &7Recipes", "    &8+&b4 SkyBlock XP", CollectionText.RULE), CollectionText.levelUp(blaze, 1));
        assertEquals("  &6&lCOLLECTION LEVEL UP &eBlaze Rod &8II➜&eIII", CollectionText.levelUp(blaze, 3).get(1));
        assertEquals("&e&l" + "▬".repeat(64), CollectionText.RULE);
    }
}
