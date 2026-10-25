package net.icxd.dungeons.museum;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.museum.Museum.Sort;
import net.icxd.dungeons.museum.MuseumData.Entry;

/** The Museum menus' items and lines against the recording (2026-10-25, 00:58.4 to 02:54.3). */
class MuseumMenusTest {
    private static final MuseumData DATA = MuseumFixtures.fixture();
    /** The recorded donation: 2026-10-25 00:31:36 UTC, which said "Oct 24, 2026". */
    private static final long DONATED = Instant.parse("2026-10-25T00:31:36Z").toEpochMilli();
    private static final String EMPTY_BAR = "&f&l&m                         &r";

    @BeforeEach
    void data() {
        Museum.data(DATA);
    }

    @AfterEach
    void noData() {
        Museum.data(MuseumData.empty());
    }

    @Test
    void yourMuseumsHead() {
        Icon head = MuseumMenu.head(0, 636);
        assertEquals("&9Museum", head.name());
        assertEquals(List.of("&7The &9Museum &7is a compendium of all of", "&7your items in SkyBlock. Donate items", "&7to your Museum to unlock rewards.",
                "", "&7Other players can visit your Museum", "&7at any time! Display your best items", "&7proudly for all to see.", "",
                "&7Items Donated: &e0&6%", EMPTY_BAR + " &b0&9/&b636"), head.lore());
        assertEquals(MuseumMenu.HEAD_TEXTURE, head.texture());
        // After the Undead Sword (its Top Items aren't here).
        assertEquals(List.of("&7Items Donated: &e0.2&6%", "&9&l&m &f&l&m                        &r &b1&9/&b636"), MuseumMenu.head(1, 636).lore().subList(8, 10));
    }

    @Test
    void categories() {
        MuseumData.Category combat = new MuseumData.Category("COMBAT", "Combat", java.util.Collections.nCopies(245, DATA.entry("UNDEAD_SWORD")));
        Icon icon = MuseumMenu.category(combat, MuseumMenu.LOOKS.get("COMBAT"), 1, true);
        assertEquals(new Icon(Material.STONE_SWORD, "&cCombat", List.of("&7View all of the items related to the", "&cCombat Skill &7that you have donated",
                "&7to the &9Museum&7!", "", "&7Items Donated: &e0.4&6%", "&9&l&m &f&l&m                        &r &b1&9/&b245", "",
                "&eClick to view!")), icon);
        // Atop its pages, without the click.
        assertEquals(icon.lore().subList(0, 6), MuseumMenu.category(combat, MuseumMenu.LOOKS.get("COMBAT"), 1, false).lore());
        assertEquals(List.of(20, 21, 22, 23, 24, 30, 31), List.of("COMBAT", "FARMING", "MINING", "FISHING", "FORAGING", "DUNGEONEERING", "HUNTING")
                .stream().map(id -> MuseumMenu.LOOKS.get(id).slot()).toList());
        assertEquals(List.of("&7View all of the items related to the", "&4Dungeoneering Skill &7that you have", "&7donated to the &9Museum&7!"),
                MuseumMenu.LOOKS.get("DUNGEONEERING").description());
        assertEquals("&cCombat", MuseumMenu.colored("COMBAT"));
    }

    @Test
    void theOtherButtons() {
        assertEquals(List.of("&7Every &b100 SkyBlock XP &7obtained from", "&7your Museum, &6Eleanor &7will reward you.", "",
                "&dSpecial Items &7do not reward SkyBlock", "&7XP.", "", "&7Total XP: &b0&3/&b3,646", "&7Milestone: &e0&6/&e40", "",
                "&7Progress to Milestone 1: &30%", "&f&m                         &r &b0&3/&b100", "", "&eClick to view!"),
                MuseumMenu.milestones(0, 3646).lore());
        // One donated: 1%, and still no part of the bar.
        assertEquals(List.of("&7Total XP: &b1&3/&b3,646", "&7Milestone: &e0&6/&e40", "", "&7Progress to Milestone 1: &31%",
                "&f&m                         &r &b1&3/&b100"), MuseumMenu.milestones(1, 3646).lore().subList(6, 11));
        assertEquals(List.of("&7Eleanor will evaluate the total value", "&7of your Museum.", "", "&cUnlocked at Museum Milestone 3"),
                MuseumMenu.appraisal(0).lore());
        assertEquals(List.of("&7View all of the &dSpecial Items &7that you", "&7have donated to the &9Museum&7!", "", "&7These items don't count towards",
                "&7Museum progress and rewards, but", "&7are cool nonetheless. Items that are", "&9rare &7and &6prestigious &7fit into this",
                "&7category, and can be displayed in", "&7the Museum.", "", "&7Items Donated: &b0", "", "&eClick to view!"), MuseumMenu.special(true).lore());
        assertEquals(List.of("&7Search your entire Museum for a", "&7specific item or armor set!", "", "&eClick to search for an item!"),
                MuseumMenu.search().lore());
    }

