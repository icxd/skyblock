package net.icxd.dungeons.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.menu.Icon;

/**
 * The collection menus against the recorded ones (the SkyBlock Menu tour, Banana's collections: Combat
 * Collections 00:42.9, Blaze Rod Collection 00:44.9, Blaze Rod I Rewards 00:46.9, String Collection 00:51.9,
 * String VIII Rewards 00:54.0, Boss Collections 00:58.3, Bonzo Collection 00:59.5, Bonzo I Rewards 01:01.0),
 * on the private data. Glyphs are the classic symbols.
 */
class CollectionMenusTest {
    @BeforeAll
    static void load() {
        PrivateData.load();
    }

    @AfterAll
    static void unload() {
        PrivateData.unload();
    }

    /** Banana's counts, as the menus showed them. */
    static Document banana() {
        Document counts = new Document("BLAZE_ROD", 35L).append("BONE", 17_153L).append("ENDER_PEARL", 7_316L).append("SULPHUR", 18L)
                .append("MAGMA_CREAM", 106L).append("ROTTEN_FLESH", 12_831L).append("SLIME_BALL", 823_679L).append("SPIDER_EYE", 662L)
                .append("STRING", 1_075L);
        Document bosses = new Document("CATACOMBS_1", 5L).append("CATACOMBS_2", 11L).append("CATACOMBS_3", 4L).append("CATACOMBS_4", 1L)
                .append("CATACOMBS_5", 19L).append("CATACOMBS_6", 10L);
        return new Document("mode", "NORMAL").append(Collections.COUNTS, counts).append(Collections.BOSS_COUNTS, bosses);
    }

    private static Icon icon(Map<Integer, MenuSlot> slots, int slot) {
        return slots.get(slot).icon();
    }

    private static void assertIcon(Map<Integer, MenuSlot> slots, int slot, Material material, String name, String... lore) {
        Icon icon = icon(slots, slot);
        assertEquals(material, icon.material(), "slot " + slot);
        assertEquals(name, icon.name(), "slot " + slot);
        assertEquals(List.of(lore), icon.lore(), "slot " + slot);
    }

    private static Set<Integer> glass(Map<Integer, MenuSlot> slots) {
        Set<Integer> glass = new HashSet<>();
        slots.forEach((slot, shown) -> {
            if (shown.filler()) glass.add(slot);
        });
        return glass;
    }

    private static Set<Integer> range(int from, int to, int... more) {
        Set<Integer> set = new HashSet<>();
        for (int i = from; i <= to; i++) set.add(i);
        for (int m : more) set.add(m);
        return set;
    }

