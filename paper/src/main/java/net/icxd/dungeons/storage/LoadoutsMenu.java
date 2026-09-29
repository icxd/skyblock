package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.menu.SkyBlockMenu;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Loadouts ({@code /loadouts}, or the barrel in the SkyBlock Menu), as recorded (the Loadouts and Storage
 * tour, 00:40.5 to 02:06.4): three pages, each with what they wear on the left (the Heart of the Forest and
 * Mountain slots, the four equipment pieces, the armor, the pet, the Power Stone and Stats Tuning) and twelve
 * loadouts on the right. A loadout shows its armor set's helmet (gray dye until it's set up), and what it
 * has; a left click equips it ("You equipped Loadout 1!", and the menu again), a right click edits it (see
 * {@link LoadoutMenu}). Slots past their rank's are locked in red dye (see {@link Loadouts#loadouts}).
 * The Power Stone opens Select Power Stone for their selected power, and Stats Tuning its menu ({@link
 * StatsTuningMenu}); the tree slots and the empty pet slot do nothing yet (no saved trees or pets). Main thread.
 */
public final class LoadoutsMenu extends GUI {
    static final int HOTF = 9;
    static final int HOTM = 18;
    static final int NECKLACE = 10;
    static final int HELMET = 11;
    static final int CLOAK = 19;
    static final int CHESTPLATE = 20;
    static final int PET = 21;
    static final int POWER_STONE = 27;
    static final int BELT = 28;
    static final int LEGGINGS = 29;
    static final int STATS_TUNING = 36;
    static final int GLOVES = 37;
    static final int BOOTS = 38;
    static final int PREVIOUS = 17;
    static final int NEXT = 44;
    static final int GO_BACK = 48;
    static final int CLOSE = 49;
    static final int PAGES = 3;
    /** Where the page's twelve loadouts go, three by four. */
    static final int[] LOADOUT_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};
    static final int[] ARMOR_SLOTS = {HELMET, CHESTPLATE, LEGGINGS, BOOTS};
    static final int[] EQUIPMENT_SLOTS = {NECKLACE, CLOAK, BELT, GLOVES};
    static final String HOTF_HEAD = "5ef539b165125cfa46b06ffb9659e7cf89084bbd3ede1b314edc8f443343d61c";
    static final String HOTM_HEAD = "86f06eaa3004aeed09b3d5b45d976de584e691c0e9cade133635de93d23b9edb";

    private final Player viewer;
    private final int page;

    public LoadoutsMenu(Player viewer, int page) {
        super("(" + (page + 1) + "/" + PAGES + ") Loadouts", Size.SIX);
        this.viewer = viewer;
        this.page = Math.clamp(page, 0, PAGES - 1);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Document profile = user.profile();
        Document storage = StoredInventory.storage(profile);
        int ap = AccessoryBag.accessoryPower(viewer);

        for (Map.Entry<Integer, Icon> e : icons(page, AccessoryBag.selectedPower(profile), ap, StatsTuning.assigned(profile), StorageTables.get()).entrySet()) {
            int slot = e.getKey();
            ItemStack stack = e.getValue().stack();
            switch (slot) {
                case POWER_STONE -> set(GUIClickableItem.button(slot, powerLook(stack, AccessoryBag.selectedPower(profile)), viewer,
                        () -> new PowerMenu(viewer, -1).open(viewer)));
                case STATS_TUNING -> set(GUIClickableItem.button(slot, stack, viewer, () -> new StatsTuningMenu(viewer).open(viewer)));
                case PREVIOUS -> set(GUIClickableItem.button(slot, stack, viewer, () -> new LoadoutsMenu(viewer, page - 1).open(viewer)));
                case NEXT -> set(GUIClickableItem.button(slot, stack, viewer, () -> new LoadoutsMenu(viewer, page + 1).open(viewer)));
                default -> set(slot, stack);
            }
        }
        // What they wear, as it is.
        List<ItemStack> armor = LoadoutActions.armor(viewer.getInventory());
        ItemStack[] equipment = Equipment.worn(viewer);
        for (int i = 0; i < 4; i++) {
            if (armor.get(i) != null && !armor.get(i).isEmpty()) set(ARMOR_SLOTS[i], armor.get(i).clone());
            if (equipment[i] != null) set(EQUIPMENT_SLOTS[i], equipment[i].clone());
        }

        int unlocked = Loadouts.loadouts(user.getRank());
        int wornArmor = StorageDocument.worn(storage, StorageDocument.WORN_ARMOR_SET);
        int wornEquipment = StorageDocument.worn(storage, StorageDocument.WORN_EQUIPMENT_SET);
        StorageTables.Power selected = AccessoryBag.selectedPower(profile);
        for (Map.Entry<Integer, Integer> e : loadoutSlots(page).entrySet()) {
            int slot = e.getKey();
            int index = e.getValue();
            if (index >= unlocked) {
                set(slot, locked(index, user.getRank()).stack());
                continue;
            }
            Loadouts.Loadout loadout = Loadouts.get(profile, index);
            boolean equipped = Loadouts.equipped(loadout, wornArmor, wornEquipment, selected == null ? null : selected.name());
            Parts parts = parts(viewer, storage, loadout, wornArmor, wornEquipment);
            ItemStack icon = look(loadout, parts, lore(loadout, parts.armorNames(), parts.equipmentNames(), equipped));
            set(GUIClickableItem.button(slot, icon, viewer, click -> {
                if (click.isRightClick()) {
                    new LoadoutMenu(viewer, index).open(viewer);
                    return;
                }
                String said = LoadoutActions.equip(viewer, index);
                if (said != null) viewer.sendMessage(Text.line(said));
                if (said != null && said.startsWith("&a")) new LoadoutsMenu(viewer, page).open(viewer);
            }));
        }
        set(GUIClickableItem.button(GO_BACK, new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Menu").stack(), viewer,
                () -> new SkyBlockMenu(viewer).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
    }

    /**
     * The page's loadouts (from 0) by slot: twelve, but past the last there's none, only glass (recorded: the
     * third page's three, 02:04.8).
     */
    static Map<Integer, Integer> loadoutSlots(int page) {
        Map<Integer, Integer> slots = new LinkedHashMap<>();
        for (int i = 0; i < LOADOUT_SLOTS.length; i++) {
            int index = page * Loadouts.PER_PAGE + i;
            if (index < Loadouts.MAX) slots.put(LOADOUT_SLOTS[i], index);
        }
        return slots;
    }

    /** {@link #icons(int, StorageTables.Power, int, int, StorageTables)} with no Tuning Points put in. */
    static Map<Integer, Icon> icons(int page, StorageTables.Power power, int accessoryPower, StorageTables tables) {
        return icons(page, power, accessoryPower, 0, tables);
    }

    /**
     * The slots that don't show what they wear or a loadout: the tree slots, the empty ones, the Power Stone, Stats Tuning
     * (with the {@code assigned} Tuning Points they've put in, see {@link StatsTuning}), the arrows.
     */
    static Map<Integer, Icon> icons(int page, StorageTables.Power power, int accessoryPower, int assigned, StorageTables tables) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(HOTF, tree("Forest", HOTF_HEAD));
        icons.put(HOTM, tree("Mountain", HOTM_HEAD));
        // As the wiki's Loadouts/UI shows them empty; a piece they wear takes their place.
        icons.put(NECKLACE, empty("&7Empty Equipment Slot", "&8> Necklace"));
        icons.put(HELMET, empty("&7Empty Helmet Slot"));
        icons.put(CLOAK, empty("&7Empty Equipment Slot", "&8> Cloak"));
        icons.put(CHESTPLATE, empty("&7Empty Chestplate Slot"));
        icons.put(PET, empty("&7Empty Pet Slot"));
        icons.put(BELT, empty("&7Empty Equipment Slot", "&8> Belt"));
        icons.put(LEGGINGS, empty("&7Empty Leggings Slot"));
        icons.put(GLOVES, empty("&7Empty Equipment Slot", "&8> Gloves", "&8> Bracelet"));
        icons.put(BOOTS, empty("&7Empty Boots Slot"));
        icons.put(POWER_STONE, powerStone(power, accessoryPower, tables));
        // What isn't put in (Stats Tuning) is unassigned: "57!!!" as recorded (UNKNOWN: whether 0 gets the "!!!").
        int tuning = Accessories.tuningPoints(accessoryPower);
        int unassigned = Math.max(0, tuning - assigned);
        icons.put(STATS_TUNING, new Icon(Material.COMPARATOR, "&aStats Tuning", "&7Optimize your build to your liking by using",
                "&eTuning Points&7.", "", "&7Every &610 MP &7grants &e1 Tuning Point&7.", "", "&7Magical Power: &6" + accessoryPower,
                "&7Tuning Points: &e" + tuning, "&7Unassigned Points: &c" + unassigned + (unassigned > 0 ? "!!!" : ""), "", "&eClick to view!"));
        if (page > 0) icons.put(PREVIOUS, new Icon(Material.ARROW, "&aPrevious Page", "&ePage " + page));
        if (page < PAGES - 1) icons.put(NEXT, new Icon(Material.ARROW, "&aNext Page", "&ePage " + (page + 2)));
        return icons;
    }

    /** "Quickly swap between saved trees": everyone has the one tree for now. */
    private static Icon tree(String what, String head) {
        return new Icon(Material.PLAYER_HEAD, "&aHeart of the " + what + " Slot", List.of("&7Quickly swap between saved trees.", "",
                "&7Current: &aHeart of the " + what + " 1", "", "&cSwapping trees has a 10m cooldown!", "", "&eClick to view!"), head);
    }

    private static Icon empty(String name, String... above) {
        List<String> lore = new ArrayList<>(List.of(above));
        lore.addAll(List.of("", "&eClick to select!"));
        return new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, name, lore);
    }

    /** "Choose your selected Power Stone.", with its stats; the wiki's "Current: &8None" without one. */
    static Icon powerStone(StorageTables.Power power, int accessoryPower, StorageTables tables) {
        List<String> lore = new ArrayList<>(List.of("&7Choose your selected Power Stone.", ""));
        if (power == null) {
            lore.addAll(List.of("&7Current: &8None", ""));
        } else {
            lore.addAll(List.of("&7Current: &a" + power.name(), "", "&7Stats:"));
            lore.addAll(Powers.statLines(power, accessoryPower, tables));
            lore.add("");
            if (!power.bonus().isEmpty()) {
                lore.add("&7Unique Power Bonus:");
                lore.addAll(Powers.bonusLines(power));
                lore.add("");
            }
        }
        lore.add("&eClick to view!");
        return new Icon(Material.PLAYER_HEAD, "&aPower Stone", lore, Bag.ACCESSORY_BAG.texture());
    }

    /** The Power Stone slot shows the selected power's stone (recorded: Silky's Luxurious Spool), else the bag (UNKNOWN). */
    static ItemStack powerLook(ItemStack stack, StorageTables.Power power) {
        SkyBlockItem stone = power == null || !power.stonePower() ? null : net.icxd.dungeons.item.ItemRegistry.get(power.stone());
        if (stone == null || stone.skin() == null) return stack;
        net.icxd.dungeons.utils.Utils.skull(stack, stone.skin());
        return stack;
    }

    /**
     * A loadout's lore, as recorded: its armor and equipment by name (of the sets it names), then the parts
     * that aren't here yet as None, its power, and what a click does.
     */
    static List<String> lore(Loadouts.Loadout loadout, List<String> armor, List<String> equipment, boolean equipped) {
        List<String> lore = new ArrayList<>();
        for (int i = 0; i < 4; i++) lore.add("&7" + Equipment.ARMOR.get(i) + ": " + name(armor, i));
        lore.add("");
        for (int i = 0; i < 4; i++) lore.add("&7" + Equipment.SLOTS.get(i) + ": " + name(equipment, i));
        lore.add("");
        lore.addAll(List.of("&7Pet: &8None", "&7HOTM: &8None", "&7HOTF: &8None",
                "&7Power Stone: " + (loadout.power() == null ? "&8None" : "&a" + loadout.power()), "&7Tuning Template Slot: &8None", ""));
        if (!loadout.customized()) {
            lore.addAll(List.of("&eRight-click to edit", "", "&cYou must customize this loadout", "&cbefore you can equip it!"));
            return lore;
        }
        if (!equipped) lore.add("&eLeft-click to equip!");
        lore.add("&eRight-click to edit");
        return lore;
    }

    private static String name(List<String> names, int i) {
        return names == null || names.get(i) == null ? "&8None" : names.get(i);
    }

    /** A loadout past their rank's slots. */
    static Icon locked(int index, Rank rank) {
        return new Icon(Material.RED_DYE, "&cLoadout " + (index + 1) + " Locked", Loadouts.unlockLines(rank, Loadouts::loadouts));
    }

    /** A loadout's sets' pieces: the items (for its icon) and their names (null for none, or no set). */
    record Parts(List<ItemStack> armor, List<String> armorNames, List<String> equipmentNames) {
    }

    /** The pieces of the sets a loadout names: those on them if it's the worn set, else as stored. */
    static Parts parts(Player player, Document storage, Loadouts.Loadout loadout, int wornArmor, int wornEquipment) {
        List<ItemStack> armor = null;
        if (loadout.armor() != null) {
            armor = loadout.armor() == wornArmor ? LoadoutActions.armor(player.getInventory())
                    : LoadoutActions.peek(StorageDocument.set(storage, StorageDocument.ARMOR_SETS, loadout.armor()));
        }
        List<ItemStack> equipment = null;
        if (loadout.equipment() != null) {
            equipment = loadout.equipment() == wornEquipment ? List.of(Equipment.worn(player))
                    : LoadoutActions.peek(StorageDocument.set(storage, StorageDocument.EQUIPMENT_SETS, loadout.equipment()));
        }
        return new Parts(armor, names(armor), names(equipment));
    }

    /** Each piece's name as its item shows it ("&dAncient Necron's Helmet &6✪✪✪✪✪"). */
    private static List<String> names(List<ItemStack> pieces) {
        if (pieces == null) return null;
        List<String> names = new ArrayList<>();
        for (ItemStack piece : pieces) {
            SkyBlockItem item = StorageItems.skyBlockItem(piece);
            names.add(item != null ? ItemBuilder.name(item, StorageItems.tag(piece)) : piece == null || piece.isEmpty() ? null
                    : "&f" + net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(piece.effectiveName()));
        }
        return names;
    }

    /**
     * A loadout's icon: its armor set's helmet (recorded: Loadout 1 was the Necron helmet, 3 the Haymaker's
     * hay bale), named and described as the loadout; gray dye until it's set up; a barrel for one without a
     * helmet (UNKNOWN).
     */
    static ItemStack look(Loadouts.Loadout loadout, Parts parts, List<String> lore) {
        ItemStack helmet = parts.armor() == null ? null : parts.armor().get(0);
        ItemStack look = !loadout.customized() ? new ItemStack(Material.GRAY_DYE)
                : helmet != null && !helmet.isEmpty() ? helmet.clone() : new ItemStack(Material.BARREL);
        look.setAmount(1);
        ItemMeta meta = look.getItemMeta();
        meta.displayName(Text.line("&a" + loadout.name()));
        meta.lore(Text.lines(lore));
        look.setItemMeta(meta);
        return look;
    }
}
