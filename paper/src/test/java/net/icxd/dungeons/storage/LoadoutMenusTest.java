package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.stats.Stat;

/**
 * Loadouts, a loadout's page, the Armor and Equipment Sets and Select Power Stone, against the recorded ones
 * (the Loadouts and Storage tour, 00:40.5 to 01:36.5).
 */
class LoadoutMenusTest {
    private static final List<String> NONE = Arrays.asList(null, null, null, null);

    // Loadouts

    @Test
    void loadoutLore() {
        // Loadout 3, never set up (01:18.7).
        assertEquals(List.of("&7Helmet: &8None", "&7Chestplate: &8None", "&7Leggings: &8None", "&7Boots: &8None", "", "&7Necklace: &8None",
                "&7Cloak: &8None", "&7Belt: &8None", "&7Gloves/Bracelet: &8None", "", "&7Pet: &8None", "&7HOTM: &8None", "&7HOTF: &8None",
                "&7Power Stone: &8None", "&7Tuning Template Slot: &8None", "", "&eRight-click to edit", "", "&cYou must customize this loadout",
                "&cbefore you can equip it!"), LoadoutsMenu.lore(Loadouts.fresh(2), null, null, false));
        // Loadout 1: its armor set, nothing else.
        List<String> armor = List.of("&dAncient Necron's Helmet &6✪✪✪✪✪", "&dAncient Necron's Chestplate &6✪✪✪✪✪&c➊",
                "&dAncient Necron's Leggings &6✪✪✪✪✪&c➊", "&dAncient Necron's Boots &6✪✪✪✪✪&c➊");
        assertEquals(List.of("&7Helmet: &dAncient Necron's Helmet &6✪✪✪✪✪", "&7Chestplate: &dAncient Necron's Chestplate &6✪✪✪✪✪&c➊",
                "&7Leggings: &dAncient Necron's Leggings &6✪✪✪✪✪&c➊", "&7Boots: &dAncient Necron's Boots &6✪✪✪✪✪&c➊", "", "&7Necklace: &8None",
                "&7Cloak: &8None", "&7Belt: &8None", "&7Gloves/Bracelet: &8None", "", "&7Pet: &8None", "&7HOTM: &8None", "&7HOTF: &8None",
                "&7Power Stone: &8None", "&7Tuning Template Slot: &8None", "", "&eLeft-click to equip!", "&eRight-click to edit"),
                LoadoutsMenu.lore(new Loadouts.Loadout("Loadout 1", 0, null, null), armor, null, false));
        // Loadout 2, what they have on: no "Left-click to equip!"; a set's empty piece is None.
        List<String> two = LoadoutsMenu.lore(new Loadouts.Loadout("Loadout 2", 1, 0, "Silky"), Arrays.asList("&dHelmet", null, null, null), NONE, true);
        assertEquals("&7Helmet: &dHelmet", two.get(0));
        assertEquals("&7Chestplate: &8None", two.get(1));
        assertEquals("&7Power Stone: &aSilky", two.get(13));
        assertEquals(List.of("", "&eRight-click to edit"), two.subList(two.size() - 2, two.size()));
    }

