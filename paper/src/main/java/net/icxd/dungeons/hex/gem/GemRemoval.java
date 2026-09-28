package net.icxd.dungeons.hex.gem;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexCosts.Check;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexScreen;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.gemstone.Gem;
import net.icxd.dungeons.item.gemstone.GemSlots;
import net.icxd.dungeons.item.gemstone.GemSlots.Slot;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.menu.Icon;

/**
 * Taking a gem out of a slot, confirmed first (the wiki's Gemstone Slot: a confirmation menu follows the click).
 * UNKNOWN: that menu's title, layout and words (no source shows it); here a small one of our own: Confirm in 11,
 * the gem in 13, Cancel in 15. Confirm pays the fee (nothing on a Sandbox profile, nor for a gem its slot no longer
 * takes) and gives the gem back (ItemStash: their inventory, else their stash), all in one save; both go back to
 * the grinder with the item, which the session holds meanwhile. Main thread.
 */
final class GemRemoval extends HexScreen {
    static final String TITLE = "Remove Gemstone";
    static final int CONFIRM = 11;
    static final int GEM = 13;
    static final int CANCEL = 15;
    static final Icon CANCEL_BUTTON = new Icon(Material.RED_TERRACOTTA, "&cCancel", "&7Back to the Gemstone Grinder.");

    private final int index;
    private final boolean fromHex;

    GemRemoval(HexSession session, int index, boolean fromHex) {
        super(session, TITLE, GUI.Size.THREE);
        this.index = index;
        this.fromHex = fromHex;
    }

    @Override
    protected void draw() {
        fill(filler());
        Slot slot = slot();
        if (slot != null && slot.gem() != null) {
            List<String> block = GemstoneGrinder.removalBlock(GemstoneGrinder.removalCost(slot), session, "&eClick to confirm!");
            set(button(CONFIRM, confirm(slot.gem(), block).stack(), this::confirmed));
            set(GEM, GemstoneGrinder.gemStack(slot.gem()));
        }
        set(button(CANCEL, CANCEL_BUTTON.stack(), this::back));
    }

    /** Confirm: what comes out, then the cost block. UNKNOWN: all of it (ours). */
    static Icon confirm(Gem gem, List<String> costBlock) {
        List<String> lore = new ArrayList<>(List.of("&7Remove the " + GemstoneGrinder.gemName(gem), "&7from this item.", ""));
        lore.addAll(costBlock);
        return new Icon(Material.GREEN_TERRACOTTA, "&aConfirm", lore);
    }

    /** The slot as it is now; null if the item hasn't it (or there's no item). */
    private Slot slot() {
        HexItem item = session.hexItem();
        List<Slot> slots = item == null ? List.of() : GemSlots.of(item.item(), item.tag());
        return index < slots.size() ? slots.get(index) : null;
    }

    private void confirmed() {
        HexItem item = session.hexItem();
        Slot slot = slot();
        if (item == null || slot == null || slot.gem() == null) {
            back();
            return;
        }
        HexCosts cost = GemstoneGrinder.removalCost(slot);
        if (session.sandbox()) cost = HexCosts.NOTHING;
        if (cost == null) {
            say(GemstoneGrinder.UNAVAILABLE);
            return;
        }
        List<Check> checks = cost.check(session);
        if (!checks.stream().allMatch(Check::owned)) {
            say(String.join(" ", GemstoneGrinder.missing(checks)));
            return;
        }
        SkyBlockItem gemItem = ItemRegistry.get(slot.gem().id());
        if (gemItem == null) {
            // Ours: the item data has no such gem to give back.
            say("&cThat Gemstone can't be taken out!");
            return;
        }
        ItemStack gem = ItemBuilder.build(gemItem);
        NBTTagCompound tag = session.tag();
        GemSlots.remove(tag, item.item(), index);
        // UNKNOWN: Hypixel's words, if it says any; ours.
        if (session.upgrade(cost, tag, null, gem)) say("&aYou removed the " + GemstoneGrinder.gemName(slot.gem()) + " &afrom your " + item.name() + "&a!");
        back();
    }

    private void back() {
        session.open(new GemstoneGrinder(session, fromHex));
    }
}