    @Test
    void combatCollections() {
        Map<Integer, MenuSlot> slots = CollectionMenus.category(banana(), Collections.data().category("COMBAT"));
        assertIcon(slots, 4, Material.STONE_SWORD, "&aCombat Collections", "&7View your &aCombat Collections&7!", "",
                "&7Collections Unlocked: &e81.8&6%", "&2&l&m                     &f&l&m    &r &e9&6/&e11");
        assertIcon(slots, 10, Material.BLAZE_ROD, "&eBlaze Rod", "&7View all your &aBlaze Rod Collection", "&7progress and rewards!", "",
                "&7Progress to Blaze Rod I: &e70&6%", "&2&l&m                  &f&l&m       &r &e35&6/&e50", "", "&7Blaze Rod I Rewards:",
                "  &9Blaze Minion &7Recipes", "  &8+&b4 SkyBlock XP", "", "&eClick to view!");
        assertIcon(slots, 11, Material.BONE, "&eBone VII", "&7View all your &aBone Collection", "&7progress and rewards!", "",
                "&7Progress to Bone VIII: &e68.6&6%", "&2&l&m                  &f&l&m       &r &e17,153&6/&e25k", "", "&7Bone VIII Rewards:",
                "  &9Skeleton's Helmet &7Recipe", "  &8+&b4 SkyBlock XP", "", "&eClick to view!");
        assertIcon(slots, 12, Material.GRAY_DYE, "&cChili Pepper", "&7Find this item to add it to your", "&7collection and unlock collection",
                "&7rewards!", "", "&cYou haven't found this item yet!");
        assertIcon(slots, 13, Material.ENDER_PEARL, "&eEnder Pearl V", "&7View all your &aEnder Pearl Collection", "&7progress and rewards!", "",
                "&7Progress to Ender Pearl VI: &e73.2&6%", "&2&l&m                   &f&l&m      &r &e7,316&6/&e10k", "", "&7Ender Pearl VI Rewards:",
                "  &9Medium Dragon Sack &7Recipe", "  &aEnchanted Eye of Ender &7Recipe", "  &8+&b4 SkyBlock XP", "", "&eClick to view!");
        assertIcon(slots, 14, Material.GRAY_DYE, "&cGhast Tear", "&7Find this item to add it to your", "&7collection and unlock collection",
                "&7rewards!", "", "&cYou haven't found this item yet!");
        assertIcon(slots, 15, Material.GUNPOWDER, "&eGunpowder", "&7View all your &aGunpowder Collection", "&7progress and rewards!", "",
                "&7Progress to Gunpowder I: &e36&6%", "&2&l&m         &f&l&m                &r &e18&6/&e50", "", "&7Gunpowder I Rewards:",
                "  &9Creeper Minion &7Recipes", "  &8+&b4 SkyBlock XP", "", "&eClick to view!");
        assertIcon(slots, 16, Material.MAGMA_CREAM, "&eMagma Cream I", "&7View all your &aMagma Cream Collection", "&7progress and rewards!", "",
                "&7Progress to Magma Cream II: &e42.4&6%", "&2&l&m           &f&l&m              &r &e106&6/&e250", "", "&7Magma Cream II Rewards:",
                "  &9Fire Protection &7Exp Discount &a(-25%)", "  &8+&b4 SkyBlock XP", "", "&eClick to view!");
        assertIcon(slots, 19, Material.ROTTEN_FLESH, "&eRotten Flesh VII", "&7View all your &aRotten Flesh Collection", "&7progress and rewards!",
                "", "&7Progress to Rotten Flesh VIII: &e51.3&6%", "&2&l&m             &f&l&m            &r &e12,831&6/&e25k", "",
                "&7Rotten Flesh VIII Rewards:", "  &5Zombie Chestplate &7Recipe", "  &5Zombie Leggings &7Recipe", "  &5Zombie Boots &7Recipe",
                "  &8+&b4 SkyBlock XP", "", "&eClick to view!");
        assertIcon(slots, 20, Material.SLIME_BALL, "&aSlimeball IX", "&7View all your &aSlimeball Collection", "&7progress and rewards!", "",
                "&a&lCOLLECTION MAXED OUT!", "&7Total collected: &e823,679", "", "&eClick to view!");
        assertIcon(slots, 21, Material.SPIDER_EYE, "&eSpider Eye III", "&7View all your &aSpider Eye Collection", "&7progress and rewards!", "",
                "&7Progress to Spider Eye IV: &e66.2&6%", "&2&l&m                 &f&l&m        &r &e662&6/&e1k", "", "&7Spider Eye IV Rewards:",
                "  &aEnchanted Spider Eye &7Recipe", "  &8+&b4 SkyBlock XP", "", "&eClick to view!");
        assertIcon(slots, 22, Material.STRING, "&eString IV", "&7View all your &aString Collection", "&7progress and rewards!", "",
                "&7Progress to String V: &e43&6%", "&2&l&m           &f&l&m              &r &e1,075&6/&e2.5k", "", "&7String V Rewards:",
                "  &9Silk Touch &7Exp Discount &a(-25%)", "  &8+&b4 SkyBlock XP", "", "&eClick to view!");
        assertIcon(slots, 48, Material.ARROW, "&aGo Back", "&7To Collections");
        // The border is glass; the inside past the collections is empty.
        assertEquals(range(0, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 50, 51, 52, 53), glass(slots));
        for (int slot : new int[] {23, 24, 25, 28, 34, 37, 43}) assertNull(slots.get(slot));
    }

