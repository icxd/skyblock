package net.icxd.dungeons.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.SkyBlockTime;

/**
 * The SkyBlock Menu against the recorded ones (the SkyBlock Menu tour: Banana's at 00:18.2, the new
 * profile Lemon's at 04:29.1; stat glyphs as classic symbols).
 */
class SkyBlockMenuTest {
    /** 04:29.1 into the tour, which began at 00:15:16 (UTC+2). */
    private static final SkyBlockTime LEMON_TIME = SkyBlockTime.at(Instant.parse("2026-09-26T22:19:45.100Z").toEpochMilli());
    private static final SkyBlockTime BANANA_TIME = SkyBlockTime.at(Instant.parse("2026-09-26T22:15:34.200Z").toEpochMilli());

    /** A new profile: base stats, no skills, no SkyBlock XP. */
    private static final SkyBlockMenu.View FRESH = new SkyBlockMenu.View(Stats.base(), 0, 0, 4, 4, "Lemon", LEMON_TIME, 0, 83, 111, 1016);

    /** Banana's numbers. */
    private static SkyBlockMenu.View levelled() {
        Stats stats = Stats.base().set(Stat.SPEED, 400).set(Stat.STRENGTH, 589.25).set(Stat.DEFENSE, 744.5).set(Stat.CRIT_DAMAGE, 834.03)
                .set(Stat.CRIT_CHANCE, 140.5).set(Stat.HEALTH, 2206).set(Stat.INTELLIGENCE, 692);
        return new SkyBlockMenu.View(stats, 21.6, 8834, 4, 4, "Banana", BANANA_TIME, 61, 83, 492, 1016);
    }

    private static List<String> lore(Map<Integer, Icon> icons, int slot) {
        return icons.get(slot).lore();
    }

    @Test
    void slots() {
        Set<Integer> expected = Set.of(13, 19, 20, 21, 22, 23, 24, 25, 29, 30, 31, 32, 33, 47, 48, 50);
        assertEquals(expected, SkyBlockMenu.icons(FRESH).keySet());
        // No Booster Cookie (slot 51) without a cookie, as Lemon's.
        assertFalse(SkyBlockMenu.icons(levelled()).containsKey(51));
    }

