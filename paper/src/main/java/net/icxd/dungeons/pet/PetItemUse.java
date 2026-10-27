package net.icxd.dungeons.pet;

import java.util.Locale;

import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Giving a summoned pet a Pet Item: "To use them, equip a pet and right-click the pet while holding a pet item" (the
 * wiki's Pet Items), here looking at its head within reach (see {@link PetFollowers#lookingAtOwn}). A pet holding none
 * takes it: "Your pet is now holding Burnt Texts." (SkyHanni's test lines; their colours UNKNOWN). One holding another
 * opens Swap Pet Item ({@link SwapPetItemMenu}) for coins by the replaced item's rarity, and the old one comes back; a
 * Super Scrubber opens it to take the item off ("You removed Quick Claw from your pet!", SkyHanni's; "&cYour Pet isn't
 * holding any item!" with none, the wiki's). One that isn't for the pet: "&cThis item is not compatible with your pet!"
 * (the wiki's Unalloyed Speed). Pet Items are never placed. Pet Food goes the same way ({@link PetFood}), and pet skins
 * ({@link #applySkin}). Main thread.
 */
public final class PetItemUse implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        String id = id(event.getItem());
        PetFood.Food food = id == null ? PetFood.of(event.getItem()) : null;
        PetData.Skin skin = id == null && food == null ? skin(event.getItem()) : null;
        if (id == null && food == null && skin == null) return;
        Player player = event.getPlayer();
        if (!PetFollowers.lookingAtOwn(player)) return;
        event.setCancelled(true);
        if (id != null) use(player, id);
        else if (food != null) PetFood.offer(player, food);
        else applySkin(player, skin);
    }

    /** The pet skin this item is (PET_SKIN_ and its id, as NEU has them); null if it isn't one the data has. */
    static PetData.Skin skin(ItemStack stack) {
        NBTTagCompound tag = ItemNBT.read(stack);
        String id = tag == null ? null : tag.getString("id");
        return id != null && id.startsWith(SKIN_PREFIX) ? Pets.data().skin(id.substring(SKIN_PREFIX.length())) : null;
    }

    static final String SKIN_PREFIX = "PET_SKIN_";

    /**
     * A pet skin on their summoned pet: "Right-click on your summoned pet to apply this skin!" (its lines). It can't be
     * taken off, and one put on in its place is gone (the wiki's Pet Skins). Not for this pet: the Pet Items' line; what
     * it says when it's on, UNKNOWN.
     */
    static void applySkin(Player player, PetData.Skin skin) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player)) return;
        Document profile = user.profile();
        OwnedPet pet = Pets.active(profile);
        if (pet == null) return;
        if (!skin.pets().contains(pet.type())) {
            player.sendMessage(Text.line("&cThis item is not compatible with your pet!"));
            return;
        }
        if (skin.id().equals(pet.skin())) {
            player.sendMessage(Text.line("&cYour pet already has this skin!"));
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (skin(hand) != skin) return;
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else player.getInventory().setItemInMainHand(null);
        Pets.replace(profile, pet.withSkin(skin.id()));
        player.sendMessage(Text.line("&aYou applied the &" + skin.color() + skin.name() + " &ato your pet!"));
        PetFollowers.refresh(player);
        user.save();
    }

    /** Pet Items, Pet Food (the Pet Cake) and pet skins (heads) are never placed. */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (id(item) != null || PetFood.of(item) != null || skin(item) != null) event.setCancelled(true);
    }

    /** The id of a Pet Item that's given to pets (not one used at Kat), or the Super Scrubber's; null for anything else. */
    static String id(ItemStack stack) {
        NBTTagCompound tag = ItemNBT.read(stack);
        if (tag == null || tag.getString("id") == null) return null;
        String id = tag.getString("id").toUpperCase(Locale.ROOT);
        if (id.equals(HeldItems.SCRUBBER)) return id;
        SkyBlockItem item = ItemRegistry.get(id);
        return item != null && item.specificItemType() == SpecificItemType.PET_ITEM && HeldItems.effect(id).appliable() ? id : null;
    }

    /** A Pet Item (or the Super Scrubber) used on their summoned pet. */
    static void use(Player player, String id) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player)) return;
        Document profile = user.profile();
        PetData data = Pets.data();
        OwnedPet pet = Pets.active(profile);
        // "The pet must be visible to apply the item!" (every Pet Item's lines): hidden, nothing happens (UNKNOWN).
        if (pet == null || Pets.visibility(profile) == Pets.Visibility.HIDE_ALL) return;
        if (id.equals(HeldItems.SCRUBBER)) {
            if (pet.heldItem() == null) player.sendMessage(Text.line("&cYour Pet isn't holding any item!"));
            else new SwapPetItemMenu(player, pet, null).open(player);
            return;
        }
        Pet kind = data.pet(pet.type());
        // A dragon's egg "cannot hold Pet Items" (the wiki's Jade Dragon Pet); what it says, UNKNOWN: this.
        if (kind == null || Pets.level(pet, data) < kind.statsFrom() || !HeldItems.compatible(id, pet.type(), kind.name(Pets.tier(pet, data)))) {
            player.sendMessage(Text.line("&cThis item is not compatible with your pet!"));
            return;
        }
        if (id.equals(pet.heldItem())) {
            // UNKNOWN: what Hypixel says.
            player.sendMessage(Text.line("&cYour pet is already holding this item!"));
            return;
        }
        if (pet.heldItem() == null) {
            if (take(player, id)) held(player, user, pet, id);
            return;
        }
        new SwapPetItemMenu(player, pet, id).open(player);
    }

    /**
     * Swap Pet Item confirmed: the new item (null: the Super Scrubber took it off) for its cost, the old one back. With
     * too few coins, UNKNOWN what Hypixel says: "&cYou don't have enough coins!".
     */
    static void swap(Player player, String id) {
        player.closeInventory();
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player)) return;
        Document profile = user.profile();
        OwnedPet pet = Pets.active(profile);
        if (pet == null || pet.heldItem() == null || pet.heldItem().equals(id)) return;
        double cost = cost(pet.heldItem());
        if (!Purse.has(user, cost)) {
            player.sendMessage(Text.line("&cYou don't have enough coins!"));
            return;
        }
        if (!take(player, id == null ? HeldItems.SCRUBBER : id)) return;
        Purse.take(user, cost);
        String old = pet.heldItem();
        SkyBlockItem refund = ItemRegistry.get(old);
        if (refund != null) ItemStash.give(player, ItemBuilder.build(refund));
        if (id == null) {
            Pets.replace(profile, pet.withHeldItem(null));
            player.sendMessage(Text.line("&aYou removed " + name(old) + " &afrom your pet!"));
            changed(player, user);
        } else {
            held(player, user, pet, id);
        }
    }

    /**
     * "The cost of the swap process depends upon the rarity of the item being replaced" (0.24.1): Common 25,000 to Divine
     * 2,500,000 (and that for anything rarer, UNKNOWN).
     */
    static double cost(String replaced) {
        SkyBlockItem item = ItemRegistry.get(replaced);
        Rarity rarity = item == null ? Rarity.COMMON : item.rarity();
        return switch (rarity) {
            case COMMON -> 25_000;
            case UNCOMMON -> 50_000;
            case RARE -> 100_000;
            case EPIC -> 250_000;
            case LEGENDARY -> 500_000;
            case MYTHIC -> 1_000_000;
            default -> 2_500_000;
        };
    }

    /** "&5Crochet Tiger Plushie": an item's name in its rarity's colour. */
    static String name(String id) {
        SkyBlockItem item = ItemRegistry.get(id);
        return item == null ? "&f" + id : "&" + item.rarity().getCode() + item.name();
    }

    /** One of the item they're using out of their hand; false if it isn't in their hand any more. */
    private static boolean take(Player player, String id) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!id.equals(id(hand))) return false;
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else player.getInventory().setItemInMainHand(null);
        return true;
    }

    private static void held(Player player, User user, OwnedPet pet, String id) {
        Pets.replace(user.profile(), pet.withHeldItem(id));
        player.sendMessage(Text.line("&aYour pet is now holding " + name(id) + "&a."));
        changed(player, user);
    }

    /** What the pet's item changes: their stats, its name and head (a Tier Boost's rarity), and the saved profile. */
    private static void changed(Player player, User user) {
        PlayerSession.of(player).invalidateStats();
        PetFollowers.refresh(player);
        user.save();
    }
}
