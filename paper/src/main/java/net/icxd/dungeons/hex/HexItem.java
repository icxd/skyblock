package net.icxd.dungeons.hex;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * The item in the Hex as the categories see it: what kind of SkyBlock item it is, its data, and who holds it
 * (for the lore's set bonuses; null in tests). {@code tag} is a copy of the data, read when this was made: a
 * category may change it and hand it to {@link HexSession#replace}, which is what changes the item.
 */
public record HexItem(SkyBlockItem item, NBTTagCompound tag, Player holder) {
    /** The SkyBlock item this stack is; null for nothing, or a stack that isn't one (a vanilla item). */
    public static HexItem of(ItemStack stack, Player holder) {
        if (stack == null || stack.isEmpty()) return null;
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        return item == null ? null : new HexItem(item, tag, holder);
    }

    /** Its name as it shows, "&6Fabled Hyperion &6✪✪✪✪✪" (see ItemBuilder#name). */
    public String name() {
        return ItemBuilder.name(item, tag);
    }

    /** Its rarity now (one up when recombobulated). */
    public Rarity rarity() {
        return ItemBuilder.rarity(item, tag);
    }

    /** "Give your time to The Hex!" is for accessories (the wiki's The Hex, trivia); the rest give their mind. */
    public boolean accessory() {
        return item.specificItemType() == SpecificItemType.ACCESSORY;
    }
}
