package net.icxd.dungeons.leveling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.leveling.LevelingData.Emblem;
import net.icxd.dungeons.leveling.LevelingData.EmblemCategory;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.skill.Skill;

/** The SkyBlock XP Guide against the recorded Starter and Amateur (01:51.3, 01:49.8), and the emblems (02:10.1). */
class GuideAndEmblemsTest {
    /** Every skill but the cosmetic ones at this level. */
    private static Document skills(int level) {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) if (!skill.cosmetic()) levels.put(skill, Math.min(level, skill.maxLevel()));
        return Fixtures.profile(levels, 0, Map.of());
    }

    @Test
    void freshStarter() {
        LevelingData data = Fixtures.recorded();
        LevelingView view = Fixtures.view(data, new Document(), Fixtures.NONE);
        assertEquals(0, GuideMenu.current(view));
        Map<Integer, Icon> icons = GuideMenu.icons(view, 0);
        assertEquals(new Icon(Material.FILLED_MAP, "&aStarter", "&8New Player", "", "&7You are starting on your journey",
                "&7through SkyBlock. Complete these", "&7tasks to get acquainted with the game.", "", "&7Total Progress: &a0%",
                "&f&l&m                         &r &a0&8/&a128", "", "&a&lSELECTED"), icons.get(1));
        assertEquals(new Icon(Material.GRAY_DYE, "&bAmateur", "&8Very Early Game", "", "&7Reach &a50% &7completion of &aStarter &7to",
                "&7unlock this stage!", "", "&c&lLOCKED"), icons.get(2));
        assertEquals(new Icon(Material.GRAY_DYE, "&dMaster", "&8End Game", "", "&7Reach &a50% &7completion of",
                "&6Professional &7to unlock this stage!", "", "&c&lLOCKED"), icons.get(7));
        // The recorded order, all undone.
        assertEquals("&c✖ &aMuseum Donations", icons.get(19).name());
        assertEquals(List.of("&862 tasks", "", "&7Donate items and armor sets to the", "&7Museum.", "", "&7Total Worth: &b+82 XP", "",
                "&7Total Progress: &a0%", "&f&l&m                         &r &a0&8/&a62", "", "&eClick to view more!"), icons.get(19).lore());
        assertEquals("&c✖ &aSkills", icons.get(25).name());
        assertEquals(List.of("&7Defeat &5Revenant Horror &7to gain", "&7Slayer XP and reach &aZombie Slayer", "&aLVL 1&7.", "",
                "&7Total Worth: &b+15 XP", "", "&7Progress to LVL 1: &a0%", "&f&l&m                         &r &a0&8/&a1"), icons.get(28).lore());
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Leveling"), icons.get(48));
        assertEquals(Material.PAPER, icons.get(50).material());
    }

    /** Every skill at IV: Skills done, as Banana's, and after the undone ones. */
    @Test
    void starterSkillsDone() {
        LevelingData data = Fixtures.recorded();
        LevelingView view = Fixtures.view(data, skills(4), Fixtures.NONE);
        Map<Integer, Icon> icons = GuideMenu.icons(view, 0);
        Icon skills = icons.get(28);
        assertEquals("&a✔ &aSkills", skills.name());
        assertEquals(List.of("&810 tasks", "", "&7Level up your Skills.", "", "&a ✔ &8Farming Skill IV", "&a ✔ &8Mining Skill IV",
                "&a ✔ &8Combat Skill IV", "&a ✔ &8Foraging Skill IV", "&a ✔ &8Fishing Skill IV", "&a ✔ &8Enchanting Skill IV",
                "&a ✔ &8Alchemy Skill IV", "&a ✔ &8Carpentry Skill IV", "&a ✔ &8Taming Skill IV", "&a ✔ &8Hunting Skill IV", "",
                "&7Total Worth: &b+200 XP", "", "&7Total Progress: &a100%", "&a&l&m                         &r &a10&8/&a10", "",
                "&eClick to view more!"), skills.lore());
        // The Zombie Slayer comes before it now: it's undone.
        assertEquals("&c✖ &aZombie LVL 1", icons.get(25).name());
        assertEquals(List.of("&7Total Progress: &a7.8%", "&a&l&m  &f&l&m                       &r &a10&8/&a128"),
                icons.get(1).lore().subList(6, 8));
        assertFalse(GuideMenu.unlocked(view, 1));
    }

    /** The Amateur stage's items, as recorded, when it's open (half the Starter done: here, made up). */
    @Test
    void amateur() {
        LevelingData data = Fixtures.recorded();
        LevelingView view = Fixtures.view(data, skills(12), Fixtures.NONE);
        Map<Integer, Icon> icons = GuideMenu.icons(view, 1);
        assertEquals("&c✖ &8[&7Lv300&8] &cArachne", icons.get(25).name());
        assertEquals(List.of("&7Kill &8[&7Lv300&8] &cArachne &7in the &cSpider's", "&cDen&7.", "", "&7Total Worth: &b+20 XP"), icons.get(25).lore());
        List<String> mobs = icons.get(24).lore();
        assertEquals("&c ✖ &f&5⊙ Ender", mobs.get(4));
    }

    @Test
    void stagesUnlock() {
        LevelingData data = Fixtures.fixture();
        // Two of four Starter tasks: the skills.
        LevelingView half = Fixtures.view(data, Fixtures.profile(Map.of(Skill.FARMING, 2, Skill.MINING, 2), 0, Map.of()), Fixtures.NONE);
        assertEquals(2, GuideMenu.done(half, data.stages().getFirst()));
        assertTrue(GuideMenu.unlocked(half, 1));
        assertEquals(1, GuideMenu.current(half));
        LevelingView less = Fixtures.view(data, Fixtures.profile(Map.of(Skill.FARMING, 2), 0, Map.of()), Fixtures.NONE);
        assertFalse(GuideMenu.unlocked(less, 1));
        // The Entrance done counts its one task; the made-up Museum can't be told.
        LevelingView entrance = Fixtures.view(data, Fixtures.profile(Map.of(Skill.FARMING, 2), 0, Map.of(), DungeonFloor.ENTRANCE), Fixtures.NONE);
        assertEquals(2, GuideMenu.done(entrance, data.stages().getFirst()));
        Map<Integer, Icon> icons = GuideMenu.icons(entrance, 0);
        // Undone first: Skills (one of two), Museum; then the Entrance.
        assertEquals("&c✖ &aSkills", icons.get(19).name());
        assertEquals("&c✖ &aMuseum", icons.get(20).name());
        assertEquals("&a✔ &aEntrance", icons.get(21).name());
        assertEquals(List.of("&7Complete it.", "", "&7Total Worth: &b+5 XP"), icons.get(21).lore());
        assertEquals(List.of("&7Donate.", "", "&7Total Worth: &b+5 XP", "", "&7Progress to Tier I: &a0%",
                "&f&l&m                         &r &a0&8/&a3"), icons.get(20).lore());
        // A stage not recorded wraps the wiki's description.
        Icon amateur = GuideMenu.stage(half, 1, false);
        assertEquals(List.of("&8Early", "", "&7Then here.", "", "&7Total Progress: &b0%", "&f&l&m                         &r &b0&8/&b1", "",
                "&eClick to select!"), amateur.lore());
    }

    @Test
    void emblems() {
        LevelingData data = Fixtures.recorded();
        // Level 88 (all levelling emblems to Star), Mining 50, Catacombs 12.
        Document profile = Fixtures.profile(Map.of(Skill.MINING, 50), 12, Map.of());
        LevelingView view = new LevelingView(data, Fixtures.NONE, profile, new SkyBlockXp.Breakdown(Map.of(), 8834), view(data, profile).mode(),
                view(data, profile).rank(), "ICoding", true);
        Map<Integer, Icon> icons = EmblemsMenu.icons(view);
        assertEquals(new Icon(Material.DIAMOND_SWORD, "&aSkills", "&81 Unlocked", "", "&7These symbols are related to",
                "&7leveling up your Skills!", "", "&eClick to view!"), icons.get(10));
        assertEquals(List.of("&80 Unlocked", "", "&7These symbols are related to", "&7completing Dungeons.", "", "&eClick to view!"),
                icons.get(11).lore());
        assertEquals(List.of("&88 Unlocked", "", "&7These symbols are unlocked by", "&7leveling up your SkyBlock Level.", "", "&eClick to view!"),
                icons.get(12).lore());
        assertEquals("&aDiscord", icons.get(16).name());
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Leveling"), icons.get(30));

        EmblemCategory leveling = EmblemListMenu.category(data, "leveling");
        assertEquals("Emblems - Leveling (8/21)", EmblemListMenu.title(leveling, 8));
        Map<Integer, Icon> list = EmblemListMenu.icons(view, leveling, 0, "diamond");
        Icon diamond = list.get(10);
        assertEquals("&fDiamond &7♦", diamond.name());
        assertEquals(List.of("&7Preview: &8[&e88&8] &7♦ §b[MVP§6+§b] ICoding", "", "&a&lSELECTED", "&eClick to remove!"), diamond.lore());
        assertEquals(List.of("&7Requires: &cSkyBlock Level 90", "", "&c&lLOCKED"), list.get(18 + 1 + 1).lore());
        assertNull(list.get(EmblemListMenu.NEXT));
        // Achievement's 37 take two pages.
        EmblemCategory achievements = EmblemListMenu.category(data, "achievement");
        assertEquals(28, EmblemListMenu.page(achievements, 0).size());
        assertEquals(9, EmblemListMenu.page(achievements, 1).size());
        assertEquals("&aNext Page", EmblemListMenu.icons(view, achievements, 0, null).get(EmblemListMenu.NEXT).name());
        assertEquals(0, EmblemListMenu.index(10));
        assertEquals(7, EmblemListMenu.index(19));
        assertEquals(-1, EmblemListMenu.index(17));
    }

    /** The chosen emblem shows while it's unlocked; per profile. */
    @Test
    void chosenEmblem() {
        LevelingData data = Fixtures.fixture();
        Document profile = new Document();
        assertNull(Emblems.shown(data, profile, 5, Fixtures.NONE));
        Emblems.choose(profile, "dot");
        assertEquals("dot", Emblems.chosen(profile));
        Emblem dot = Emblems.shown(data, profile, 5, Fixtures.NONE);
        assertEquals("&7•", dot.symbol());
        // Locked again (the level's lower on this data): nothing shows.
        assertNull(Emblems.shown(data, profile, 2, Fixtures.NONE));
        // One the plugin can't tell is never unlocked.
        Emblems.choose(profile, "brain");
        assertNull(Emblems.shown(data, profile, 500, Fixtures.NONE));
        Emblems.choose(profile, null);
        assertNull(Emblems.chosen(profile));
    }

    private static LevelingView view(LevelingData data, Document profile) {
        return Fixtures.view(data, profile, Fixtures.NONE);
    }
}
