package net.icxd.dungeons.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The skill menus against the recorded ones (research skills.md 1.2, 1.3; stat glyphs as classic
 * symbols): the snake path, the view that opens, and the items' lore.
 */
class SkillMenuTest {
    private static final double COMBAT_24 = Skill.COMBAT.cumulative(24) + 272_514.2;

    /** The recorded spots: I at (3,1), V at (0,2), X at (3,4), XI at (3,5), XV at (0,6), XX at (3,8), LX at (3,24). */
    @Test
    void snake() {
        int[][] expected = {{1, 3, 1}, {5, 0, 2}, {10, 3, 4}, {11, 3, 5}, {15, 0, 6}, {20, 3, 8}, {25, 0, 10}, {60, 3, 24}};
        for (int[] e : expected) {
            assertEquals(e[1], SkillMenu.row(e[0]), "row of " + e[0]);
            assertEquals(e[2], SkillMenu.column(e[0]), "column of " + e[0]);
        }
        // Up the second column of a block, down its fourth.
        assertEquals(List.of(3, 3, 2, 1, 0, 0, 0, 1, 2, 3), rows(1, 10));
        assertEquals(List.of(1, 2, 2, 2, 2, 3, 4, 4, 4, 4), columns(1, 10));
    }

    @Test
    void view() {
        assertEquals(16, SkillMenu.maxOffset(60));
        assertEquals(12, SkillMenu.maxOffset(50));
        assertEquals(2, SkillMenu.maxOffset(25));
        // Recorded: Combat XXIV opens with XXV (column 10) in the fifth column, offset 6, in slot 4.
        assertEquals(6, SkillMenu.centred(25, 60));
        assertEquals(4, SkillMenu.levelSlot(25, 6));
        // XV in slot 0, XVI and XVII in 1 and 2, XIV in 9 (the recorded opening view).
        assertEquals(0, SkillMenu.levelSlot(15, 6));
        assertEquals(1, SkillMenu.levelSlot(16, 6));
        assertEquals(9, SkillMenu.levelSlot(14, 6));
        assertEquals(35, SkillMenu.levelSlot(32, 6));
        assertEquals(-1, SkillMenu.levelSlot(10, 6));
        // At the left end the skill's own item is in slot 27, and I next to it.
        assertEquals(27, SkillMenu.slot(3, 0, 0));
        assertEquals(28, SkillMenu.levelSlot(1, 0));
        // Low levels and the end are clamped.
        assertEquals(0, SkillMenu.centred(1, 60));
        assertEquals(16, SkillMenu.centred(60, 60));
        assertEquals(2, SkillMenu.centred(25, 25));
    }

    /** The recorded Combat XXIV item in Your Skills. */
    @Test
    void yourSkillsCombat() {
        assertEquals(List.of(
                "&7Fight mobs and special bosses to",
                "&7earn Combat EXP!",
                "",
                "&7Progress to Level XXV: &e38.9%",
                "&2&l&m          &f&l&m               &r &e272,514.2&6/&e700k",
                "",
                "&7Level XXV Rewards:",
                "  &eWarrior XXV",
                "    &fDeal &896➜&a100%&f more damage to mobs.",
                "  &8+&a0.5% &9☣ Crit Chance",
                "  &5Horns of Torment &7Power Stone",
                "  &6Bubba Blister &7Power Stone",
                "  &6Eccentric Painting &7Power Stone",
                "  &5Magma Urchin &7Power Stone",
                "  &5Precious Pearl &7Power Stone",
                "  &8+&6225,000 &7Coins",
                "  &8+&b10 SkyBlock XP",
                "",
                "&eClick to view!"), SkillsMenu.lore(Skill.COMBAT, COMBAT_24));
    }

