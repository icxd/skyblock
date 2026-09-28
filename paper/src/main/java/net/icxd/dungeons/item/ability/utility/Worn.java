package net.icxd.dungeons.item.ability.utility;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/** A SkyBlock item a player has on (a helmet with Second Wind, boots with Spirit Glide) or holds, and its data. */
record Worn(SkyBlockItem item, NBTTagCompound tag) {
    /** The SkyBlock item this stack is; null for nothing or a vanilla item. */
    static Worn of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        return item == null ? null : new Worn(item, tag);
    }

    /** Its blocks, as its behaviour has them. */
    List<ItemBlock> blocks() {
        return ItemBehaviours.of(item).blocks(item, tag, item.blocks());
    }

    /** Its ABILITY block with this name (as its behaviour has its blocks); null if it has none. */
    ItemBlock ability(String name) {
        for (ItemBlock block : blocks()) {
            if (block.isAbility() && name.equals(block.name())) return block;
        }
        return null;
    }

    /** The ability with this name on {@code stack}; null if it's no SkyBlock item or hasn't one. */
    static ItemBlock ability(ItemStack stack, String name) {
        Worn worn = of(stack);
        return worn == null ? null : worn.ability(name);
    }
}
