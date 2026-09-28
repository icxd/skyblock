package net.icxd.dungeons.storage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;

/**
 * The four equipment slots (the wiki's Equipment: a necklace, a cloak, a belt, and gloves or a bracelet),
 * kept in the profile's storage ({@link StorageDocument#EQUIPMENT}) as there's no slot for them on the
 * player; the pieces worn count for their stats (PlayerStats#addModifier). They're put on by equipping a
 * loadout with an equipment set (see {@link Loadouts}); Hypixel's right-click to equip and the Stats &amp;
 * Equipment menu's slots aren't here yet. And which pieces go in which armor or equipment slot. Main thread.
 */
public final class Equipment {
    /** Each slot's name as the menus say it. */
    public static final List<String> SLOTS = List.of("Necklace", "Cloak", "Belt", "Gloves/Bracelet");
    /** And the armor's, top down. */
    public static final List<String> ARMOR = List.of("Helmet", "Chestplate", "Leggings", "Boots");
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final List<SpecificItemType> ARMOR_TYPES = List.of(SpecificItemType.HELMET, SpecificItemType.CHESTPLATE,
            SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);

    private record Worn(Document profile, ItemStack[] pieces) {
    }

    private static final Map<UUID, Worn> WORN = new HashMap<>();

    private Equipment() {
    }

    /** Once, at startup. */
    static void register() {
        PlayerStats.addModifier((player, stats) -> {
            for (ItemStack piece : worn(player)) stats.add(ItemStats.of(piece, player));
        });
        // Set bonuses that count equipment (Bloodrush's gloves) see what's worn here.
        SetBonuses.setEquipment(player -> Arrays.stream(worn(player)).filter(Objects::nonNull).toList());
    }

    /** Whether a piece goes in an equipment slot (0 to 3: necklace, cloak, belt, gloves or bracelet). */
    public static boolean fitsEquipment(int slot, SkyBlockItem item) {
        if (item == null) return false;
        SpecificItemType type = item.specificItemType();
        return switch (slot) {
            case 0 -> type == SpecificItemType.NECKLACE;
            case 1 -> type == SpecificItemType.CLOAK;
            case 2 -> type == SpecificItemType.BELT;
            case 3 -> type == SpecificItemType.GLOVES || type == SpecificItemType.BRACELET;
            default -> false;
        };
    }

    /** Whether a piece goes in an armor slot (0 to 3, helmet to boots): a SkyBlock item by its type, anything else by where it's worn. */
    public static boolean fitsArmor(int slot, SkyBlockItem item, Material material) {
        if (slot < 0 || slot > 3) return false;
        if (item != null) return item.specificItemType() == ARMOR_TYPES.get(slot);
        return material.isItem() && material.getEquipmentSlot() == ARMOR_SLOTS.get(slot);
    }

    /** What they wear in the four slots (null for an empty one); nothing until their data is loaded. */
    public static ItemStack[] worn(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null) return new ItemStack[4];
        Worn worn = WORN.get(player.getUniqueId());
        if (worn == null || worn.profile() != user.profile()) {
            List<Object> slots = StorageDocument.slots(StoredInventory.storage(user.profile()), StorageDocument.EQUIPMENT, 4);
            ItemStack[] pieces = new ItemStack[4];
            for (int i = 0; i < 4; i++) pieces[i] = StorageItems.peek(slots.get(i));
            worn = new Worn(user.profile(), pieces);
            WORN.put(player.getUniqueId(), worn);
        }
        return worn.pieces().clone();
    }

    static void forget(UUID player) {
        WORN.remove(player);
    }
}
