package net.icxd.dungeons.leveling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.leveling.LevelingData.RewardKind;
import net.icxd.dungeons.menu.Icon;

/**
 * SkyBlock Leveling, Level N Rewards, Leveling Rewards and its lists against the recorded ones (the SkyBlock Menu
 * tour: Banana at level 88 with 8,834 XP, 01:34.5 to 02:04.4, and the new profile Lemon's at 04:39.7; stat glyphs as
 * the classic symbols).
 */
class LevelingMenusTest {
    private static final int BANANA = 8834;

    private static List<String> lore(Map<Integer, Icon> icons, int slot) {
        return icons.get(slot).lore();
    }

    @Test
    void levelingSlots() {
        LevelingData data = Fixtures.fixture();
        assertEquals(Set.of(4, 16, 19, 20, 21, 22, 23, 25, 30, 34, 43, 48, 50), LevelingMenu.icons(Fixtures.viewAt(data, 0)).keySet());
        // No milestone left: no item (UNKNOWN on Hypixel).
        assertNull(LevelingMenu.icons(Fixtures.viewAt(data, 4000)).get(LevelingMenu.MILESTONE));
    }

    @Test
    void freshLeveling() {
        LevelingData data = Fixtures.recorded();
        Map<Integer, Icon> icons = LevelingMenu.icons(Fixtures.viewAt(data, 0));
        assertEquals(List.of("&8Classic Mode", "", "&7Your level: &70", "&7You have: &b0 XP", "", "&7You have completed &30%&7 of the total",
                "&7SkyBlock XP Tasks.", "", "&7Ranking information requires", "&7SkyBlock Level 10 or higher.", "&8Level rankings may take time to",
                "&8refresh."), lore(icons, 4));
        assertEquals(new Icon(Material.LIME_STAINED_GLASS_PANE, "&7Level 0", "&8Your Level", "", "&7Rewards:", "", "&a&lUNLOCKED", "",
                "&eClick to view rewards!"), icons.get(19));
        assertEquals(new Icon(Material.YELLOW_STAINED_GLASS_PANE, "&7Level 1", "&8Next Level", "", "&7Reward:", " &8+&a5 &c❤ Health", "",
                "&7Progress to Level Up:", "&f&l&m                         &r &70&8/&7100 XP", "", "&eClick to view rewards!"), icons.get(20));
        assertEquals(new Icon(Material.RED_STAINED_GLASS_PANE, "&7Level 2", "&7Reward:", " &8+&a5 &c❤ Health", "", "&eClick to view rewards!"),
                icons.get(21));
        assertEquals(new Icon(Material.RED_STAINED_GLASS, "&7Level 3", "&7Rewards:", " &bAccess to Community Shop", " &8+&a5 &c❤ Health", "",
                "&eClick to view rewards!"), icons.get(22));
        Icon milestone = icons.get(30);
        assertEquals(Material.PLAYER_HEAD, milestone.material());
        assertEquals("&7Level 3", milestone.name());
        assertEquals(List.of("&8Next Milestone Level", "", "&7Rewards:", " &bAccess to Community Shop", " &8+&a5 &c❤ Health", "",
                "&7XP Left to Gain: &b300 XP &8(3 Levels)", "", "&eClick to view rewards!"), milestone.lore());
        assertEquals(List.of("&7View all the rewards you can unlock", "&7by leveling up your SkyBlock Level.", "", "&7Progress to Max: &30%",
                "&f&l&m                         &r &30&b/&354", "", "&eClick to view rewards!"), lore(icons, 34));
        assertEquals(new Icon(Material.LIME_DYE, "&bSkyBlock Levels in Chat", "&7View other players' SkyBlock Level",
                "&7and their selected emblem in their", "&7chat messages.", "", "&a&lENABLED", "", "&eClick to disable!"), icons.get(50));
    }

