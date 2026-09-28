package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.SignInput;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * A loadout's page (right-click it in Loadouts), as recorded (the Loadouts and Storage tour, 00:50.7 and
 * 01:22.3): its armor set's four pieces and its equipment set's, its pet, tree slots, Power Stone and
 * Stats Tuning slot, each "Left-click to change!" (the Armor Sets, Equipment Sets or Select Power Stone
 * menu) and, once set, "Right-click to clear!" (the whole set: recorded, clearing the helmet cleared all
 * four); Clear sets it back as it was new, Rename Loadout asks for a name on a sign. The pet, trees and
 * tuning aren't here yet, so those slots always say None and do nothing. Main thread.
 */
final class LoadoutMenu extends GUI {
    static final int NECKLACE = 10;
    static final int HELMET = 11;
    static final int CLOAK = 19;
    static final int CHESTPLATE = 20;
    static final int PET = 21;
    static final int HOTM = 23;
    static final int HOTF = 24;
    static final int BELT = 28;
    static final int LEGGINGS = 29;
    static final int POWER_STONE = 32;
    static final int STATS_TUNING = 33;
    static final int GLOVES = 37;
    static final int BOOTS = 38;
    static final int GO_BACK = 48;
    static final int CLOSE = 49;
    static final int CLEAR = 50;
    static final int RENAME = 51;
    static final int[] ARMOR_SLOTS = {HELMET, CHESTPLATE, LEGGINGS, BOOTS};
    static final int[] EQUIPMENT_SLOTS = {NECKLACE, CLOAK, BELT, GLOVES};
    /** The sign's lines under what's typed (UNKNOWN: Hypixel's; these are the item browser's search sign's kind). */
    private static final List<String> RENAME_SIGN = List.of("", "^^^^^^^^^^^^^^^", "Enter a name", "");
    /** Longer names are cut (UNKNOWN: Hypixel's limit). */
    static final int NAME_LENGTH = 16;

    private final Player viewer;
    private final int index;

    LoadoutMenu(Player viewer, int index) {
        super(title(viewer, index), Size.SIX);
        this.viewer = viewer;
        this.index = index;
    }

    /** The loadout's name ("Loadout 2", as recorded). */
    private static String title(Player viewer, int index) {
        User user = User.ifLoaded(viewer.getUniqueId());
        return user == null ? Loadouts.defaultName(index) : Loadouts.get(user.profile(), index).name();
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Document profile = user.profile();
        Document storage = StoredInventory.storage(profile);
        Loadouts.Loadout loadout = Loadouts.get(profile, index);
        int ap = AccessoryBag.accessoryPower(viewer);
        StorageTables.Power power = StorageTables.get().power(loadout.power());

        for (Map.Entry<Integer, Icon> e : icons(loadout, power, ap, StorageTables.get()).entrySet()) {
            int slot = e.getKey();
            ItemStack stack = e.getValue().stack();
            if (slot == POWER_STONE) stack = LoadoutsMenu.powerLook(stack, power);
            set(slot, stack);
        }
        LoadoutsMenu.Parts parts = LoadoutsMenu.parts(viewer, storage, loadout, StorageDocument.worn(storage, StorageDocument.WORN_ARMOR_SET),
                StorageDocument.worn(storage, StorageDocument.WORN_EQUIPMENT_SET));
        List<ItemStack> equipment = equipmentPieces(storage, loadout);
        for (int i = 0; i < 4; i++) {
            ItemStack piece = parts.armor() == null ? null : parts.armor().get(i);
            if (piece != null && !piece.isEmpty()) set(ARMOR_SLOTS[i], withClicks(piece));
            ItemStack worn = equipment == null ? null : equipment.get(i);
            if (worn != null && !worn.isEmpty()) set(EQUIPMENT_SLOTS[i], withClicks(worn));
        }
        for (int slot : ARMOR_SLOTS) button(slot, () -> new SetsMenu(viewer, index, SetsMenu.Kind.ARMOR, 0).open(viewer),
                () -> change(l -> l.withArmor(null)));
        for (int slot : EQUIPMENT_SLOTS) button(slot, () -> new SetsMenu(viewer, index, SetsMenu.Kind.EQUIPMENT, 0).open(viewer),
                () -> change(l -> l.withEquipment(null)));
        button(POWER_STONE, () -> new PowerMenu(viewer, index).open(viewer), () -> change(l -> l.withPower(null)));
        set(GUIClickableItem.button(GO_BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Loadouts").stack(), viewer,
                () -> new LoadoutsMenu(viewer, index / Loadouts.PER_PAGE).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
        set(GUIClickableItem.button(CLEAR, new Icon(Material.LAVA_BUCKET, "&cClear", "&7Clear all settings in this loadout,",
                "&7restoring it back to default.", "", "&eClick to clear!").stack(), viewer, () -> change(l -> Loadouts.fresh(index))));
        set(GUIClickableItem.button(RENAME, rename(loadout.name()).stack(), viewer, this::rename));
    }

    /** The equipment set's pieces: those they wear if it's the worn set, else as stored; null for no set. */
    private List<ItemStack> equipmentPieces(Document storage, Loadouts.Loadout loadout) {
        if (loadout.equipment() == null) return null;
        if (loadout.equipment() == StorageDocument.worn(storage, StorageDocument.WORN_EQUIPMENT_SET)) return List.of(Equipment.worn(viewer));
        return LoadoutActions.peek(StorageDocument.set(storage, StorageDocument.EQUIPMENT_SETS, loadout.equipment()));
    }

    /** The slots that aren't a set's piece, and the pieces' empty look. */
    static Map<Integer, Icon> icons(Loadouts.Loadout loadout, StorageTables.Power power, int accessoryPower, StorageTables tables) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        for (int i = 0; i < 4; i++) {
            icons.put(ARMOR_SLOTS[i], emptyArmor(Equipment.ARMOR.get(i), loadout.armor() != null));
            icons.put(EQUIPMENT_SLOTS[i], emptyEquipment(i, loadout.equipment() != null));
        }
        icons.put(PET, new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&aPet", "&7Select a pet to use in this loadout!", "",
                "&7Current: &8None", "", "&eLeft-click to change!"));
        icons.put(HOTM, tree("Mountain", "&7Select a Heart of the Mountain to", "&7use in this loadout!", LoadoutsMenu.HOTM_HEAD));
        icons.put(HOTF, tree("Forest", "&7Select a Heart of the Forest to use", "&7in this loadout!", LoadoutsMenu.HOTF_HEAD));
        icons.put(POWER_STONE, powerStone(power, accessoryPower, tables));
        icons.put(STATS_TUNING, new Icon(Material.COMPARATOR, "&aStats Tuning Slot", "&7Select a Stats Tuning template slot",
                "&7to use in this loadout!", "", "&7Current: &8None", "", "&eLeft-click to change!"));
        return icons;
    }

