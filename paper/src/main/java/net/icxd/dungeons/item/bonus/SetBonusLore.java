package net.icxd.dungeons.item.bonus;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.user.StoredInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Set bonuses in lore count what the item's holder wears, as Hypixel's do: "Full Set Bonus: Shadow
 * Assassin (4/4)" on each piece of the worn set in the SkyBlock Menu tour's Stats & Equipment (02:18.2),
 * and a stored Necron's Helmet's "Witherborn &7(0/4)" in the Loadouts recording's Armor Sets while
 * Shadow Assassin Armor was worn (01:29.4). The count is of the holder's worn pieces of the set,
 * wherever the item is; items' data (read with none worn) has "(0/4)", which is what shows with no
 * holder. Lore is built with its item (see {@link ItemBuilder#build(SkyBlockItem, NBTTagCompound, Player)}),
 * so when what they wear changes, their items with a set bonus are built again ({@link #refresh}).
 */
public final class SetBonusLore {
    /** A header's count at its end ("(0/4)"), grey before it while the set isn't complete. */
    private static final Pattern COUNT = Pattern.compile("^(.*) (?:&7)?\\((\\d+)/(\\d+)\\)$");
    /** A colour code at the start. */
    private static final Pattern COLOUR = Pattern.compile("^&[0-9a-fk-or]");

    private SetBonusLore() {
    }

    /**
     * The block as it shows in {@code holder}'s inventory: a set bonus's header with how many of the set
     * they wear, and its text with the numbers for that many (a tiered one's). As it is for no holder,
     * and for a block that isn't a set's.
     */
    public static ItemBlock shown(ItemBlock block, Player holder) {
        SetKey set = SetKey.of(block);
        if (holder == null || set == null) return block;
        Bonus bonus = SetBonuses.bonus(block.kind(), block.name());
        int count = SetBonuses.worn(holder).count(set);
        return shown(block, count, count >= SetBonuses.needs(bonus, set), bonus);
    }

    /** The block for someone wearing {@code count} of its set, which counts ({@code active}) or not. */
    static ItemBlock shown(ItemBlock block, int count, boolean active, Bonus bonus) {
        String header = header(block, count, active);
        List<String> text = bonus == null ? block.text() : bonus.text(block.text(), count);
        if (Objects.equals(header, block.header()) && text.equals(block.text())) return block;
        return new ItemBlock(block.kind(), block.name(), header, block.activation(), text, block.mana(), block.manaPercent(),
                block.cooldown(), block.soulflow(), block.healthCost(), block.vitality(), block.pieces());
    }

    /**
     * A set bonus's header for someone wearing {@code count} of its set. A full set's stays gold, its count
     * grey until the set is complete ("&6Full Set Bonus: Shadow Assassin &7(3/4)", then "… (4/4)", as
     * recorded); a tiered one's is dark gray until it counts, then gold, the count going past the pieces
     * it takes ("Tiered Bonus: Peace Treaty (3/2)" in gold, the wiki's Armor: "If the conditions are
     * fulfilled for the Bonus to be active, then the Bonus name in the item lore will be golden"). A
     * header without a count ("Full Set Bonus: Bat Powers Activate!") is as it is.
     */
    static String header(ItemBlock block, int count, boolean active) {
        if (block.header() == null) return null;
        Matcher m = COUNT.matcher(block.header());
        if (!m.matches()) return block.header();
        String name = m.group(1);
        String of = "(" + count + "/" + m.group(3) + ")";
        // UNKNOWN: an inactive tiered header with some pieces worn is the data's dark gray (the wiki's example shows grey).
        if (SetKey.TIERED.equals(block.kind())) return (active ? "&6" : "&8") + COLOUR.matcher(name).replaceFirst("") + " " + of;
        return name + (active ? " " : " &7") + of;
    }

    /**
     * Their items with a set bonus built again for what they wear now, where that changes their lore
     * (the rest are left alone), the one on their cursor too (a piece just taken off). One kept out of
     * their stored inventory stays so.
     */
    static void refresh(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack rebuilt = rebuilt(contents[i], player);
            if (rebuilt != null) inventory.setItem(i, rebuilt);
        }
        ItemStack cursor = rebuilt(player.getItemOnCursor(), player);
        if (cursor != null) player.setItemOnCursor(cursor);
    }

    /** The item built again for what they wear now; null if it has no set bonus, or its lore stays the same. */
    private static ItemStack rebuilt(ItemStack stack, Player player) {
        if (!hasSetBonus(stack)) return null;
        ItemStack rebuilt = ItemBuilder.refresh(stack, player);
        if (lines(stack).equals(lines(rebuilt))) return null;
        if (StoredInventory.isNotSaved(stack)) StoredInventory.markNotSaved(rebuilt);
        return rebuilt;
    }

    private static List<?> lines(ItemStack stack) {
        ItemLore lore = stack.getData(DataComponentTypes.LORE);
        return lore == null ? List.of() : lore.lines();
    }

    private static boolean hasSetBonus(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null) return false;
        for (ItemBlock block : ItemBehaviours.of(item).blocks(item, tag, item.blocks())) if (SetKey.of(block) != null) return true;
        return false;
    }
}