    @Test
    void blazeRodCollection() {
        Map<Integer, MenuSlot> slots = CollectionMenus.collection(banana(), Collections.data().collection("BLAZE_ROD"));
        assertIcon(slots, 4, Material.BLAZE_ROD, "&eBlaze Rod", "&7View all your &aBlaze Rod Collection", "&7progress and rewards!", "",
                "&7Total Collected: &e35");
        assertIcon(slots, 18, Material.YELLOW_STAINED_GLASS_PANE, "&eBlaze Rod I", "", "&7Progress: &e70&6%",
                "&2&l&m                  &f&l&m       &r &e35&6/&e50", "", "&7Rewards:", "  &9Blaze Minion &7Recipes", "  &8+&b4 SkyBlock XP", "",
                "&eClick to view rewards!");
        assertEquals(1, slots.get(18).amount());
        assertIcon(slots, 19, Material.RED_STAINED_GLASS_PANE, "&cBlaze Rod II", "", "&7Progress: &e14&6%",
                "&2&l&m    &f&l&m                     &r &e35&6/&e250", "", "&7Rewards:", "  &9Fire Aspect &7Exp Discount &a(-25%)",
                "  &8+&b4 SkyBlock XP");
        assertEquals(2, slots.get(19).amount());
        assertIcon(slots, 20, Material.RED_STAINED_GLASS_PANE, "&cBlaze Rod III", "", "&7Progress: &e3.5&6%",
                "&2&l&m &f&l&m                        &r &e35&6/&e1k", "", "&7Rewards:", "  &aEnchanted Blaze Powder &7Recipe",
                "  &8+&b4 SkyBlock XP", "", "&eClick to view rewards!");
        assertIcon(slots, 21, Material.RED_STAINED_GLASS_PANE, "&cBlaze Rod IV", "", "&7Progress: &e1.4&6%",
                "&2&l&m &f&l&m                        &r &e35&6/&e2.5k", "", "&7Rewards:", "  &fFire Talisman &7Recipe", "  &9Blaze Belt &7Recipe",
                "  &8+&b4 SkyBlock XP", "", "&eClick to view rewards!");
        assertIcon(slots, 23, Material.RED_STAINED_GLASS_PANE, "&cBlaze Rod V", "", "&7Progress: &e0.7&6%",
                "&2&l&m &f&l&m                        &r &e35&6/&e5k", "", "&7Rewards:", "  &9Flame &7Exp Discount &a(-25%)", "  &8+&b4 SkyBlock XP");
        assertIcon(slots, 24, Material.RED_STAINED_GLASS_PANE, "&cBlaze Rod VI", "", "&7Progress: &e0.4&6%",
                "&2&l&m &f&l&m                        &r &e35&6/&e10k", "", "&7Rewards:", "  &9Enchanted Blaze Rod &7Recipe", "  &9Blaze Wax &7Recipe",
                "  &7[Lvl 1] &fBlaze &7Recipe", "  &8+&b4 SkyBlock XP", "", "&eClick to view rewards!");
        assertIcon(slots, 25, Material.RED_STAINED_GLASS_PANE, "&cBlaze Rod VII", "", "&7Progress: &e0.1&6%",
                "&2&l&m &f&l&m                        &r &e35&6/&e25k", "", "&7Rewards:", "  &5Blaze Helmet &7Recipe", "  &5Blaze Chestplate &7Recipe",
                "  &5Blaze Leggings &7Recipe", "  &5Blaze Boots &7Recipe", "  &5Vanquished Blaze Belt &7Recipe", "  &8+&b4 SkyBlock XP", "",
                "&eClick to view rewards!");
        assertIcon(slots, 26, Material.RED_STAINED_GLASS_PANE, "&cBlaze Rod VIII", "", "&7Progress: &e0.1&6%",
                "&2&l&m &f&l&m                        &r &e35&6/&e50k", "", "&7Rewards:", "  &8+&310,000 &7Combat Experience", "  &8+&b4 SkyBlock XP");
        assertEquals(8, slots.get(26).amount());
        assertIcon(slots, 48, Material.ARROW, "&aGo Back", "&7To Combat Collections");
        // Eight tiers skip the middle slot.
        Set<Integer> glass = range(0, 53);
        glass.removeAll(Set.of(4, 18, 19, 20, 21, 23, 24, 25, 26, 48, 49));
        assertEquals(glass, glass(slots));
    }