    @Test
    void yourSkillsOthers() {
        assertEquals(List.of(
                "&7Harvest crops and shear sheep to",
                "&7earn Farming EXP!",
                "",
                "&7Progress to Level XXII: &e39.9%",
                "&2&l&m          &f&l&m               &r &e159,722.9&6/&e400k",
                "",
                "&7Level XXII Rewards:",
                "  &eFarmhand XXII",
                "    &fGrants &a+&884➜&a88&f &6☘ Farming Fortune&f,",
                "    &fwhich increases your chance for",
                "    &fmultiple crops.",
                "  &8+&a4 &c❤ Health",
                "  &8+&6150,000 &7Coins",
                "  &8+&b10 SkyBlock XP",
                "",
                "&eClick to view!"), SkillsMenu.lore(Skill.FARMING, Skill.FARMING.cumulative(21) + 159_722.9));
        assertEquals(List.of(
                "&7Slay bosses and runic mobs, and",
                "&7fuse runes to earn Runecrafting EXP!",
                "",
                "&7Progress to Level XI: &e85.3%",
                "&2&l&m                      &f&l&m   &r &e669.7&6/&e785",
                "",
                "&7Level XI Reward:",
                "  &7Access to Level &511 &7Runes",
                "",
                "&dLevel up Runecrafting to activate",
                "&dthe effects of rune-bearing items!",
                "",
                "&eClick to view!"), SkillsMenu.lore(Skill.RUNECRAFTING, Skill.RUNECRAFTING.cumulative(10) + 669.7));
        assertEquals(List.of(
                "&7Gain Social EXP for every new unique",
                "&7guest, hosting guests, and visiting",
                "&7islands!",
                "",
                "&7Progress to Level II: &e19.8%",
                "&2&l&m     &f&l&m                    &r &e19.8&6/&e100",
                "",
                "&7Level II Rewards:",
                "  &fAbility to purchase &dSocial Display",
                "  &ffrom Amelia",
                "  &8+&6250 &7Coins",
                "",
                "&eClick to view!"), SkillsMenu.lore(Skill.SOCIAL, 50 + 19.8));
        assertEquals(List.of(
                "&7Hunt various monsters to earn",
                "&7Hunting EXP!",
                "",
                "&7Progress to Level IX: &e4.2%",
                "&2&l&m  &f&l&m                       &r &e85&6/&e2k",
                "",
                "&7Level IX Rewards:",
                "  &eCharming IX",
                "    &fGrants &a+&80.32➜&a0.36&f &b❣ Charm Chance&f.",
                "  &8+&a1 &d☘ Hunting Fortune",
                "  &8+&65,000 &7Coins",
                "  &8+&b5 SkyBlock XP",
                "",
                "&eClick to view!"), SkillsMenu.lore(Skill.HUNTING, Skill.HUNTING.cumulative(8) + 85));
        assertEquals(List.of(
                "&7Level up pets to earn Taming EXP!",
                "",
                "&7Progress to Level XXXI: &e79.8%",
                "&2&l&m                    &f&l&m     &r &e1,037,068.8&6/&e1.3M",
                "",
                "&7Level XXXI Rewards:",
                "  &eZoologist XXXI",
                "    &fGain &830➜&a31%&f extra pet exp.",
                "  &8+&fGain &86➜&a6.2% &fExp Share rate.",
                "  &8+&a1 &d♣ Pet Luck",
                "  &8+&6375,000 &7Coins",
                "  &8+&b20 SkyBlock XP",
                "",
                "&eClick to view!"), SkillsMenu.lore(Skill.TAMING, Skill.TAMING.cumulative(30) + 1_037_068.8));
        assertEquals("&2&l&m                        &f&l&m &r &e71,605.4&6/&e75k",
                SkillsMenu.lore(Skill.CARPENTRY, Skill.CARPENTRY.cumulative(17) + 71_605.4).get(3));
        // Maxed.
        assertEquals(List.of("&7Max Skill level reached!", "&2&l&m                         &r &e111,672,425"),
                SkillsMenu.lore(Skill.COMBAT, 111_672_425).subList(3, 5));
    }

    @Test
    void yourSkillsItem() {
        assertEquals(List.of("&7View your Skill progression and", "&7rewards.", "", "&621.6 Skill Avg. &8(non-cosmetic)", "",
                "&8Also accessible via /skills."), SkillsMenu.summary(21.6, false));
        assertEquals("&eClick to view!", SkillsMenu.summary(0, true).getLast());
        assertEquals(19, SkillsMenu.slot(Skill.COMBAT));
        assertEquals(28, SkillsMenu.slot(Skill.CARPENTRY));
        assertEquals(32, SkillsMenu.slot(Skill.SOCIAL));
        assertEquals(33, SkillsMenu.slot(Skill.HUNTING));
    }

    /** The recorded Combat Skill item (REC3 00:34.4). */
    @Test
    void header() {
        assertEquals(List.of(
                "&7Fight mobs and special bosses to",
                "&7earn Combat EXP!",
                "",
                "&7Progress to Level XXV: &e38.9%",
                "&2&l&m          &f&l&m               &r &e272,514.2&6/&e700k",
                "",
                "&eWarrior XXIV",
                "  &fDeal &a96%&f more damage to mobs.",
                "",
                "&8Increase your Combat Level to",
                "&8unlock Perks, statistic bonuses, and",
                "&8more!"), SkillMenu.header(Skill.COMBAT, COMBAT_24));
    }

    /** The recorded XV (reached), XXV (in progress) and XXVI (locked) items. */
    @Test
    void levels() {
        assertEquals(List.of(
                "&7Rewards:",
                "  &eWarrior XV",
                "    &fDeal &856➜&a60%&f more damage to mobs.",
                "  &8+&a0.5% &9☣ Crit Chance",
                "  &7Intermediate Accessory Bag Powers",
                "  &aLuxurious Spool &7Power Stone",
                "  &aRock Candy &7Power Stone",
                "  &9Ender Monocle &7Power Stone",
                "  &8+&630,000 &7Coins",
                "  &8+&b10 SkyBlock XP",
                "",
                "&a&lUNLOCKED"), SkillMenu.levelLore(Skill.COMBAT, 15, COMBAT_24));
        List<String> inProgress = SkillMenu.levelLore(Skill.COMBAT, 25, COMBAT_24);
        assertEquals(List.of("", "&7Progress: &e38.9%", "&2&l&m          &f&l&m               &r &e272,514.2&6/&e700k"),
                inProgress.subList(inProgress.size() - 3, inProgress.size()));
        assertEquals(List.of(
                "&7Rewards:",
                "  &eWarrior XXVI",
                "    &fDeal &8100➜&a104%&f more damage to mobs.",
                "  &8+&a0.5% &9☣ Crit Chance",
                "  &8+&6250,000 &7Coins",
                "  &8+&b20 SkyBlock XP"), SkillMenu.levelLore(Skill.COMBAT, 26, COMBAT_24));
    }

    private static List<Integer> rows(int from, int to) {
        return java.util.stream.IntStream.rangeClosed(from, to).map(SkillMenu::row).boxed().toList();
    }

    private static List<Integer> columns(int from, int to) {
        return java.util.stream.IntStream.rangeClosed(from, to).map(SkillMenu::column).boxed().toList();
    }
}