    /**
     * A piece the set doesn't have, as recorded for a loadout with no armor ("No armor selected for this
     * loadout."); with a set that just lacks the piece, UNKNOWN, the same with "Right-click to clear!".
     */
    static Icon emptyArmor(String piece, boolean set) {
        List<String> lore = new ArrayList<>(List.of("", "&7No armor selected for this loadout.", "", "&eLeft-click to change!"));
        if (set) lore.add("&eRight-click to clear!");
        return new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Empty " + piece + " Slot", lore);
    }

    static Icon emptyEquipment(int slot, boolean set) {
        List<String> lore = new ArrayList<>(slot == 3 ? List.of("&8> Gloves", "&8> Bracelet") : List.of("&8> " + Equipment.SLOTS.get(slot)));
        lore.addAll(List.of("", "&7No equipment selected for this", "&7loadout.", "", "&eLeft-click to change!"));
        if (set) lore.add("&eRight-click to clear!");
        return new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Empty Equipment Slot", lore);
    }

    private static Icon tree(String what, String line1, String line2, String head) {
        return new Icon(Material.PLAYER_HEAD, "&aHeart of the " + what + " Slot", List.of(line1, line2, "", "&7Selected: &8None", "",
                "&cSwapping trees has a 10m cooldown!", "", "&eLeft-click to change!"), head);
    }

    /** "Select a Power Stone to use in this loadout!", with its stats (recorded) or None. */
    static Icon powerStone(StorageTables.Power power, int accessoryPower, StorageTables tables) {
        List<String> lore = new ArrayList<>(List.of("&7Select a Power Stone to use in this", "&7loadout!", ""));
        if (power == null) {
            lore.addAll(List.of("&7Current: &8None", "", "&eLeft-click to change!"));
        } else {
            lore.addAll(List.of("&7Current: &a" + power.name(), "", "&7Stats:"));
            lore.addAll(Powers.statLines(power, accessoryPower, tables));
            lore.add("");
            if (!power.bonus().isEmpty()) {
                lore.add("&7Unique Power Bonus:");
                lore.addAll(Powers.bonusLines(power));
                lore.add("");
            }
            lore.addAll(List.of("&eLeft-click to change!", "&eRight-click to clear!"));
        }
        return new Icon(Material.PLAYER_HEAD, "&aPower Stone", lore, Bag.ACCESSORY_BAG.texture());
    }

    static Icon rename(String name) {
        return new Icon(Material.NAME_TAG, "&aRename Loadout", "&7Want to feel a more personal", "&7connection with your loadout slot?",
                "&7Give it a name!", "", "&7Current Name: &a" + name, "", "&eClick to rename!");
    }

    /** A piece as its item, with the recorded "Left-click to change!" and "Right-click to clear!" under its lore. */
    private static ItemStack withClicks(ItemStack piece) {
        ItemStack look = piece.clone();
        look.setAmount(1);
        ItemMeta meta = look.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.addAll(Text.lines(List.of("", "&eLeft-click to change!", "&eRight-click to clear!")));
        meta.lore(lore);
        look.setItemMeta(meta);
        return look;
    }

    /** A slot that opens a menu on a left click and clears its part on a right one. */
    private void button(int slot, Runnable change, Runnable clear) {
        ItemStack stack = get(slot).stack();
        set(GUIClickableItem.button(slot, stack, viewer, click -> {
            if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) clear.run();
            else change.run();
        }));
    }

    /** Changes the loadout, and shows it again. */
    private void change(java.util.function.UnaryOperator<Loadouts.Loadout> how) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Loadouts.put(user.profile(), index, how.apply(Loadouts.get(user.profile(), index)));
        new LoadoutMenu(viewer, index).open(viewer);
    }

    /** Asks for a name on a sign; an empty one keeps the name it has. */
    private void rename() {
        boolean opened = SignInput.open(viewer, RENAME_SIGN, lines -> {
            // Without colour codes: a name is shown in the loadout's green.
            String typed = lines.isEmpty() ? "" : lines.getFirst().replaceAll("[&§][0-9a-fk-orA-FK-OR]", "").trim();
            if (!typed.isEmpty()) {
                User user = User.ifLoaded(viewer.getUniqueId());
                if (user != null) {
                    String name = typed.length() > NAME_LENGTH ? typed.substring(0, NAME_LENGTH) : typed;
                    Loadouts.put(user.profile(), index, Loadouts.get(user.profile(), index).withName(name));
                }
            }
            if (viewer.isOnline()) new LoadoutMenu(viewer, index).open(viewer);
        });
        if (!opened) new LoadoutMenu(viewer, index).open(viewer);
    }
}