    @Test
    void blazeRodIRewards() {
        Map<Integer, MenuSlot> slots = CollectionMenus.rewards(banana(), Collections.data().collection("BLAZE_ROD"), 1);
        assertIcon(slots, 4, Material.BLAZE_ROD, "&eBlaze Rod I", "", "&7Progress: &e70&6%", "&2&l&m                  &f&l&m       &r &e35&6/&e50",
                "", "&7Rewards:", "  &9Blaze Minion &7Recipes", "  &8+&b4 SkyBlock XP");
        assertEquals(new Icon(Material.PLAYER_HEAD, "&9Blaze Minion Recipes", List.of("&7Place this minion and it will start",
                "&7generating and slaying Blazes!", "", "&eClick to view recipes!"),
                "43f32ebdc046675a936f214164dbd3c2fbd2e84b4bde737483aecd9bbbcb03f1"), icon(slots, 22));
        assertIcon(slots, 48, Material.ARROW, "&aGo Back", "&7To Blaze Rod Collection");
        Set<Integer> glass = range(0, 53);
        glass.removeAll(Set.of(4, 22, 48, 49));
        assertEquals(glass, glass(slots));
    }

    @Test
    void stringCollection() {
        Map<Integer, MenuSlot> slots = CollectionMenus.collection(banana(), Collections.data().collection("STRING"));
        assertIcon(slots, 4, Material.STRING, "&eString IV", "&7View all your &aString Collection", "&7progress and rewards!", "",
                "&7Total Collected: &e1,075");
        assertIcon(slots, 18, Material.LIME_STAINED_GLASS_PANE, "&aString I", "", "&7Progress: &a100%",
                "&2&l&m                         &r &e1,075&6/&e50", "", "&7Rewards:", "  &9Spider Minion &7Recipes", "  &8+&b4 SkyBlock XP", "",
                "&eClick to view rewards!");
        assertIcon(slots, 20, Material.LIME_STAINED_GLASS_PANE, "&aString III", "", "&7Progress: &a100%",
                "&2&l&m                         &r &e1,075&6/&e250", "", "&7Rewards:", "  &7[Lvl 1] &fSpider &7Recipe", "  &aQuiver",
                "  &8+&b4 SkyBlock XP", "", "&eClick to view rewards!");
        assertIcon(slots, 21, Material.LIME_STAINED_GLASS_PANE, "&aString IV", "", "&7Progress: &a100%",
                "&2&l&m                         &r &e1,075&6/&e1k", "", "&7Rewards:", "  &aEnchanted String &7Recipe", "  &aGrappling Hook &7Recipe",
                "  &8+&b4 SkyBlock XP", "", "&eClick to view rewards!");
        assertIcon(slots, 22, Material.YELLOW_STAINED_GLASS_PANE, "&eString V", "", "&7Progress: &e43&6%",
                "&2&l&m           &f&l&m              &r &e1,075&6/&e2.5k", "", "&7Rewards:", "  &9Silk Touch &7Exp Discount &a(-25%)",
                "  &8+&b4 SkyBlock XP");
        assertIcon(slots, 23, Material.RED_STAINED_GLASS_PANE, "&cString VI", "", "&7Progress: &e21.5&6%",
                "&2&l&m      &f&l&m                   &r &e1,075&6/&e5k", "", "&7Rewards:", "  &9Infinite Quiver &7Exp Discount &a(-25%)",
                "  &8+&79 &aQuiver &7Slots", "  &8+&b4 SkyBlock XP");
        assertIcon(slots, 24, Material.RED_STAINED_GLASS_PANE, "&cString VII", "", "&7Progress: &e10.8&6%",
                "&2&l&m   &f&l&m                      &r &e1,075&6/&e10k", "", "&7Rewards:", "  &8+&320,000 &7Combat Experience", "  &8+&b4 SkyBlock XP");
        assertIcon(slots, 25, Material.RED_STAINED_GLASS_PANE, "&cString VIII", "", "&7Progress: &e4.3&6%",
                "&2&l&m  &f&l&m                       &r &e1,075&6/&e25k", "", "&7Rewards:", "  &9Spider's Boots &7Recipe", "  &8+&b4 SkyBlock XP",
                "", "&eClick to view rewards!");
        assertIcon(slots, 26, Material.RED_STAINED_GLASS_PANE, "&cString IX", "", "&7Progress: &e2.1&6%",
                "&2&l&m &f&l&m                        &r &e1,075&6/&e50k", "", "&7Rewards:", "  &8+&79 &aQuiver &7Slots", "  &8+&b4 SkyBlock XP");
        assertEquals(9, slots.get(26).amount());
    }

