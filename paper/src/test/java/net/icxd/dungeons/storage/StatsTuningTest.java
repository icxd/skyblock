package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** Stats Tuning: what points give (the wiki's Maxwell/UI "Per point"), how many count, and its menu. */
class StatsTuningTest {
    @Test
    void pointsGiveStats() {
        Document profile = new Document();
        assertEquals(new Stats(), StatsTuning.stats(profile, 57));
        StatsTuning.set(profile, StatsTuning.Tuned.HEALTH, 10);
        StatsTuning.set(profile, StatsTuning.Tuned.CRIT_CHANCE, 5);
        StatsTuning.set(profile, StatsTuning.Tuned.ATTACK_SPEED, 10);
        StatsTuning.set(profile, StatsTuning.Tuned.SPEED, 2);
        assertEquals(27, StatsTuning.assigned(profile));
        Stats stats = StatsTuning.stats(profile, 57);
        assertEquals(50, stats.get(Stat.HEALTH), 1e-9);
        assertEquals(1, stats.get(Stat.CRIT_CHANCE), 1e-9);
        assertEquals(3, stats.get(Stat.ATTACK_SPEED), 1e-9);
        assertEquals(3, stats.get(Stat.SPEED), 1e-9);
        // Taking them all out leaves none.
        StatsTuning.set(profile, StatsTuning.Tuned.SPEED, 0);
        assertEquals(25, StatsTuning.assigned(profile));
        assertEquals(0, StatsTuning.points(profile, StatsTuning.Tuned.SPEED));
    }

    /** With fewer points than put in, only as many count, in the menu's order. */
    @Test
    void onlyThePointsTheyHave() {
        Document profile = new Document();
        StatsTuning.set(profile, StatsTuning.Tuned.HEALTH, 10);
        StatsTuning.set(profile, StatsTuning.Tuned.INTELLIGENCE, 10);
        Stats stats = StatsTuning.stats(profile, 12);
        assertEquals(50, stats.get(Stat.HEALTH), 1e-9);
        assertEquals(4, stats.get(Stat.INTELLIGENCE), 1e-9);
        assertEquals(new Stats(), StatsTuning.stats(profile, 0));
    }

    /** A click puts in 1 or 10, as many as are left; takes out 1 or 10, as many as it has. */
    @Test
    void clicks() {
        assertEquals(1, StatsTuning.change(0, 5, 1));
        assertEquals(5, StatsTuning.change(0, 5, 10));
        assertEquals(0, StatsTuning.change(0, 0, 1));
        assertEquals(-1, StatsTuning.change(3, 0, -1));
        assertEquals(-3, StatsTuning.change(3, 0, -10));
        assertEquals(0, StatsTuning.change(0, 5, -1));
    }

    /** The wiki's Health item with none put in, and with some; the Stats Tuning item's unassigned points. */
    @Test
    void menu() {
        Document profile = new Document();
        Stats stats = new Stats().set(Stat.HEALTH, 100).set(Stat.CRIT_CHANCE, 30);
        Map<Integer, Icon> icons = StatsTuningMenu.icons(profile, 0, stats);
        assertEquals(new Icon(Material.GOLDEN_APPLE, "&c❤ Health", "&7Your Health stat increases your", "&7maximum health.", "",
                "&7You have: &c100", "&8&m------------------------", "&7Stat has: &c0 points", "&7Per point: &c+5❤", "&7Points left: 0", "",
                "&eRight-Click to add a point!", "&8Hold shift for 10 points!"), icons.get(19));
        assertEquals("&7You have: &930%", icons.get(29).lore().get(4));
        assertEquals("&7Per point: &9+0.2☣", icons.get(29).lore().get(7));
        assertEquals(new Icon(Material.COMPARATOR, "&aStats Tuning", "&7Optimize your build to your liking by using", "&eTuning Points&7.", "",
                "&7Every &610 MP &7grants &e1 Tuning Point&7.", "", "&7Magical Power: &60", "&7Tuning Points: &e0"), icons.get(StatsTuningMenu.INFO));

        StatsTuning.set(profile, StatsTuning.Tuned.HEALTH, 10);
        icons = StatsTuningMenu.icons(profile, 571, new Stats().set(Stat.HEALTH, 150));
        assertEquals(List.of("&7You have: &c100 &7+ &c50 ❤", "&8&m------------------------", "&7Stat has: &c10 points", "&7Per point: &c+5❤",
                "&7Points left: 47", "", "&eRight-Click to add a point!", "&eLeft-Click to remove a point!", "&8Hold shift for 10 points!"),
                icons.get(19).lore().subList(3, 12));
        assertEquals("&7Unassigned Points: &c47!!!", icons.get(StatsTuningMenu.INFO).lore().get(7));
        assertEquals("&7Unassigned Points: &c47!!!", LoadoutsMenu.icons(0, null, 571, 10, PrivateTables.madeUp(Map.of()))
                .get(LoadoutsMenu.STATS_TUNING).lore().get(7));
    }
}
