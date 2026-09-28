package net.icxd.dungeons.leveling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.utils.Text;

/** SkyBlock Leveling's bars, percentages, rewards' states and chat, against the recorded menus and real chat logs. */
class LevelingTextTest {
    /** Level 89's bar in the recorded SkyBlock Leveling: 34 of 100 in the level's yellow. */
    @Test
    void bars() {
        assertEquals("&e&l&m         &f&l&m                &r", LevelingText.strip("&e", 0.34));
        assertEquals("&f&l&m                         &r", LevelingText.strip("&7", 0));
        assertEquals("&3&l&m                         &r", LevelingText.strip("&3", 1));
    }

    @Test
    void percentages() {
        assertEquals("44.4", LevelingText.percent(24 / 54.0));
        assertEquals("0", LevelingText.percent(0));
        assertEquals("100", LevelingText.percent(1));
        assertEquals("0", LevelingText.percent(-1));
    }

    /** The recorded Rewards and Level N Rewards menus, at 8,834 XP. */
    @Test
    void states() {
        assertEquals(List.of("&a&lUNLOCKED"), LevelingText.state(88, 8834));
        assertEquals(List.of("&7Levels left to Unlock: &32", "&3&l&m     &f&l&m                    &r &334&b/&3200 XP"),
                LevelingText.state(90, 8834));
        assertEquals(List.of("&7Progress to Unlock: &388.3%", "&3&l&m                      &f&l&m   &r &388&b/&3100"),
                LevelingText.state(100, 8834));
        assertEquals(List.of("&7Progress to Unlock: &373.6%", "&3&l&m                   &f&l&m      &r &388&b/&3120"),
                LevelingText.state(120, 8834));
        assertEquals(List.of("&7Progress to Unlock: &344.2%", "&3&l&m           &f&l&m              &r &388&b/&3200"),
                LevelingText.state(200, 8834));
    }

    @Test
    void unlocked() {
        assertEquals(List.of("&7Rewards Unlocked: &375%", "&3&l&m                   &f&l&m      &r &39&b/&312"),
                LevelingText.unlocked("Rewards Unlocked", 9, 12));
    }

    /** Wrapped as the recorded ranking item and bonuses are. */
    @Test
    void wrapping() {
        assertEquals(List.of("&7You have completed &315.2%&7 of the", "&7total SkyBlock XP Tasks."),
                LevelingText.wrap("&7You have completed &315.2%&7 of the total SkyBlock XP Tasks."));
        assertEquals(List.of("&7You have completed &30%&7 of the total", "&7SkyBlock XP Tasks."),
                LevelingText.wrap("&7You have completed &30%&7 of the total SkyBlock XP Tasks."));
        assertEquals(List.of("&7Reach &a50% &7completion of &bAmateur &7to", "&7unlock this stage!"),
                LevelingText.wrap("&7Reach &a50% &7completion of &bAmateur &7to unlock this stage!"));
        assertEquals(List.of("&7Reach &a50% &7completion of", "&6Professional &7to unlock this stage!"),
                LevelingText.wrap("&7Reach &a50% &7completion of &6Professional &7to unlock this stage!"));
    }

    /** A real chat log's (2024-11-26): Level 170 to 171, +5 Health, each line centred. */
    @Test
    void levelUp() {
        List<String> lines = LevelingText.levelUp(170, 171, List.of("&8+&a5 &c❤ Health"));
        assertEquals(List.of(" ",
                "&f                          &r&3&lSKYBLOCK LEVEL UP",
                "&f                             &r&2Level &r&8170 ➡ &r&8[&r&2171&r&8]",
                " ",
                "&f                                  &r&6&lREWARDS",
                "&f                                 &r&8+&a5 &c❤ Health",
                " "), lines);
    }

    @Test
    void actionBar() {
        assertEquals("&b+20 SkyBlock XP &7(Skill Level Up&7)&b (34/100)", LevelingText.actionBar(20, "Skill Level Up", 8834));
    }

    @Test
    void plain() {
        assertEquals("Core Tasks", LevelingText.plain("&3Core Tasks"));
        assertEquals(Text.width("Core Tasks"), Text.width(LevelingText.plain("§3§lCore Tasks")));
    }
}
