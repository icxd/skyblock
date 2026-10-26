package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.utils.Text;

/**
 * "Swap Pet Item", as the wiki's 0.24.1 screenshot shows it: five rows of glass, the pet's item and the new one side by
 * side, and Confirm Swap: "&7Replace &5Dwarf Turtle Shelmet &7with &5Crochet Tiger Plushie&7." / "&7You will be refunded
 * your &5Dwarf Turtle Shelmet&7." / "&7Cost" / "&6250,000 Coins" / "&eClick to swap!". Its Cancel (under the screenshot's
 * tooltip) is UNKNOWN, as is the Super Scrubber's menu: this one, "Remove" for "Replace" and the scrubber for the new
 * item. Main thread.
 */
final class SwapPetItemMenu extends GUI {
    static final String TITLE = "Swap Pet Item";
    static final int OLD = 12;
    static final int NEW = 14;
    static final int CONFIRM = 29;
    static final int CANCEL = 33;
    /** As wide as the pets' lines wrap (the screenshot's "Replace Dwarf Turtle Shelmet with" / "Crochet Tiger Plushie."). */
    static final int WIDTH = PetText.WIDTH;

    private final Player viewer;
    private final OwnedPet pet;
    /** The item going on; null for the Super Scrubber taking the pet's off. */
    private final String id;

    SwapPetItemMenu(Player viewer, OwnedPet pet, String id) {
        super(TITLE, Size.FIVE);
        this.viewer = viewer;
        this.pet = pet;
        this.id = id;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        ItemStack old = stack(pet.heldItem());
        if (old != null) set(OLD, old);
        ItemStack using = stack(id == null ? HeldItems.SCRUBBER : id);
        if (using != null) set(NEW, using);
        set(GUIClickableItem.button(CONFIRM, confirm(pet.heldItem(), id).stack(), viewer, () -> PetItemUse.swap(viewer, id)));
        set(GUIClickableItem.button(CANCEL, new Icon(Material.RED_TERRACOTTA, "&cCancel").stack(), viewer, () -> viewer.closeInventory()));
    }

    /** Confirm Swap's lines, for putting {@code id} on in place of {@code old} (null: taking it off). */
    static Icon confirm(String old, String id) {
        List<String> lore = new ArrayList<>();
        String what = id == null ? "&7Remove " + PetItemUse.name(old) + " &7from your pet." : "&7Replace " + PetItemUse.name(old) + " &7with "
                + PetItemUse.name(id) + "&7.";
        lore.addAll(Text.wrap(what, WIDTH));
        lore.add("");
        lore.addAll(Text.wrap("&7You will be refunded your " + PetItemUse.name(old) + "&7.", WIDTH));
        lore.add("");
        lore.add("&7Cost");
        lore.add("&6" + Text.number(PetItemUse.cost(old)) + " Coins");
        lore.add("");
        lore.add(id == null ? "&eClick to remove!" : "&eClick to swap!");
        return new Icon(Material.LIME_TERRACOTTA, id == null ? "&aConfirm Removal" : "&aConfirm Swap", lore);
    }

    private static ItemStack stack(String id) {
        SkyBlockItem item = id == null ? null : ItemRegistry.get(id);
        return item == null ? null : ItemBuilder.build(item);
    }
}