    @Test
    void pagesAndSorts() {
        assertEquals("(1/9) Museum ➜ Combat", MuseumCategoryMenu.title("Combat", 1, 9));
        assertEquals("Museum ➜ Foraging", MuseumCategoryMenu.title("Foraging", 1, 1));
        assertEquals("Museum ➜ Special Items", MuseumCategoryMenu.title(MuseumCategoryMenu.SPECIAL_ITEMS, 1, 1));
        assertEquals(List.of(1, 1, 2, 9), List.of(MuseumCategoryMenu.pages(0), MuseumCategoryMenu.pages(28), MuseumCategoryMenu.pages(29),
                MuseumCategoryMenu.pages(245)));
        assertEquals(List.of("", "&b▶ Ascending XP", "&7  Descending XP", "&7  Missing First", "&7  A to Z", "&7  Z to A", "", "&eClick to switch sort!",
                "&bRight-Click to go backwards!"), MuseumText.sortLore(Sort.ASCENDING_XP));
        assertEquals(List.of("&7Show info on your donated items:", "&8- &7Date of the donation", "&8- &7Item created edition", "&8- &7Item Value",
                "&8- &7Clean Item Value", "&8- &7Museum Display Slot", "", "&7Toggled: &a&lON", "", "&eClick to toggle!"), MuseumText.extraDetailsLore(true));
        assertEquals("&7Toggled: &c&lOFF", MuseumText.extraDetailsLore(false).get(7));

        List<Entry> combat = DATA.category("COMBAT").entries();
        assertEquals(combat, MuseumCategoryMenu.sorted(combat, Sort.ASCENDING_XP, e -> false));
        assertEquals(List.of("MONSTER_HUNTER", "REVENANT_SWORD", "MONSTER_RAIDER", "DOJO_GREEN_BELT", "DOJO_YELLOW_BELT", "DOJO_WHITE_BELT", "UNDEAD_SWORD",
                "CELESTE"), MuseumCategoryMenu.sorted(combat, Sort.DESCENDING_XP, e -> false).stream().map(Entry::id).toList());
        assertEquals("UNDEAD_SWORD", MuseumCategoryMenu.sorted(combat, Sort.MISSING_FIRST, e -> e.id().equals("UNDEAD_SWORD")).getLast().id());
        assertEquals(List.of("Celeste Armor", "Green Belt", "Monster Hunter Armor", "Monster Raider Armor", "Revenant Falchion", "Undead Sword",
                "White Belt", "Yellow Belt"), MuseumCategoryMenu.sorted(combat, Sort.A_TO_Z, e -> false).stream().map(Entry::name).toList());
        assertEquals("Yellow Belt", MuseumCategoryMenu.sorted(combat, Sort.Z_TO_A, e -> false).getFirst().name());
    }

