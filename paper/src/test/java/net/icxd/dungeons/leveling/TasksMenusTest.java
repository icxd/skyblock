package net.icxd.dungeons.leveling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.skill.Skill;

/** Ways to Level Up against the recorded one (01:46.1), and the task lists as the wiki's copies of them. */
class TasksMenusTest {
    /** Banana's XP in each category (the recorded progress lines), put on one task of each. */
    private static LevelingView banana(LevelingData data) {
        Map<String, Integer> tasks = Map.of("skill_level_up", 4031, "spooky_festival", 100, "catacombs_level_up", 900, "slayer_level_up", 1868,
                "heart_of_the_mountain", 830, "the_dojo", 135, "rift_guide", 152, "metaphysical_serum", 9);
        int total = tasks.values().stream().mapToInt(Integer::intValue).sum();
        return new LevelingView(data, Fixtures.NONE, new Document(), new SkyBlockXp.Breakdown(tasks, total), ProfileMode.NORMAL, Rank.MVP_PLUS,
                "ICoding", true);
    }

    @Test
    void waysToLevelUp() {
        LevelingData data = Fixtures.recorded();
        Map<Integer, Icon> icons = WaysMenu.icons(banana(data), List.of());
        assertEquals(Set.of(11, 12, 13, 14, 15, 20, 21, 23, 24, 37, 38, 39, 40, 41, 42, 48, 50), icons.keySet());
        assertEquals(new Icon(Material.NETHER_STAR, "&3Core Tasks", "&89 Tasks", "", "&7All of the core XP tasks that are", "&7integral to your general",
                "&7progression in SkyBlock!", "", "&e▶ Skill Level Up", "&e▶ Museum Donations", "&e▶ Fairy Souls", "&e▶ Accessory Bag",
                "&e▶ Pet Score", "&e▶ Collections", "&e▶ Craft Minions", "&e▶ Bank Upgrades", "&e▶ Fast Travels Unlocked", "",
                "&7Progress to Complete Category: &620.3%", "&6&l&m      &f&l&m                   &r &e4,031&6/&e19,810 XP", "",
                "&eClick to view tasks!"), icons.get(11));
        List<String> dungeon = icons.get(13).lore();
        assertEquals(List.of("&83 Tasks", "", "&7All Dungeon related XP Tasks.", "", "&e▶ Catacombs Level Up", "&e▶ Class Level Up",
                "&e▶ Complete Dungeons", "", "&7Progress to Complete Category: &632.6%",
                "&6&l&m         &f&l&m                &r &e900&6/&e2,760 XP", "", "&eClick to view tasks!"), dungeon);
        assertEquals(Material.PLAYER_HEAD, icons.get(13).material());
        assertEquals(List.of("&7Progress to Complete Category: &60%", "&f&l&m                         &r &e0&6/&e2,121 XP"),
                icons.get(14).lore().subList(icons.get(14).lore().size() - 4, icons.get(14).lore().size() - 2));
        assertEquals(List.of("&7Progress to Complete Category: &615.2%", "&6&l&m    &f&l&m                     &r &e1,868&6/&e12,315 XP"),
                tail(icons.get(15)));
        assertEquals(List.of("&7Progress to Complete Category: &67.1%", "&6&l&m  &f&l&m                       &r &e830&6/&e11,732 XP"),
                tail(icons.get(20)));
        assertEquals(List.of("&7Progress to Complete Category: &62.2%", "&6&l&m &f&l&m                        &r &e135&6/&e6,266 XP"),
                tail(icons.get(21)));
        assertEquals(List.of("&7Progress to Complete Category: &613.4%", "&6&l&m    &f&l&m                     &r &e152&6/&e1,132 XP"),
                tail(icons.get(23)));
        assertEquals(List.of("&7Progress to Complete Category: &63%", "&6&l&m &f&l&m                        &r &e9&6/&e299 XP"),
                tail(icons.get(24)));
        assertEquals(new Icon(Material.OAK_SIGN, "&bRecently Viewed ➜", "&7When you view XP Tasks, they will", "&7show up here for your convenience!"),
                icons.get(37));
        assertEquals(new Icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&bRecently Viewed #1"), icons.get(38));
        assertEquals(new Icon(Material.GRAY_DYE, "&aShow Progress Bars", "&7Toggle whether to display more", "&7detailed task progress in each",
                "&7category.", "", "&eClick to show!"), icons.get(50));
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Leveling"), icons.get(48));
    }

    private static List<String> tail(Icon icon) {
        List<String> lore = icon.lore();
        return lore.subList(lore.size() - 4, lore.size() - 2);
    }

    @Test
    void recentlyViewed() {
        UUID player = UUID.randomUUID();
        for (String id : List.of("core", "dungeon", "event", "core", "story", "slaying", "consumables")) WaysMenu.viewed(player, id);
        assertEquals(List.of("consumables", "slaying", "story", "core", "event"), WaysMenu.viewed(player));
        LevelingData data = Fixtures.recorded();
        Map<Integer, Icon> icons = WaysMenu.icons(banana(data), WaysMenu.viewed(player));
        assertEquals("&3Consumables Tasks", icons.get(38).name());
        assertEquals("&3Event Tasks", icons.get(42).name());
        WaysMenu.forget(player);
        assertEquals(List.of(), WaysMenu.viewed(player));
    }

    /** The wiki's layouts: Dungeon's three every other slot, Complete Dungeons' two, the Essence Shop's six, Core's eight. */
    @Test
    void layouts() {
        assertEquals(List.of(22), box(TasksMenu.slots(1)));
        assertEquals(List.of(21, 23), box(TasksMenu.slots(2)));
        assertEquals(List.of(20, 22, 24), box(TasksMenu.slots(3)));
        assertEquals(List.of(20, 21, 22, 23, 24), box(TasksMenu.slots(5)));
        assertEquals(List.of(19, 20, 21, 23, 24, 25), box(TasksMenu.slots(6)));
        assertEquals(List.of(20, 21, 22, 23, 24, 30, 31, 32), box(TasksMenu.slots(8)));
        assertEquals(5, TasksMenu.rows(3));
        assertEquals(6, TasksMenu.rows(8));
        assertEquals(6, TasksMenu.rows(28));
        // 28 fill four rows of seven from the second.
        int[] most = TasksMenu.slots(28);
        assertEquals(10, most[0]);
        assertEquals(43, most[27]);
        assertEquals(28, Arrays.stream(TasksMenu.slots(40)).distinct().count());
    }

    @Test
    void titles() {
        LevelingData data = Fixtures.recorded();
        assertEquals("Tasks ➜ Core", TasksMenu.title(data, "core", null));
        assertEquals("Core ➜ Bank Upgrades", TasksMenu.title(data, "core", "bank_upgrades"));
        assertEquals("Complete Dungeons ➜ Complete The Catacombs",
                TasksMenu.title(data, "dungeon", "complete_dungeons.complete_the_catacombs"));
        assertEquals("Tasks ➜ Skill Related", TasksMenu.title(data, "skill_related", null));
    }

    /** A task list as the wiki's (Tasks ➜ Dungeon): the category on top, its tasks, and Go Back, Sort at the bottom. */
    @Test
    void dungeonTasks() {
        LevelingData data = Fixtures.recorded();
        Document profile = Fixtures.profile(Map.of(Skill.COMBAT, 12), 3, Map.of(DungeonClass.MAGE, 2), DungeonFloor.ENTRANCE);
        LevelingView view = Fixtures.view(data, profile, Fixtures.NONE);
        Map<Integer, Icon> icons = TasksMenu.icons(view, "dungeon", null, TasksMenu.Sort.UNLOCKED);
        assertEquals(Set.of(4, 20, 22, 24, 39, 41), icons.keySet());
        assertEquals(List.of("&83 Tasks", "", "&7All Dungeon related XP Tasks.", "", "&e▶ Catacombs Level Up", "&e▶ Class Level Up",
                "&e▶ Complete Dungeons", "", "&7Progress to Complete Category: &63.2%",
                "&6&l&m &f&l&m                        &r &e88&6/&e2,760 XP"), icons.get(4).lore());
        assertEquals(List.of("&8XP Task", "", "&7Gain XP by leveling up your", "&7Catacombs level!", "&7Level 1-39: &b+20 XP",
                "&7Level 40-50: &b+40 XP", "", "&7Total Progress: &34.9%", "&3&l&m  &f&l&m                       &r &b60&3/&b1,220 XP", "",
                "&8This task is worth &338%&8 of", "&8your Total SkyBlock XP!"), icons.get(20).lore());
        assertEquals("&aComplete Dungeons", icons.get(24).name());
        assertEquals(List.of("&82 Tasks", "", "  &c✖ &eComplete The Catacombs&8 (1/8)", "  &c✖ &eComplete Master Mode Catacombs&8 (0/7)", "",
                "&7Total Progress: &33.7%", "&3&l&m &f&l&m                        &r &b20&3/&b540 XP", "", "&8This task is worth &312.7%&8 of",
                "&8your Total SkyBlock XP!", "", "&eClick to view tasks!"), icons.get(24).lore());
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To Ways to Level Up"), icons.get(39));
        assertEquals(new Icon(Material.HOPPER, "&aSort", "&7Change how Tasks are sorted.", "", "&b▶ Unlocked", "&7Earned XP", "&7Available XP",
                "&7A to Z", "&7Z to A", "", "&eClick to switch sort!"), icons.get(41));

        // Its parts, and theirs: the Entrance done.
        Map<Integer, Icon> parts = TasksMenu.icons(view, "dungeon", "complete_dungeons.complete_the_catacombs", TasksMenu.Sort.UNLOCKED);
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To Dungeon ➜ Complete Dungeons"), parts.get(TasksMenu.back(54)));
        Icon entrance = parts.get(20);
        assertEquals("&aComplete Catacombs Entrance", entrance.name());
        assertEquals(List.of("&8XP Task", "", "&b+20 XP", "", "&7Total Progress: &3100%", "&3&l&m                         &r &b20&3/&b20 XP", "",
                "&8This task is worth &312.7%&8 of", "&8your Total SkyBlock XP!"), entrance.lore());
    }

    @Test
    void sorting() {
        LevelingData data = Fixtures.fixture();
        Document profile = Fixtures.profile(Map.of(Skill.COMBAT, 5), 6, Map.of());
        LevelingView view = Fixtures.view(data, profile, Fixtures.NONE);
        List<String> ids = TasksMenu.sorted(view, data.category("dungeon").tasks(), TasksMenu.Sort.EARNED).stream().map(LevelingData.Task::id).toList();
        assertEquals("catacombs_level_up", ids.getFirst());
        assertEquals(List.of("catacombs_level_up", "class_level_up", "complete_dungeons"),
                TasksMenu.sorted(view, data.category("dungeon").tasks(), TasksMenu.Sort.A_TO_Z).stream().map(LevelingData.Task::id).toList());
        assertEquals(List.of("complete_dungeons", "class_level_up", "catacombs_level_up"),
                TasksMenu.sorted(view, data.category("dungeon").tasks(), TasksMenu.Sort.Z_TO_A).stream().map(LevelingData.Task::id).toList());
        // Available: Catacombs has 100 - 21, classes 50, completions 40.
        assertEquals(List.of("catacombs_level_up", "class_level_up", "complete_dungeons"),
                TasksMenu.sorted(view, data.category("dungeon").tasks(), TasksMenu.Sort.AVAILABLE).stream().map(LevelingData.Task::id).toList());
        assertEquals(TasksMenu.Sort.EARNED, TasksMenu.Sort.UNLOCKED.next());
        assertEquals(TasksMenu.Sort.UNLOCKED, TasksMenu.Sort.Z_TO_A.next());
    }

    private static List<Integer> box(int[] slots) {
        return Arrays.stream(slots).boxed().toList();
    }
}