    @Test
    void stringVIIIRewards() {
        Map<Integer, MenuSlot> slots = CollectionMenus.rewards(banana(), Collections.data().collection("STRING"), 8);
        assertIcon(slots, 4, Material.STRING, "&cString VIII", "", "&7Progress: &e4.3&6%", "&2&l&m  &f&l&m                       &r &e1,075&6/&e25k",
                "", "&7Rewards:", "  &9Spider's Boots &7Recipe", "  &8+&b4 SkyBlock XP");
        assertEquals(MenuSlot.item("SPIDER_BOOTS", 1, List.of("", "&eClick to view recipe!")), slots.get(22));
        assertIcon(slots, 48, Material.ARROW, "&aGo Back", "&7To String Collection");
    }

    @Test
    void bossCollections() {
        Map<Integer, MenuSlot> slots = CollectionMenus.bosses(banana());
        assertIcon(slots, 4, Material.WITHER_SKELETON_SKULL, "&5Boss Collections", "&7View your progress and claim", "&7rewards you have obtained from",
                "&7defeating SkyBlock bosses!", "", "&7Boss Collections Unlocked: &e75&6%", "&2&l&m                   &f&l&m      &r &e6&6/&e8");
        assertEquals(new Icon(Material.PLAYER_HEAD, "&eBonzo", List.of("&7View all your Bonzo Collection", "&7progress and rewards!", "",
                "&7Progress to Bonzo I: &e20&6%", "&2&l&m     &f&l&m                    &r &e5&6/&e25", "", "&7Bonzo I Reward:", "  &9Red Nose", "",
                "&eClick to view!"), "12716ecbf5b8da00b05f316ec6af61e8bd02805b21eb8e440151468dc656549c"), icon(slots, 10));
        assertIcon(slots, 11, Material.PLAYER_HEAD, "&eScarf", "&7View all your Scarf Collection", "&7progress and rewards!", "",
                "&7Progress to Scarf I: &e44&6%", "&2&l&m           &f&l&m              &r &e11&6/&e25", "", "&7Scarf I Reward:", "  &9Red Scarf", "",
                "&eClick to view!");
        assertIcon(slots, 12, Material.PLAYER_HEAD, "&eThe Professor", "&7View all your The Professor", "&7Collection progress and rewards!", "",
                "&7Progress to The Professor I: &e16&6%", "&2&l&m    &f&l&m                     &r &e4&6/&e25", "", "&7The Professor I Reward:",
                "  &9Suspicious Vial", "", "&eClick to view!");
        assertIcon(slots, 13, Material.PLAYER_HEAD, "&eThorn", "&7View all your Thorn Collection", "&7progress and rewards!", "",
                "&7Progress to Thorn I: &e2&6%", "&2&l&m &f&l&m                        &r &e1&6/&e50", "", "&7Thorn I Reward:", "  &9Spirit Stone", "",
                "&eClick to view!");
        assertIcon(slots, 15, Material.PLAYER_HEAD, "&eSadan", "&7View all your Sadan Collection", "&7progress and rewards!", "",
                "&7Progress to Sadan I: &e20&6%", "&2&l&m     &f&l&m                    &r &e10&6/&e50", "", "&7Sadan I Reward:", "  &5Giant Tooth", "",
                "&eClick to view!");
        assertIcon(slots, 16, Material.GRAY_DYE, "&cNecron", "&7Kill this boss once to view collection", "&7rewards!", "",
                "&cKill Necron once to view this", "&ccollection!");
        assertIcon(slots, 19, Material.GRAY_DYE, "&cKuudra", "&7Kill this boss once to view collection", "&7rewards!", "",
                "&cKill Kuudra once to view this", "&ccollection!");
        assertEquals(range(0, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 50, 51, 52, 53), glass(slots));
    }