    @Test
    void entries() {
        assertEquals(List.of("&7Click on this item in your inventory to", "&7add it to your &9Museum&7!", "", "&7Donating a &eYellow Belt &7will count as",
                "&7donating this item.", "", "&7Reward: &b+1 SkyBlock XP"), MuseumText.missing(DATA.entry("DOJO_WHITE_BELT")));
        assertEquals(List.of("&7Click on an armor piece in your", "&7inventory that belongs to this armor", "&7set to donate the full set to your",
                "&9Museum&7.", "", "&7Reward: &b+1 SkyBlock XP"), MuseumText.missing(DATA.entry("CELESTE")));
        Entry sword = DATA.entry("UNDEAD_SWORD");
        // As recorded, less its value and its Atrium slot (neither is here).
        assertEquals(List.of(MuseumText.RULE, "&7Donated: &bOct 24, 2026", "&7Created: &aOct 24, 2026", "&7Reward: &b+1 SkyBlock XP &a✔", "",
                "&eClick to retrieve item!"), MuseumText.donatedLines(sword, DONATED, DONATED - 60_000, true, false));
        assertEquals(List.of(MuseumText.RULE, "&7Donated: &bOct 24, 2026", "&7Created: &aOct 24, 2026", "&7Reward: &b+1 SkyBlock XP &a✔",
                MuseumText.RULE, "&7You have retrieved this from your", "&7Museum but can add it back at any", "&7time.", "",
                "&7Click on this item in your inventory to", "&7add it to your &9Museum&7!"), MuseumText.donatedLines(sword, DONATED, DONATED, true, true));
        assertEquals(List.of(MuseumText.RULE, "&7Reward: &b+1 SkyBlock XP &a✔", "", "&eClick to retrieve item!"),
                MuseumText.donatedLines(sword, DONATED, 0, false, false));
        assertEquals("&8&m                         ", MuseumText.RULE);
    }

    @Test
    void milestonesOnThePath() {
        MuseumData.Milestone first = DATA.milestones().getFirst();
        assertEquals(List.of("&7Required XP: &e100", "", "&7Rewards:", "&8+&b1% Bits Multiplier", "&8+&62% Bank Interest", "", "&7Progress: &30%",
                "&f&m                         &r &b0&3/&b100", "", "&cYou can't claim this reward!"), MuseumText.milestoneLore(first, 0));
        assertEquals(List.of("&7Required XP: &e300", "", "&7Rewards:", "&8+&dDeluxe Bank Upgrade", "&8+&fMuseum Appraisal Service",
                "&8+&b1% Bits Multiplier", "&8+&62% Bank Interest", "", "&cYou can't claim this reward!"), MuseumText.milestoneLore(DATA.milestones().get(2), 0));
        assertEquals("&a&lUNLOCKED", MuseumText.milestoneLore(first, 100).getLast());
        assertEquals(List.of(Material.YELLOW_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE),
                List.of(MilestonesMenu.pane(first, 0), MilestonesMenu.pane(DATA.milestones().get(1), 0), MilestonesMenu.pane(first, 150)));
        assertEquals(1, MilestonesMenu.current(0, 40));
        assertEquals(40, MilestonesMenu.current(4000, 40));
        assertEquals("&3&m      &f&m                   &r &b25&3/&b100", MuseumText.milestoneBar(25, 100));
    }

    @Test
    void whatTheySayInChat() {
        Entry sword = DATA.entry("UNDEAD_SWORD");
        assertEquals("&aYou donated your &fUndead Sword &ato the &cCombat &asection of the Museum! &b+1 SkyBlock XP",
                MuseumDonations.donatedLine("&fUndead Sword", sword, 1));
        assertEquals("&aYou donated your &fUndead Sword &ato the &cCombat &asection of the Museum!", MuseumDonations.donatedLine("&fUndead Sword", sword, 0));
        assertEquals("&aYou retrieved your &fUndead Sword &afrom the &3Museum&a!", MuseumDonations.retrievedLine("&fUndead Sword"));
        assertEquals("&7It still counts towards your &3Museum&7 progress, but not towards your &6total item value&7.", MuseumDonations.RETRIEVED);
    }

    @Test
    void numbers() {
        assertEquals(List.of("0", "0.2", "0.4", "100"), List.of(MuseumText.percent(0, 636), MuseumText.percent(1, 636), MuseumText.percent(1, 245),
                MuseumText.percent(636, 636)));
        assertEquals("&9&l&m                         &r &b636&9/&b636", MuseumText.donatedBar(636, 636));
        assertEquals("Oct 24, 2026", MuseumText.date(DONATED));
        assertEquals(List.of(), MuseumText.missing(DATA.entry("CELESTE")).stream().filter(l -> l.contains("Donating")).toList());
    }
}
