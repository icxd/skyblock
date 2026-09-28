package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;

/**
 * Equipping a loadout: its armor set onto their armor slots and its equipment set into their equipment
 * slots (see {@link Wardrobe}: what they wore goes back to the set it came from, or into their inventory),
 * and its power selected; a None part stays as it is. All of it or nothing: if what they wear has to go
 * into their inventory and there's no room, nothing changes. Items only move, so none is made or lost:
 * an armor set with a piece that can't be read isn't put on (it stays in the set), and a piece they wear
 * that storage doesn't keep (see StoredInventory#saved) goes into their inventory. Main thread.
 */
final class LoadoutActions {
    private LoadoutActions() {
    }

    /** Equips loadout {@code index} (from 0); the message to tell them, red if it didn't. */
    static String equip(Player player, int index) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || InventorySyncListener.frozen(player)) return null;
        Document profile = user.profile();
        Document storage = StoredInventory.storage(profile);
        Loadouts.Loadout loadout = Loadouts.get(profile, index);
        if (!loadout.customized()) return "&c" + loadout.name() + " needs to be customized before you can equip it!";

        // What changes, worked out first.
        int wornArmor = StorageDocument.worn(storage, StorageDocument.WORN_ARMOR_SET);
        int wornEquipment = StorageDocument.worn(storage, StorageDocument.WORN_EQUIPMENT_SET);
        Integer armorSet = usable(loadout.armor(), Loadouts.armorSets(user.getRank()));
        Integer equipmentSet = usable(loadout.equipment(), Loadouts.equipmentSets(user.getRank()));
        PlayerInventory inventory = player.getInventory();

        Wardrobe.Swap<ItemStack> armor = null;
        List<ItemStack> kept = new ArrayList<>();
        if (armorSet != null && armorSet != wornArmor) {
            List<Object> blobs = StorageDocument.set(storage, StorageDocument.ARMOR_SETS, armorSet);
            List<ItemStack> incoming = peek(blobs);
            for (int i = 0; i < 4; i++) {
                // What Hypixel says then is UNKNOWN (it can't happen there).
                if (blobs.get(i) != null && incoming.get(i) == null) return "&cThis armor set can't be put on right now.";
            }
            armor = Wardrobe.swap(wornArmor, armorSet, armor(inventory), incoming, ItemStack::isEmpty);
            if (armor.stored() != null) {
                for (int i = 0; i < 4; i++) {
                    ItemStack piece = armor.stored().get(i);
                    if (piece == null || piece.isEmpty() || StoredInventory.saved(piece)) continue;
                    kept.add(piece);
                    armor.stored().set(i, null);
                }
            }
        }

        List<Object> wornBlobs = StorageDocument.slots(storage, StorageDocument.EQUIPMENT, 4);
        Wardrobe.Swap<Object> equipment = equipmentSet == null ? null
                : Wardrobe.swap(wornEquipment, equipmentSet, wornBlobs, StorageDocument.set(storage, StorageDocument.EQUIPMENT_SETS, equipmentSet),
                blob -> false);

        int needed = kept.size() + (armor == null ? 0 : armor.toInventory().size()) + (equipment == null ? 0 : equipment.toInventory().size());
        if (free(inventory) < needed) {
            // What Hypixel says then is UNKNOWN.
            return "&cYou don't have enough space in your inventory to equip this loadout!";
        }

        // Then it all happens.
        List<ItemStack> toInventory = new ArrayList<>(kept);
        if (armor != null) {
            if (armor.stored() != null) {
                StorageDocument.setSet(storage, StorageDocument.ARMOR_SETS, wornArmor, StorageItems.encode(armor.stored().toArray(new ItemStack[0])));
            }
            toInventory.addAll(armor.toInventory());
            StorageDocument.setSet(storage, StorageDocument.ARMOR_SETS, armorSet, Arrays.asList(new Object[4]));
            wear(inventory, armor.wearing());
            StorageDocument.setWorn(storage, StorageDocument.WORN_ARMOR_SET, armor.worn());
        }
        if (equipment != null && equipment.worn() != wornEquipment) {
            if (equipment.stored() != null) StorageDocument.setSet(storage, StorageDocument.EQUIPMENT_SETS, wornEquipment, equipment.stored());
            for (Object blob : equipment.toInventory()) {
                ItemStack item = StoredInventory.read(player, blob, StorageDocument.EQUIPMENT, 0, storage, log());
                if (item != null) toInventory.add(item);
            }
            StorageDocument.setSet(storage, StorageDocument.EQUIPMENT_SETS, equipmentSet, Arrays.asList(new Object[4]));
            StorageDocument.setSlots(storage, StorageDocument.EQUIPMENT, equipment.wearing());
            StorageDocument.setWorn(storage, StorageDocument.WORN_EQUIPMENT_SET, equipment.worn());
            Equipment.forget(player.getUniqueId());
            SetBonuses.refreshLore(player);
        }
        // There's room (see above); should there not be after all, the rest goes to their stash.
        if (!toInventory.isEmpty()) ItemStash.give(player, toInventory.toArray(new ItemStack[0]));
        if (loadout.power() != null && StorageTables.get().power(loadout.power()) != null) AccessoryBag.select(profile, loadout.power());
        AccessoryBag.changed(player);
        PlayerSession.of(player).invalidateStats();
        user.save();
        return "&aYou equipped " + loadout.name() + "!";
    }

    /** A set the loadout names, if they still have it (a slot that's locked now counts as None). */
    private static Integer usable(Integer set, int unlocked) {
        return set == null || set < 0 || set >= unlocked ? null : set;
    }

    /** Helmet, chestplate, leggings, boots (Bukkit's armor contents go the other way). */
    static List<ItemStack> armor(PlayerInventory inventory) {
        return new ArrayList<>(Arrays.asList(inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots()));
    }

    private static void wear(PlayerInventory inventory, List<ItemStack> pieces) {
        inventory.setHelmet(pieces.get(0));
        inventory.setChestplate(pieces.get(1));
        inventory.setLeggings(pieces.get(2));
        inventory.setBoots(pieces.get(3));
    }

    /** Stored pieces as items (null for none, or one that can't be read). */
    static List<ItemStack> peek(List<Object> blobs) {
        List<ItemStack> out = new ArrayList<>(blobs.size());
        for (Object blob : blobs) out.add(StorageItems.peek(blob));
        return out;
    }

    /** Empty slots among the 36 of their inventory. */
    private static int free(PlayerInventory inventory) {
        int free = 0;
        for (ItemStack item : inventory.getStorageContents()) if (item == null || item.isEmpty()) free++;
        return free;
    }

    private static Logger log() {
        return Dungeons.getInstance().getLogger();
    }
}