    @Test
    void bonzoCollection() {
        Map<Integer, MenuSlot> slots = CollectionMenus.collection(banana(), Collections.data().collection("CATACOMBS_1"));
        assertIcon(slots, 4, Material.PLAYER_HEAD, "&eBonzo", "&7View all your Bonzo Collection", "&7progress and rewards!", "",
                "&7Total Collection: &e5", "", "&7Master Mode completions reward", "&7more Kill Count:", "&8 - &cBasic Dungeons: &e+1 Kill Count",
                "&8 - &cMaster Mode: &e+2 Kill Count");
        assertIcon(slots, 19, Material.YELLOW_STAINED_GLASS_PANE, "&eBonzo I", "", "&7Progress: &e20&6%",
                "&2&l&m     &f&l&m                    &r &e5&6/&e25", "", "&7Rewards:", "  &9Red Nose", "  &8+&b15 SkyBlock XP", "",
                "&eClick to view rewards!");
        assertIcon(slots, 21, Material.RED_STAINED_GLASS_PANE, "&cBonzo III", "", "&7Progress: &e5&6%",
                "&2&l&m  &f&l&m                       &r &e5&6/&e100", "", "&7Rewards:", "  &cGolden Bonzo Head", "  &dGold Essence &8x250",
                "  &8+&b15 SkyBlock XP", "", "&eClick to view rewards!");
        assertIcon(slots, 23, Material.RED_STAINED_GLASS_PANE, "&cBonzo IV", "", "&7Progress: &e3.3&6%",
                "&2&l&m &f&l&m                        &r &e5&6/&e150", "", "&7Rewards:", "  &9Bonzo's Staff", "  &8+&b25 SkyBlock XP", "",
                "&eClick to view rewards!");
        assertIcon(slots, 24, Material.RED_STAINED_GLASS_PANE, "&cBonzo V", "", "&7Progress: &e2&6%",
                "&2&l&m &f&l&m                        &r &e5&6/&e250", "", "&7Rewards:", "  &6Recombobulator 3000", "  &8+&b25 SkyBlock XP", "",
                "&eClick to view rewards!");
        assertIcon(slots, 25, Material.RED_STAINED_GLASS_PANE, "&cBonzo VI", "", "&7Progress: &e0.5&6%",
                "&2&l&m &f&l&m                        &r &e5&6/&e1k", "", "&7Rewards:", "  &cDiamond Bonzo Head &7Recipe",
                "  &dDiamond Essence &8x1000", "  &8+&b25 SkyBlock XP", "", "&eClick to view rewards!");
        assertIcon(slots, 48, Material.ARROW, "&aGo Back", "&7To Boss Collections");
        Set<Integer> glass = range(0, 53);
        glass.removeAll(Set.of(4, 19, 20, 21, 23, 24, 25, 48, 49));
        assertEquals(glass, glass(slots));
    }