    @Test
    void loadoutsSlots() {
        Map<Integer, Icon> icons = LoadoutsMenu.icons(0, null, 571, PrivateTables.madeUp(Map.of()));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aHeart of the Forest Slot", List.of("&7Quickly swap between saved trees.", "",
                "&7Current: &aHeart of the Forest 1", "", "&cSwapping trees has a 10m cooldown!", "", "&eClick to view!"), LoadoutsMenu.HOTF_HEAD),
                icons.get(LoadoutsMenu.HOTF));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aHeart of the Mountain Slot", List.of("&7Quickly swap between saved trees.", "",
                "&7Current: &aHeart of the Mountain 1", "", "&cSwapping trees has a 10m cooldown!", "", "&eClick to view!"), LoadoutsMenu.HOTM_HEAD),
                icons.get(LoadoutsMenu.HOTM));
        assertEquals(new Icon(Material.COMPARATOR, "&aStats Tuning", "&7Optimize your build to your liking by using", "&eTuning Points&7.", "",
                "&7Every &610 MP &7grants &e1 Tuning Point&7.", "", "&7Magical Power: &6571", "&7Tuning Points: &e57", "&7Unassigned Points: &c57!!!", "",
                "&eClick to view!"), icons.get(LoadoutsMenu.STATS_TUNING));
        assertEquals(new Icon(Material.ARROW, "&aNext Page", "&ePage 2"), icons.get(LoadoutsMenu.NEXT));
        assertFalse(icons.containsKey(LoadoutsMenu.PREVIOUS));
        assertEquals(new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Empty Equipment Slot", "&8> Gloves", "&8> Bracelet", "", "&eClick to select!"),
                icons.get(LoadoutsMenu.GLOVES));
        assertEquals(new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Empty Helmet Slot", "", "&eClick to select!"), icons.get(LoadoutsMenu.HELMET));
        assertEquals(List.of("&7Choose your selected Power Stone.", "", "&7Current: &8None", "", "&eClick to view!"),
                icons.get(LoadoutsMenu.POWER_STONE).lore());
        Map<Integer, Icon> last = LoadoutsMenu.icons(2, null, 0, PrivateTables.madeUp(Map.of()));
        assertEquals(new Icon(Material.ARROW, "&aPrevious Page", "&ePage 2"), last.get(LoadoutsMenu.PREVIOUS));
        assertFalse(last.containsKey(LoadoutsMenu.NEXT));
        assertEquals("&7Unassigned Points: &c0", last.get(LoadoutsMenu.STATS_TUNING).lore().get(7));
    }

    @Test
    void recordedPowerStone() {
        StorageTables tables = PrivateTables.load();
        // Loadouts' own (01:18.7) and a loadout's (01:02.8), with Silky, at 500 as those showed it.
        assertEquals(List.of("&7Choose your selected Power Stone.", "", "&7Current: &aSilky", "", "&7Stats:", "&9+420.94 Crit Damage",
                "&f+11.08 Speed", "", "&7Unique Power Bonus:", "&e+5 Attack Speed", "", "&eClick to view!"),
                LoadoutsMenu.powerStone(tables.power("Silky"), 500, tables).lore());
        assertEquals(List.of("&7Select a Power Stone to use in this", "&7loadout!", "", "&7Current: &aSilky", "", "&7Stats:", "&9+420.94 Crit Damage",
                "&f+11.08 Speed", "", "&7Unique Power Bonus:", "&e+5 Attack Speed", "", "&eLeft-click to change!", "&eRight-click to clear!"),
                LoadoutMenu.powerStone(tables.power("Silky"), 500, tables).lore());
    }

    @Test
    void loadoutsPerPage() {
        // Twelve a page, three by four; the third has only Loadouts 25 to 27, the rest glass (02:04.8).
        assertEquals(List.of(14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43), List.copyOf(LoadoutsMenu.loadoutSlots(0).keySet()));
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), List.copyOf(LoadoutsMenu.loadoutSlots(0).values()));
        assertEquals(12, LoadoutsMenu.loadoutSlots(1).get(14));
        assertEquals(Map.of(14, 24, 15, 25, 16, 26), LoadoutsMenu.loadoutSlots(2));
    }

    @Test
    void lockedLoadout() {
        assertEquals(new Icon(Material.RED_DYE, "&cLoadout 19 Locked", "&7Unlock more slots from:", "&8▶ &aAccount Upgrades &8- &69 Slots", "",
                "&cUnlock more slots from &dElizabeth &cat", "&cthe &bCommunity Center"), LoadoutsMenu.locked(18, Rank.MVP_PLUS));
    }

    // A loadout's page

    @Test
    void freshLoadout() {
        // Loadout 3's page (01:22.3).
        Map<Integer, Icon> icons = LoadoutMenu.icons(Loadouts.fresh(2), null, 571, PrivateTables.madeUp(Map.of()));
        String[] pieces = {"Helmet", "Chestplate", "Leggings", "Boots"};
        for (int i = 0; i < 4; i++) {
            assertEquals(new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Empty " + pieces[i] + " Slot", "", "&7No armor selected for this loadout.", "",
                    "&eLeft-click to change!"), icons.get(LoadoutMenu.ARMOR_SLOTS[i]));
        }
        assertEquals(new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Empty Equipment Slot", "&8> Necklace", "", "&7No equipment selected for this",
                "&7loadout.", "", "&eLeft-click to change!"), icons.get(LoadoutMenu.NECKLACE));
        assertEquals(new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Empty Equipment Slot", "&8> Gloves", "&8> Bracelet", "",
                "&7No equipment selected for this", "&7loadout.", "", "&eLeft-click to change!"), icons.get(LoadoutMenu.GLOVES));
        assertEquals(new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&aPet", "&7Select a pet to use in this loadout!", "", "&7Current: &8None", "",
                "&eLeft-click to change!"), icons.get(LoadoutMenu.PET));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aHeart of the Mountain Slot", List.of("&7Select a Heart of the Mountain to", "&7use in this loadout!",
                "", "&7Selected: &8None", "", "&cSwapping trees has a 10m cooldown!", "", "&eLeft-click to change!"), LoadoutsMenu.HOTM_HEAD),
                icons.get(LoadoutMenu.HOTM));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aHeart of the Forest Slot", List.of("&7Select a Heart of the Forest to use", "&7in this loadout!",
                "", "&7Selected: &8None", "", "&cSwapping trees has a 10m cooldown!", "", "&eLeft-click to change!"), LoadoutsMenu.HOTF_HEAD),
                icons.get(LoadoutMenu.HOTF));
        assertEquals(List.of("&7Select a Power Stone to use in this", "&7loadout!", "", "&7Current: &8None", "", "&eLeft-click to change!"),
                icons.get(LoadoutMenu.POWER_STONE).lore());
        assertEquals(new Icon(Material.COMPARATOR, "&aStats Tuning Slot", "&7Select a Stats Tuning template slot", "&7to use in this loadout!", "",
                "&7Current: &8None", "", "&eLeft-click to change!"), icons.get(LoadoutMenu.STATS_TUNING));
        assertEquals(new Icon(Material.NAME_TAG, "&aRename Loadout", "&7Want to feel a more personal", "&7connection with your loadout slot?",
                "&7Give it a name!", "", "&7Current Name: &aLoadout 3", "", "&eClick to rename!"), LoadoutMenu.rename("Loadout 3"));
        // With a set that lacks the piece, it can be cleared too.
        assertEquals("&eRight-click to clear!", LoadoutMenu.emptyArmor("Boots", true).lore().getLast());
        assertEquals("&eRight-click to clear!", LoadoutMenu.emptyEquipment(1, true).lore().getLast());
    }

    // Armor and Equipment Sets

    @Test
    void sets() {
        assertEquals("(1/3) Armor Sets", SetsMenu.title(SetsMenu.Kind.ARMOR, 0));
        assertEquals("(2/2) Equipment Sets", SetsMenu.title(SetsMenu.Kind.EQUIPMENT, 1));
        // Recorded at 00:56.1 and 01:14.7.
        assertEquals(new Icon(Material.LIME_STAINED_GLASS_PANE, "&aSlot 4 Helmet", "&7Place a helmet here to add it to this", "&7set."),
                SetsMenu.placeholder(SetsMenu.Kind.ARMOR, 3, 3, 0));
        assertEquals(new Icon(Material.PURPLE_STAINED_GLASS_PANE, "&aSlot 9 Boots", "&7Place a pair of boots here to add it", "&7to this set."),
                SetsMenu.placeholder(SetsMenu.Kind.ARMOR, 8, 8, 3));
        assertEquals(new Icon(Material.GREEN_STAINED_GLASS_PANE, "&aSlot 14 Leggings", "&7Place a pair of leggings here to add", "&7it to this set."),
                SetsMenu.placeholder(SetsMenu.Kind.ARMOR, 4, 13, 2));
        assertEquals(new Icon(Material.MAGENTA_STAINED_GLASS_PANE, "&aSlot 8 Gloves/Bracelet", "&7Place a pair of gloves or a bracelet",
                "&7here to add it to this set."), SetsMenu.placeholder(SetsMenu.Kind.EQUIPMENT, 7, 7, 3));
        assertEquals(List.of("&7Place a belt here to add it to this set."), SetsMenu.placeLore(SetsMenu.Kind.EQUIPMENT, 2));
        assertEquals(List.of("&7Place a chestplate here to add it to", "&7this set."), SetsMenu.placeLore(SetsMenu.Kind.ARMOR, 1));
        assertEquals(new Icon(Material.GRAY_DYE, "Slot 2:&a Ready", "&7This slot is ready to be selected.", "", "&eClick to equip to loadout!"),
                SetsMenu.button(1, false));
        assertEquals(new Icon(Material.LIME_DYE, "Slot 1:&a Selected", "&7This slot contains this loadout's", "&7currently selected set.", "",
                "&eClick to unequip from loadout!"), SetsMenu.button(0, true));
        assertEquals(new Icon(Material.LAVA_BUCKET, "&cClear Selection", "&7Clears your current selection for", "&7this component of your loadout.", "",
                "&eClick to clear!"), SetsMenu.clearSelection());
    }

    @Test
    void lockedSets() {
        // The wiki's Wardrobe: the rank that has it.
        assertEquals(new Icon(Material.RED_DYE, "&7Slot 5: &cLocked", "&7This wardrobe slot is locked and", "&7cannot be used", "",
                "&cRequires &aVIP&6+&a"), SetsMenu.locked(SetsMenu.Kind.ARMOR, 4, Rank.DEFAULT));
        assertEquals("&cRequires &bMVP&6+&b", SetsMenu.locked(SetsMenu.Kind.ARMOR, 10, Rank.VIP_PLUS).lore().getLast());
        assertEquals("&cRequires &aVIP&6+&a", SetsMenu.locked(SetsMenu.Kind.EQUIPMENT, 2, Rank.DEFAULT).lore().getLast());
        // Past every rank's: the Account Upgrades'.
        assertEquals(Loadouts.unlockLines(Rank.MVP_PLUS, SetsMenu.Kind.ARMOR::unlocked), SetsMenu.locked(SetsMenu.Kind.ARMOR, 18, Rank.MVP_PLUS).lore());
    }

    // Select Power Stone

    @Test
    void selectPowerStone() {
        StorageTables.Power stone = new StorageTables.Power("Shiny", "Advanced Stone Power", "SHINY_STONE", null, 20, Map.of(Stat.HEALTH, 100.0),
                Map.of(Stat.FEROCITY, 1.0));
        StorageTables.Power plain = new StorageTables.Power("Plain", "Starter Power", null, "OXEYE_DAISY", 0, Map.of(Stat.STRENGTH, 100.0), Map.of());
        StorageTables tables = PrivateTables.withPowers(stone, plain);
        Map<Integer, Icon> icons = PowerMenu.icons(List.of(stone, plain), "Plain", 250, tables, "Loadout 2", 0, 1);
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aShiny", "&8Advanced Stone Power", "", "&7Stats:", "&c+462.82 Health", "", "&7Unique Power Bonus:",
                "&c+1 Ferocity", "", "&7You have: &6250 Magical Power", "", "&eClick to select reforge!"), icons.get(10));
        // The one selected is lime glass (recorded: Silky, 01:07.4); one without a bonus has no bonus lines (Commando).
        assertEquals(new Icon(Material.LIME_STAINED_GLASS_PANE, "&aPlain", "&8Starter Power", "", "&7Stats:", "&c+231.41 Strength", "",
                "&7You have: &6250 Magical Power", "", "&aPower is selected!"), icons.get(11));
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To Loadout 2"), icons.get(PowerMenu.GO_BACK));
        assertEquals(SetsMenu.clearSelection(), icons.get(PowerMenu.CLEAR));
        assertEquals(4, icons.size());
        assertEquals(Material.OXEYE_DAISY, PowerMenu.material(plain));
        // Where they go: seven a row, as recorded (15 powers: 10 to 16, 19 to 25, 28).
        List<Integer> slots = new ArrayList<>();
        for (int slot : PowerMenu.SLOTS) slots.add(slot);
        assertEquals(List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28), slots.subList(0, 15));
        // More than 28 (every power, Stone Powers included, is 32) go on a second page.
        assertEquals(1, PowerMenu.pages(0));
        assertEquals(1, PowerMenu.pages(28));
        assertEquals(2, PowerMenu.pages(32));
        Map<Integer, Icon> first = PowerMenu.icons(List.of(plain), null, 0, tables, "Loadouts", 0, 2);
        assertEquals(new Icon(Material.ARROW, "&aNext Page", "&ePage 2"), first.get(PowerMenu.NEXT));
        assertFalse(first.containsKey(PowerMenu.PREVIOUS));
        Map<Integer, Icon> second = PowerMenu.icons(List.of(plain), null, 0, tables, "Loadouts", 1, 2);
        assertEquals(new Icon(Material.ARROW, "&aPrevious Page", "&ePage 1"), second.get(PowerMenu.PREVIOUS));
        assertFalse(second.containsKey(PowerMenu.NEXT));
    }

    @Test
    void recordedSelectPowerStone() {
        StorageTables tables = PrivateTables.load();
        // 01:07.4: Sighted, and Silky selected.
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aSighted", "&8Intermediate Stone Power", "", "&7Stats:", "&b+745.31 Intelligence", "",
                "&7Unique Power Bonus:", "&c+3 Ability Damage", "", "&7You have: &6571 Magical Power", "", "&eClick to select reforge!"),
                PowerMenu.power(tables.power("Sighted"), false, 571, tables));
        assertEquals(new Icon(Material.LIME_STAINED_GLASS_PANE, "&aSilky", "&8Intermediate Stone Power", "", "&7Stats:", "&9+472.03 Crit Damage",
                "&f+12.42 Speed", "", "&7Unique Power Bonus:", "&e+5 Attack Speed", "", "&7You have: &6571 Magical Power", "", "&aPower is selected!"),
                PowerMenu.power(tables.power("Silky"), true, 571, tables));
        assertEquals(new Icon(Material.STICK, "&aCommando", "&8Intermediate Power", "", "&7Stats:", "&c+104.34 Health", "&a+49.69 Defense",
                "&c+173.91 Strength", "&9+9.94 Crit Chance", "&9+173.91 Crit Damage", "", "&7You have: &6571 Magical Power", "",
                "&eClick to select reforge!"), PowerMenu.power(tables.power("Commando"), false, 571, tables));
        assertTrue(PowerMenu.power(tables.power("Simple"), false, 571, tables).lore().contains("&b+111.8 Intelligence"));
    }
}