    @Test
    void levelledLeveling() {
        LevelingData data = Fixtures.recorded();
        Map<Integer, Icon> icons = LevelingMenu.icons(Fixtures.viewAt(data, BANANA));
        // Hypixel's rank shows a moment later; before it, this (01:34.2). Its 15.2% needs more than the categories'
        // 57,874 XP: UNKNOWN what else it counts, so 15.3% here.
        assertEquals(List.of("&8Classic Mode", "", "&7Your level: &e88", "&7You have: &b8,834 XP", "", "&7You have completed &315.3%&7 of the",
                "&7total SkyBlock XP Tasks.", "", "&7Ranking information requires", "&7SkyBlock Level 10 or higher.",
                "&8Level rankings may take time to", "&8refresh."), lore(icons, 4));
        assertEquals(new Icon(Material.REDSTONE_TORCH, "&aWays to Level Up", "&7Learn more about the different ways", "&7to earn SkyBlock XP.",
                "", "&7Also see a specific breakdown of", "&7where all your XP comes from!", "", "&eClick to view!"), icons.get(16));
        assertEquals(new Icon(Material.LIME_STAINED_GLASS_PANE, "&eLevel 88", "&8Your Level", "", "&7Reward:", " &8+&a5 &c❤ Health", "",
                "&a&lUNLOCKED", "", "&eClick to view rewards!"), icons.get(19));
        assertEquals(new Icon(Material.YELLOW_STAINED_GLASS_PANE, "&eLevel 89", "&8Next Level", "", "&7Reward:", " &8+&a5 &c❤ Health", "",
                "&7Progress to Level Up:", "&e&l&m         &f&l&m                &r &e34&8/&e100 XP", "", "&eClick to view rewards!"), icons.get(20));
        assertEquals(new Icon(Material.RED_STAINED_GLASS, "&eLevel 90", "&7Rewards:", " &fBoxes Emblem &7⧉", " &8+&a1 &c❁ Strength",
                " &8+&a5 &c❤ Health", "", "&eClick to view rewards!"), icons.get(21));
        assertEquals(new Icon(Material.RED_STAINED_GLASS_PANE, "&eLevel 92", "&7Reward:", " &8+&a5 &c❤ Health", "", "&eClick to view rewards!"),
                icons.get(23));
        assertEquals(new Icon(Material.NAME_TAG, "&eLevel 90", "&8Next Milestone Level", "", "&7Rewards:", " &fBoxes Emblem &7⧉",
                " &8+&a1 &c❁ Strength", " &8+&a5 &c❤ Health", "", "&7XP Left to Gain: &b166 XP &8(2 Levels)", "", "&eClick to view rewards!"),
                icons.get(30));
        assertEquals(List.of("&7View all the rewards you can unlock", "&7by leveling up your SkyBlock Level.", "", "&7Progress to Max: &344.4%",
                "&3&l&m            &f&l&m             &r &324&b/&354", "", "&eClick to view rewards!"), lore(icons, 34));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&a⚑ SkyBlock XP Guide", List.of("&7Your &6SkyBlock XP Guide &7tracks the",
                "&7progress you have made through", "&7SkyBlock.", "", "&7Complete tasks within your current", "&7game stage to increase your",
                "&bSkyBlock Level &7and become a &dMaster", "&7of SkyBlock!", "", "&8Also accessible via /skyblockxp", "", "&eClick to view!"),
                LevelingMenu.GUIDE_HEAD), icons.get(25));
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Menu"), icons.get(48));
    }

    @Test
    void chatSwitch() {
        assertEquals(List.of("&7View other players' SkyBlock Level", "&7and their selected emblem in their", "&7chat messages.", "",
                "&c&lDISABLED", "", "&eClick to enable!"), LevelingMenu.chat(false).lore());
        assertEquals(Material.GRAY_DYE, LevelingMenu.chat(false).material());
    }

    /** Level 88's and Level 90's rewards (01:37.9, 01:40.3). */
    @Test
    void levelRewards() {
        LevelingData data = Fixtures.recorded();
        LevelingView view = Fixtures.viewAt(data, BANANA);
        assertEquals(Map.of(13, new Icon(Material.GOLDEN_APPLE, "&8+&a5 &c❤ Health", "&8Level 88", "", "&a&lUNLOCKED")),
                LevelMenu.icons(view, 88));
        Map<Integer, Icon> ninety = LevelMenu.icons(view, 90);
        assertEquals(Set.of(11, 13, 15), ninety.keySet());
        List<String> state = List.of("&7Levels left to Unlock: &32", "&3&l&m     &f&l&m                    &r &334&b/&3200 XP");
        assertEquals(Material.NAME_TAG, ninety.get(11).material());
        assertEquals("&fBoxes Emblem &7⧉", ninety.get(11).name());
        assertEquals(concat(List.of("&8Level 90", ""), state), ninety.get(11).lore());
        assertEquals(new Icon(Material.BLAZE_POWDER, "&8+&a1 &c❁ Strength", concat(List.of("&8Level 90", ""), state)), ninety.get(13));
        assertEquals(new Icon(Material.GOLDEN_APPLE, "&8+&a5 &c❤ Health", concat(List.of("&8Level 90", ""), state)), ninety.get(15));
        assertEquals("Level 90 Rewards", "Level " + 90 + " Rewards");
    }

    @Test
    void levelRewardSlots() {
        assertEquals(List.of(13), box(LevelMenu.slots(1)));
        assertEquals(List.of(12, 14), box(LevelMenu.slots(2)));
        assertEquals(List.of(11, 13, 15), box(LevelMenu.slots(3)));
        assertEquals(List.of(9, 11, 13, 15, 17), box(LevelMenu.slots(5)));
    }

    /** Leveling Rewards (01:53.4). */
    @Test
    void levelingRewards() {
        LevelingData data = Fixtures.recorded();
        Map<Integer, Icon> icons = RewardsMenu.icons(Fixtures.viewAt(data, BANANA));
        assertEquals(new Icon(Material.NETHER_STAR, "&aFeature Rewards", "&7Specific game features such as the", "&7Bazaar or Community Shop.", "",
                "&7Next Reward:", "&625B Auction House Bid Limit", "&8at Level 100", "", "&7Rewards Unlocked: &375%",
                "&3&l&m                   &f&l&m      &r &39&b/&312", "", "&eClick to view rewards!"), icons.get(11));
        assertEquals(new Icon(Material.YELLOW_DYE, "&aPrefix Color Rewards", "&7New colors for your level prefix", "&7shown in TAB and in chat!", "",
                "&7Next Reward:", "&aGreen Level Prefix", "&8at Level 120", "", "&7Rewards Unlocked: &316.7%",
                "&3&l&m     &f&l&m                    &r &32&b/&312", "", "&eClick to view rewards!"), icons.get(12));
        assertEquals(new Icon(Material.NAME_TAG, "&aPrefix Emblem Rewards", "&7Emblems to show next to your name", "&7that signify special achievements.",
                "", "&7Next Reward:", "&fBoxes Emblem &7⧉", "&8at Level 90", "", "&7Rewards Unlocked: &338.1%",
                "&3&l&m          &f&l&m               &r &38&b/&321", "", "&eClick to view rewards!"), icons.get(13));
        assertEquals(new Icon(Material.DIAMOND_HELMET, "&aStat Rewards", "&7Statistic bonuses that will power you", "&7up as you level up.", "",
                "&7Next Reward:", "&8+&a5 &c❤ Health", "&8at Level 89", "", "&7For every level:", "&8+&a5 &c❤ Health", "", "&7For every 5 levels:",
                "&8+&a1 &c❁ Strength"), icons.get(14));
        assertEquals(new Icon(Material.BOOK, "&aBonus Rewards", "&7Bonuses that upgrade your Book of", "&7Progression which will provide you",
                "&7better rewards!", "", "&7Next Reward:", "&9Upgrades Book of Progression", "&8at Level 150", "", "&7Rewards Unlocked: &355.6%",
                "&3&l&m              &f&l&m           &r &35&b/&39", "", "&eClick to view rewards!"), icons.get(15));
    }

    /** The features' list (01:55.1) and the emblems' (01:59.8). */
    @Test
    void rewardLists() {
        LevelingData data = Fixtures.recorded();
        LevelingView view = Fixtures.viewAt(data, BANANA);
        Map<Integer, Icon> features = RewardListMenu.icons(view, RewardKind.FEATURE);
        assertEquals(Set.of(4, 19, 20, 21, 22, 23, 24, 25, 29, 30, 31, 32, 33), features.keySet());
        assertEquals("&aRewards", features.get(4).name());
        assertEquals(List.of("&7Specific game features such as the", "&7Bazaar or Community Shop.", "", "&7Next Reward:",
                "&625B Auction House Bid Limit", "&8at Level 100", "", "&7Rewards Unlocked: &375%",
                "&3&l&m                   &f&l&m      &r &39&b/&312"), features.get(4).lore());
        assertEquals("&bAccess to Community Shop", features.get(19).name());
        assertEquals(List.of("&8Level 3", "", "&a&lUNLOCKED"), features.get(19).lore());
        assertEquals(new Icon(Material.GOLDEN_HORSE_ARMOR, "&650B Auction House Bid Limit", "&8Level 200", "", "&7Progress to Unlock: &344.2%",
                "&3&l&m           &f&l&m              &r &388&b/&3200"), features.get(33));

        Map<Integer, Icon> emblems = RewardListMenu.icons(view, RewardKind.EMBLEM);
        assertEquals(new Icon(Material.NAME_TAG, "&fDiamond Emblem &7♦", "&8Level 10", "", "&a&lUNLOCKED"), emblems.get(10));
        assertEquals(new Icon(Material.NAME_TAG, "&fBoxes Emblem &7⧉", "&8Level 90", "", "&7Levels left to Unlock: &32",
                "&3&l&m     &f&l&m                    &r &334&b/&3200 XP"), emblems.get(20));
        assertEquals(new Icon(Material.NAME_TAG, "&fGlobe Emblem &7&l㋖", "&8Level 120", "", "&7Progress to Unlock: &373.6%",
                "&3&l&m                   &f&l&m      &r &388&b/&3120"), emblems.get(22));
        assertEquals(Material.NAME_TAG, emblems.get(RewardListMenu.EMBLEMS).material());

        // The bonuses (02:04.4): theirs at level 88 is 0.9%.
        Map<Integer, Icon> bonuses = RewardListMenu.icons(view, RewardKind.BONUS);
        assertEquals(new Icon(Material.BOOK, "&8+&fBook of Progression", "&8Level 25", "", "&7The &fBook of Progression&7 is a unique",
                "&7accessory that will upgrade and", "&7unlock future bonuses as you level", "&7up.", "", "&a&lUNLOCKED"), bonuses.get(19));
        assertEquals(new Icon(Material.GOLDEN_APPLE, "&9Stacked Healing Bonus", "&8Level 25", "", "&7Bonuses are extra perks inside the",
                "&fBook of Progression&7.", "", "&7When being healed, heal &9+0.9%&7 the", "&7amount.", "", "&a&lUNLOCKED"), bonuses.get(20));
        assertEquals(List.of("&8Level 50", "", "&7Bonuses are extra perks inside the", "&fBook of Progression&7.", "",
                "&7When you are attacked, take &90.9%", "&7less of the incoming damage.", "", "&a&lUNLOCKED"), bonuses.get(21).lore());
        assertEquals(List.of("&8Level 150", "", "&7Your &fBook of Progression&7 will be", "&7upgraded to &9Rare&7.", "",
                "&7Progress to Unlock: &358.9%", "&3&l&m               &f&l&m          &r &388&b/&3150"), bonuses.get(24).lore());
    }

    @Test
    void rewardListSlots() {
        // Twelve: seven, then five centred (as recorded).
        assertEquals(List.of(19, 20, 21, 22, 23, 24, 25, 29, 30, 31, 32, 33), box(RewardListMenu.slots(12)));
        // Nine: seven and two with a gap between (the bonuses', as recorded).
        assertEquals(List.of(19, 20, 21, 22, 23, 24, 25, 30, 32), box(RewardListMenu.slots(9)));
    }

    /** A prefix colour's preview, with their own rank and name; a bonus's size at their level. */
    @Test
    void rewardIcons() {
        LevelingData data = Fixtures.fixture();
        LevelingView view = Fixtures.viewAt(data, 250);
        Icon prefix = RewardListMenu.icon(data.rewards(RewardKind.PREFIX).getFirst(), view);
        assertEquals(List.of("&8Level 40", "", "&7Preview: &8[&f40&8] &b[MVP&6+&b] ICoding", "", "&7Progress to Unlock: &36.2%",
                "&3&l&m  &f&l&m                       &r &32&b/&340"), prefix.lore().stream().map(l -> l.replace('§', '&')).toList());
        Icon bonus = RewardListMenu.icon(data.rewards(RewardKind.BONUS).getFirst(), view);
        assertEquals(List.of("&8Level 3", "", "&7When hit, take &90% &7less.", "", "&7Levels left to Unlock: &31",
                "&3&l&m             &f&l&m            &r &350&b/&3100 XP"), bonus.lore());
        assertEquals("0.9", RewardListMenu.bonus(88));
        assertEquals("5", RewardListMenu.bonus(600));
    }

    private static List<Integer> box(int[] slots) {
        return java.util.Arrays.stream(slots).boxed().toList();
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> out = new java.util.ArrayList<>(a);
        out.addAll(b);
        return out;
    }
}