    @Test
    void bonzoIRewards() {
        Map<Integer, MenuSlot> slots = CollectionMenus.rewards(banana(), Collections.data().collection("CATACOMBS_1"), 1);
        assertIcon(slots, 4, Material.PLAYER_HEAD, "&eBonzo I", "", "&7Progress: &e20&6%", "&2&l&m     &f&l&m                    &r &e5&6/&e25", "",
                "&7Rewards:", "  &9Red Nose", "  &8+&b15 SkyBlock XP");
        assertEquals(MenuSlot.item("RED_NOSE", 1, List.of("", "&cYou don't qualify for this reward!")), slots.get(22));
        assertIcon(slots, 48, Material.ARROW, "&aGo Back", "&7To Bonzo Collection");
        // Reached and claimed: what it says then is UNKNOWN (see CollectionMenus.reward).
        Document profile = banana();
        Collections.set(profile, "CATACOMBS_1", 25);
        assertEquals(List.of("", "&eClick to claim!"), CollectionMenus.rewards(profile, Collections.data().collection("CATACOMBS_1"), 1)
                .get(22).extra());
        Collections.claim(profile, "CATACOMBS_1", 1);
        assertEquals(List.of("", "&aYou have claimed this reward!"),
                CollectionMenus.rewards(profile, Collections.data().collection("CATACOMBS_1"), 1).get(22).extra());
    }

