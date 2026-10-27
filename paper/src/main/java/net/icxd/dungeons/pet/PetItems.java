package net.icxd.dungeons.pet;

import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.behaviour.ItemBehaviour;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.pet.PetText.Where;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * Pets as items: Hypixel's PET item, which is whichever pet its {@code petInfo} says ({@link #PET_INFO}; as Hypixel's are, recorded: a
 * bought Bee, "&7[Lvl 1] &fBee", its head, and the lore that ends "&eRight-click to add this pet to your pet menu!").
 */
public final class PetItems implements ItemBehaviour {
    public static final String ID = "PET";
    /** Where the pet is kept on the item: Hypixel's {@code petInfo} (item data keys here are lowercase). */
    public static final String PET_INFO = "pet_info";
    /** Hypixel's own key for it, as the API's items have it. */
    public static final String HYPIXEL_PET_INFO = "petInfo";

    @Override
    public Shown shown(SkyBlockItem item, NBTTagCompound tag) {
        OwnedPet pet = pet(tag);
        if (pet == null) return null;
        PetData data = Pets.data();
        String texture = Pets.texture(pet, data);
        return new Shown(PetText.name(pet, data, Where.ITEM), PetText.lore(pet, data, Where.ITEM, false),
                texture == null ? null : Utils.texture(texture));
    }

    /** The pet an item's data is; null if it isn't one. */
    public static OwnedPet pet(NBTTagCompound tag) {
        if (tag == null || !ID.equals(tag.getString("id"))) return null;
        return OwnedPet.fromPetInfo(tag.getString(PET_INFO), tag.getString("uuid"));
    }

    /** A pet as an item (out of the menu, bought, yoinked). */
    public static ItemStack item(OwnedPet pet) {
        SkyBlockItem kind = ItemRegistry.get(ID);
        if (kind == null) throw new IllegalStateException("no PET item in the items data");
        return ItemBuilder.build(kind, data(kind, pet));
    }

    /** A new pet of this type: its lowest rarity, level 1 ("New pets are lowest level!", Bea's Bee Pet); null for none. */
    public static OwnedPet newPet(String type) {
        Pet kind = Pets.data().pet(type);
        if (kind == null || kind.variants().isEmpty()) return null;
        return OwnedPet.of(kind.id(), kind.variants().keySet().iterator().next());
    }

    /**
     * A new pet of this type as a shop shows it, as recorded (Bea's Bee Pet): "&7[Lvl 100] &fBee", its level 100's stats
     * and abilities, "&cThis is a preview of Lvl 100." and "&cNew pets are lowest level!"; null for none.
     */
    public static ItemStack preview(String type) {
        OwnedPet pet = newPet(type);
        if (pet == null) return null;
        PetData data = Pets.data();
        OwnedPet shown = pet.withExp(data.totalXp(data.pet(type), pet.tier(), 100));
        ItemStack stack = item(shown);
        stack.setData(DataComponentTypes.CUSTOM_NAME, Text.line(PetText.name(shown, data, Where.SHOP)));
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(Text.lines(PetText.lore(shown, data, Where.SHOP, false))));
        return stack;
    }

    /** "&fBee Pet": a pet's name as a shop's line says it bought (recorded). */
    public static String shopName(OwnedPet pet) {
        return PetText.colored(pet, Pets.data()) + " Pet";
    }

    /** The item's data for a pet: a new PET item's, its {@code petInfo}, and the pet's own uuid. */
    public static NBTTagCompound data(SkyBlockItem kind, OwnedPet pet) {
        NBTTagCompound tag = ItemBuilder.newData(kind);
        tag.setString(PET_INFO, pet.petInfo());
        tag.setString("uuid", pet.uuid());
        return tag;
    }
}
