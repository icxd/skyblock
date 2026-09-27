package net.icxd.dungeons.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** Stats & Equipment against the recorded one (the SkyBlock Menu tour, 02:18.4; stat glyphs as classic symbols). */
class StatsMenuTest {
    /** The recording's player's stats, those this plugin has. */
    private static Stats recorded() {
        return new Stats().set(Stat.HEALTH, 2206).set(Stat.DEFENSE, 744.5).set(Stat.TRUE_DEFENSE, 33).set(Stat.STRENGTH, 579.25)
                .set(Stat.CRIT_CHANCE, 140.5).set(Stat.CRIT_DAMAGE, 834.03).set(Stat.ATTACK_SPEED, 5).set(Stat.FEROCITY, 7)
                .set(Stat.SWING_RANGE, 3).set(Stat.INTELLIGENCE, 692).set(Stat.ABILITY_DAMAGE, 17.5).set(Stat.HEALTH_REGEN, 153.5)
                .set(Stat.VITALITY, 104).set(Stat.MENDING, 100).set(Stat.MINING_SPEED, 665).set(Stat.FISHING_SPEED, 14)
                .set(Stat.SEA_CREATURE_CHANCE, 22.3).set(Stat.TREASURE_CHANCE, 1.9).set(Stat.SPEED, 400).set(Stat.MAGIC_FIND, 111.25)
                .set(Stat.PET_LUCK, 51).set(Stat.RESPIRATION, 90).set(Stat.PRESSURE_RESISTANCE, 30).set(Stat.HUNTING_FORTUNE, 8)
                .set(Stat.COMBAT_WISDOM, 72).set(Stat.FARMING_WISDOM, 46).set(Stat.FISHING_WISDOM, 47).set(Stat.MINING_WISDOM, 45);
    }

    @Test
    void recordedCategories() {
        Map<Integer, Icon> icons = StatsMenu.icons(recorded());
        assertEquals(new Icon(Material.STONE_SWORD, "&cCombat Stats", "&7Stats that influence how much", "&7damage you take and deal when in",
                "&7combat.", "", " &c❤ Health &f2,206", " &a❈ Defense &f744.5", " &f❂ True Defense 33", " &c❁ Strength &f579.25",
                " &9☣ Crit Chance &f140.5%", " &9☠ Crit Damage &f834.03%", " &e⚔ Attack Speed &f5%", " &c⫽ Ferocity &f7",
                " &eⓈ Swing Range &f3", " &b✎ Intelligence &f692", " &c๑ Ability Damage &f17.5%", " &c❣ Health Regen &f153.5",
                " &4♨ Vitality &f104", " &a☄ Mending &f100", "", "&eClick for details!"), icons.get(14));
        assertEquals(new Icon(Material.FISHING_ROD, "&bFishing Stats", "&7Stats that influence what you catch",
                "&7and how quickly you catch it while", "&7fishing.", "", " &b☂ Fishing Speed &f14", " &3α Sea Creature Chance &f22.3%",
                " &6⛃ Treasure Chance &f1.9%", "", "&eClick for details!"), icons.get(24));
        assertEquals(new Icon(Material.CLOCK, "&dMiscellaneous Stats", "&7Stats that augment various aspects", "&7of your gameplay.", "",
                " &f✦ Speed 400", " &b✯ Magic Find &f111.25", " &d♣ Pet Luck &f51", " &3⚶ Respiration &f90", " &9❍ Pressure Resistance &f30",
                "", "&eClick for details!"), icons.get(25));
        assertEquals(List.of("&7Stats that influence how much &3Skill", "&3XP &7you gain.", "", " &3☯ Combat Wisdom &f72",
                " &3☯ Farming Wisdom &f46", " &3☯ Fishing Wisdom &f47", " &3☯ Mining Wisdom &f45", "", "&eClick for details!"),
                icons.get(34).lore());
        assertEquals(" &6⸕ Mining Speed &f665", icons.get(15).lore().get(5));
        assertEquals(" &d☘ Hunting Fortune &f8", icons.get(32).lore().get(4));
    }

    /** A category with none of its stats: the wiki's words, and nothing to click for. */
    @Test
    void emptyCategory() {
        assertEquals(new Icon(Material.BOOK, "&3Wisdom Stats", "&7Stats that influence how much &3Skill", "&3XP &7you gain.", "",
                "&8You do not have any stats to show in", "&8this category!"), StatsMenu.icons(Stats.base()).get(34));
        assertEquals(List.of("&7Stats that influence how many drops", "&7you receive and how many pests", "&7spawn when farming.", "",
                "&8You do not have any stats to show in", "&8this category!"), StatsMenu.icons(Stats.base()).get(16).lore());
    }

    @Test
    void layout() {
        Map<Integer, Icon> icons = StatsMenu.icons(Stats.base());
        assertEquals(Set.of(2, 10, 11, 14, 15, 16, 19, 20, 23, 24, 25, 28, 29, 32, 34, 37, 38, 47, 50), icons.keySet());
        assertEquals(new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Empty Equipment Slot", " &8> Gloves", " &8> Bracelet"), icons.get(37));
        assertEquals(new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Empty Helmet Slot"), icons.get(11));
        assertEquals("&7Currently Active: &e0", icons.get(50).lore().get(6));
        // Each stat is in one category at most.
        Set<Stat> seen = new HashSet<>();
        for (StatsMenu.Category category : StatsMenu.Category.values()) {
            for (Stat stat : category.stats) assertEquals(true, seen.add(stat), stat + " twice");
        }
    }
}