    /**
     * Bonzo III's rewards (not recorded: the wiki's Collection UI): the head and the Gold Essence, x250 as the
     * recorded tier says, with its own head; claimable once the tier is reached, claimed after.
     */
    @Test
    void bonzoIIIRewards() {
        Map<Integer, MenuSlot> slots = CollectionMenus.rewards(banana(), Collections.data().collection("CATACOMBS_1"), 3);
        assertEquals(MenuSlot.item("GOLD_BONZO_HEAD", 1, List.of("", "&cYou don't qualify for this reward!")), slots.get(21));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&dGold Essence&8 x250", List.of("&7Essence can be used to convert",
                "&7some items into Dungeon items", "&7and to repair Dungeon items!", "", "&cYou don't qualify for this reward!"),
                "8816606260779b23ed15f87c56c932240db745f86f683d1f4deb83a4a125fa7b"), icon(slots, 23));
        Document profile = banana();
        Collections.set(profile, "CATACOMBS_1", 100);
        assertEquals("&eClick to claim!", icon(CollectionMenus.rewards(profile, Collections.data().collection("CATACOMBS_1"), 3), 23)
                .lore().getLast());
        RewardsMenu.claim(profile, Collections.data().collection("CATACOMBS_1"), 3);
        assertEquals("&aYou have claimed this reward!",
                icon(CollectionMenus.rewards(profile, Collections.data().collection("CATACOMBS_1"), 3), 23).lore().getLast());
    }

    /** Banana's Collections: each category's found collections of all (00:40.3), from any found ones. */
    @Test
    void collectionsMenu() {
        Document counts = new Document();
        int[] found = {13, 19, 9, 10, 10};
        List<CollectionData.Category> categories = Collections.data().categories();
        for (int c = 0; c < categories.size(); c++) {
            for (int i = 0; i < found[c]; i++) counts.append(categories.get(c).collections().get(i), 1L);
        }
        Document profile = new Document("mode", "NORMAL").append(Collections.COUNTS, counts).append(Collections.BOSS_COUNTS,
                banana().get(Collections.BOSS_COUNTS));
        Map<Integer, MenuSlot> slots = CollectionMenus.collections(profile, 0, 719);
        assertIcon(slots, 4, Material.PAINTING, "&aCollections", "&7View all of the items available in", "&7SkyBlock. Collect more of an item to",
                "&7unlock rewards on your way to", "&7becoming a master of SkyBlock!", "", "&7Collections Unlocked: &e73.5&6%",
                "&2&l&m                   &f&l&m      &r &e61&6/&e83", "", "&8Also accessible via /collection.");
        assertIcon(slots, 20, Material.GOLDEN_HOE, "&aFarming Collections", "&7View your &aFarming Collections&7!", "",
                "&7Collections Unlocked: &e65&6%", "&2&l&m                 &f&l&m        &r &e13&6/&e20", "", "&eClick to view!");
        assertIcon(slots, 21, Material.STONE_PICKAXE, "&aMining Collections", "&7View your &aMining Collections&7!", "",
                "&7Collections Unlocked: &e76&6%", "&2&l&m                   &f&l&m      &r &e19&6/&e25", "", "&eClick to view!");
        assertIcon(slots, 22, Material.STONE_SWORD, "&aCombat Collections", "&7View your &aCombat Collections&7!", "",
                "&7Collections Unlocked: &e81.8&6%", "&2&l&m                     &f&l&m    &r &e9&6/&e11", "", "&eClick to view!");
        assertIcon(slots, 23, Material.JUNGLE_SAPLING, "&aForaging Collections", "&7View your &aForaging Collections&7!", "",
                "&7Collections Unlocked: &e66.7&6%", "&2&l&m                 &f&l&m        &r &e10&6/&e15", "", "&eClick to view!");
        assertIcon(slots, 24, Material.FISHING_ROD, "&aFishing Collections", "&7View your &aFishing Collections&7!", "",
                "&7Collections Unlocked: &e83.3&6%", "&2&l&m                     &f&l&m    &r &e10&6/&e12", "", "&eClick to view!");
        assertIcon(slots, 31, Material.WITHER_SKELETON_SKULL, "&5Boss Collections", "&7View your progress and claim",
                "&7rewards you have obtained from", "&7defeating SkyBlock bosses!", "", "&7Boss Collections Unlocked: &e75&6%",
                "&2&l&m                   &f&l&m      &r &e6&6/&e8", "", "&eClick to view!");
        assertIcon(slots, 48, Material.ARROW, "&aGo Back", "&7To SkyBlock Menu");
        assertIcon(slots, 53, Material.OAK_SIGN, "&aShow Collection Rankings", "&7Show the rankings display for your", "&7Collections.", "",
                "&eClick to show!");
        Icon minions = icon(slots, 50);
        assertEquals("&aCrafted Minions", minions.name());
        assertEquals("ebcc099f3a00ece0e5c4b31d31c828e52b06348d0a4eac11f3fcbef3c05cb407", minions.texture());
        assertTrue(minions.lore().contains("&7Crafted minions: &e0&7/&6719"));
        assertTrue(minions.lore().contains("&7Craft &b5 &7more &aunique &7minions to unlock"));
        assertTrue(minions.lore().contains("&7your &b6th &7slot."));
        Set<Integer> glass = range(0, 53);
        glass.removeAll(Set.of(4, 20, 21, 22, 23, 24, 31, 48, 49, 50, 53));
        assertEquals(glass, glass(slots));
    }

    /** A new profile has found nothing: every collection a gray dye, no tiers reached. */
    @Test
    void freshProfile() {
        Document fresh = new Document("mode", "NORMAL");
        Map<Integer, MenuSlot> slots = CollectionMenus.category(fresh, Collections.data().category("COMBAT"));
        for (int i = 0; i < 11; i++) assertEquals(Material.GRAY_DYE, icon(slots, CollectionMenus.INSIDE[i]).material());
        assertEquals(List.of("&7View all of the items available in", "&7SkyBlock. Collect more of an item to", "&7unlock rewards on your way to",
                "&7becoming a master of SkyBlock!", "", "&7Collections Unlocked: &e0&6%", "&f&l&m                         &r &e0&6/&e83", "",
                "&8Also accessible via /collection.", "", "&eClick to view!"), CollectionMenus.summary(fresh, true).lore());
        assertFalse(Collections.found(fresh, "BLAZE_ROD"));
        Map<Integer, MenuSlot> tiers = CollectionMenus.collection(fresh, Collections.data().collection("BLAZE_ROD"));
        assertEquals(Material.YELLOW_STAINED_GLASS_PANE, icon(tiers, 18).material());
        assertEquals(Material.RED_STAINED_GLASS_PANE, icon(tiers, 19).material());
    }

    /** Eleven tiers: nine on the third row, the rest on the next (the wiki's layout; none were recorded). */
    @Test
    void moreThanNineTiers() {
        assertEquals(List.of(18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28), java.util.Arrays.stream(CollectionMenus.rowSlots(11)).boxed().toList());
        assertEquals(List.of(19, 20, 21, 23, 24, 25), java.util.Arrays.stream(CollectionMenus.rowSlots(6)).boxed().toList());
        assertEquals(List.of(22), java.util.Arrays.stream(CollectionMenus.rowSlots(1)).boxed().toList());
    }
}