    @Test
    void freshProfile() {
        Map<Integer, Icon> icons = SkyBlockMenu.icons(FRESH);
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aStats & Equipment", "&7View your equipment, stats,", "&7achievements, and more!", "",
                " &f✦ Speed 100", " &c❁ Strength &f0", " &a❈ Defense &f0", " &9☠ Crit Damage &f50%", " &9☣ Crit Chance &f30%",
                " &c❤ Health &f100", " &b✎ Intelligence &f0", " &8and more...", "", "&8Also accessible via /stats", "", "&eClick to view!"),
                icons.get(13));
        assertEquals(List.of("&7View your Skill progression and", "&7rewards.", "", "&8Also accessible via /skills.", "", "&eClick to view!"),
                lore(icons, 19));
        assertEquals(List.of("&7View all of the items available in", "&7SkyBlock. Collect more of an item to", "&7unlock rewards on your way to",
                "&7becoming a master of SkyBlock!", "", "&7Collections Unlocked: &e0&6%", "&f&l&m                         &r &e0&6/&e83", "",
                "&8Also accessible via /collection.", "", "&eClick to view!"), lore(icons, 20));
        assertEquals(List.of("&7Through your adventure, you will", "&7unlock recipes for all kinds of", "&7special items! You can view how to",
                "&7craft these items here.", "", "&7Recipes Unlocked: &e10.9&6%", "&2&l&m   &f&l&m                      &r &e111&6/&e1k", "",
                "&8Also accessible via /recipes.", "", "&eClick to view!"), lore(icons, 21));
        assertEquals(List.of("&7Your SkyBlock Level: &8[&70&8]", "", "&7Determine how far you've", "&7progressed in SkyBlock and earn",
                "&7rewards from completing unique", "&7tasks.", "", "&7Progress to Level 1:", "&f&l&m                         &r &b0&3/&b100 XP", "",
                "&8Also accessible via /levels", "", "&eClick to view!"), lore(icons, 22));
        assertEquals(new Icon(Material.GRAY_DYE, "&cPets", "&7View and manage all of your Pets.", "", "&cFind your first pet to unlock!"),
                icons.get(30));
        assertEquals(new Icon(Material.GRAY_DYE, "&cPersonal Bank", "&7Contact your Banker from anywhere.", "",
                "&cRequires &aEmerald Collection VI"), icons.get(33));
        assertEquals(List.of("&7Teleport to islands you've already", "&7visited.", "", "&8Also accessible via /warp", "",
                "&cYou haven't unlocked this yet!"), lore(icons, 47));
        assertEquals(List.of("&7You can have multiple SkyBlock", "&7profiles at the same time.", "", "&7Each profile has its own island,",
                "&7inventory, quest log...", "", "&7Profiles: &e4&6/&e4", "&7Playing on: &aLemon", "", "&bPlay with friends using /coopadd <name>!",
                "", "&8Also accessible via /profiles", "", "&eClick to view!"), lore(icons, 48));
    }

    @Test
    void levelledProfile() {
        Map<Integer, Icon> icons = SkyBlockMenu.icons(levelled());
        assertEquals(List.of("&7View your equipment, stats,", "&7achievements, and more!", "", " &f✦ Speed 400", " &c❁ Strength &f589.25",
                " &a❈ Defense &f744.5", " &9☠ Crit Damage &f834.03%", " &9☣ Crit Chance &f140.5%", " &c❤ Health &f2,206",
                " &b✎ Intelligence &f692", " &8and more...", "", "&8Also accessible via /stats", "", "&eClick to view!"), lore(icons, 13));
        assertEquals(List.of("&7View your Skill progression and", "&7rewards.", "", "&621.6 Skill Avg. &8(non-cosmetic)", "",
                "&8Also accessible via /skills.", "", "&eClick to view!"), lore(icons, 19));
        // 61 of 83 collections found, 492 of the book's recipes unlocked (Banana's, 00:18.0).
        assertEquals(List.of("&7View all of the items available in", "&7SkyBlock. Collect more of an item to", "&7unlock rewards on your way to",
                "&7becoming a master of SkyBlock!", "", "&7Collections Unlocked: &e73.5&6%", "&2&l&m                   &f&l&m      &r &e61&6/&e83",
                "", "&8Also accessible via /collection.", "", "&eClick to view!"), lore(icons, 20));
        assertEquals(List.of("&7Through your adventure, you will", "&7unlock recipes for all kinds of", "&7special items! You can view how to",
                "&7craft these items here.", "", "&7Recipes Unlocked: &e48.4&6%", "&2&l&m             &f&l&m            &r &e492&6/&e1k", "",
                "&8Also accessible via /recipes.", "", "&eClick to view!"), lore(icons, 21));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aSkyBlock Leveling", List.of("&7Your SkyBlock Level: &8[&e88&8]", "",
                "&7Determine how far you've", "&7progressed in SkyBlock and earn", "&7rewards from completing unique", "&7tasks.", "",
                "&7Progress to Level 89:", "&3&l&m         &f&l&m                &r &b34&3/&b100 XP", "", "&8Also accessible via /levels", "",
                "&eClick to view!"), SkyBlockMenu.LEVELING_HEAD), icons.get(22));
        assertEquals(List.of("&7View the SkyBlock Calendar, upcoming", "&7events, and event rewards!", "", "&7Date: &a25th Autumn 516", "",
                "&8Also accessible via /calendar", "", "&eClick to view!"), lore(icons, 24));
        assertEquals("&7Playing on: &aBanana", lore(icons, 48).get(7));
    }

    /** These don't change with the profile: as recorded in both. */
    @Test
    void theSameForEveryone() {
        for (SkyBlockMenu.View view : List.of(FRESH, levelled())) {
            Map<Integer, Icon> icons = SkyBlockMenu.icons(view);
            assertEquals(new Icon(Material.WRITABLE_BOOK, "&aQuests & Chapters", "&7Each island has its own series of",
                    "&bChapters &7for you to complete!", "", "&7Complete tasks within a Chapter to", "&7earn small &6rewards&7, or complete",
                    "&7entire Chapters to earn big ones!", "", "&7Some islands also have &aQuests &7for", "&7you to complete! Some items can only",
                    "&7be obtained through Quests.", "", "&eClick to view!"), icons.get(23));
            assertEquals(new Icon(Material.CHEST, "&aStorage", "&7Store global items that you want to", "&7access at any time from anywhere",
                    "&7here.", "", "&8Also accessible via /storage", "", "&eClick to view!"), icons.get(25));
            assertEquals(new Icon(Material.PLAYER_HEAD, "&aYour Bags", List.of("&7Different bags allow you to store",
                    "&7many different items inside!", "", "&8Also accessible via /bags", "", "&eClick to open!"), SkyBlockMenu.BAGS_HEAD),
                    icons.get(29));
            assertEquals(new Icon(Material.CRAFTING_TABLE, "&aCrafting Table", "&7Opens the crafting grid.", "", "&8Also accessible via /craft",
                    "", "&eClick to open!"), icons.get(31));
            assertEquals(new Icon(Material.BARREL, "&aLoadouts", "&7View and edit preset armor and", "&7equipment sets with other settings to",
                    "&7make switching activities easy.", "", "&8Also accessible via /loadouts", "", "&eClick to view!"), icons.get(32));
            assertEquals(new Icon(Material.REDSTONE_TORCH, "&aSettings", "&7View and edit your SkyBlock settings.", "",
                    "&8Also accessible via /viewsettings.", "", "&eClick to view!"), icons.get(50));
        }
    }

    /** The heads' skins, as the recording's packets have them (Banana's and Lemon's menus alike). */
    @Test
    void heads() {
        assertEquals("1a11a7f11bcd5784903c5201d08261c4df8379109d6e611c1cd3ededf031afed", SkyBlockMenu.icons(FRESH).get(29).texture());
        assertEquals("3255327dd8e90afad681a19231665bea2bd06065a09d77ac1408837f9e0b242", SkyBlockMenu.icons(FRESH).get(22).texture());
        assertEquals("35f4b40cef9e017cd4112d26b62557f8c1d5b189da2e99534222bc8cec7d9196", SkyBlockMenu.icons(FRESH).get(47).texture());
    }

    @Test
    void levels() {
        assertEquals(0, SkyBlockMenu.level(99));
        assertEquals(1, SkyBlockMenu.level(100));
        assertEquals(88, SkyBlockMenu.level(8834));
        // Grey to 39, then a colour every 40 levels, dark red from 480 (the wiki's Prefix Color).
        assertEquals("&7", SkyBlockMenu.levelColor(39));
        assertEquals("&f", SkyBlockMenu.levelColor(40));
        assertEquals("&e", SkyBlockMenu.levelColor(88));
        assertEquals("&6", SkyBlockMenu.levelColor(400));
        assertEquals("&4", SkyBlockMenu.levelColor(480));
        assertEquals("&4", SkyBlockMenu.levelColor(600));
    }
}
