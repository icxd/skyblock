package net.icxd.dungeons.item.ability.utility;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.gui.SignInput;
import net.icxd.dungeons.utils.Utils;

/**
 * The Rancher's Boots' Farmer's Speed (LEFT CLICK, held): "Set a maximum amount of ✦ Speed." A sign asks for it (its
 * lines UNKNOWN), and the number typed is kept on the boots ({@link #CAP}): while they're worn, their Speed is at most
 * that (see WornPassives' Farmer's Speed). Nothing typed, or no number, leaves it as it was; 0 and less, or the Speed
 * cap and more, take the cap off (UNKNOWN what Hypixel takes). Its Farmer's Grace line ("Current Speed Cap: 400")
 * stays the data's (the boots' lines are the armor bonuses', for their Farming level stats). Main thread.
 */
final class FarmersSpeed implements AbilityHandler {
    /** The boots' own Speed cap, in their data (none until one is set). */
    static final String CAP = "ranchers_speed_cap";
    /** The Speed cap there is anyway (the plugin's 400), for "no cap". */
    static final int NONE = 400;
    private static final List<String> SIGN = List.of("", "^^^^^^^^^^^^^^^", "Set your max", "Speed");

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        int slot = player.getInventory().getHeldItemSlot();
        String uuid = tag.getString("uuid");
        SignInput.open(player, SIGN, lines -> {
            Integer typed = typed(lines.isEmpty() ? "" : lines.getFirst());
            if (typed == null || !player.isOnline() || InventorySyncListener.frozen(player)) return;
            ItemStack stack = player.getInventory().getItem(slot);
            NBTTagCompound now = ItemNBT.read(stack);
            SkyBlockItem boots = now == null ? null : ItemRegistry.get(now.getString("id"));
            if (boots == null || !boots.id().equals(item.id()) || !uuid.equals(now.getString("uuid"))) return;
            int cap = typed <= 0 || typed >= NONE ? NONE : typed;
            now.setInt(CAP, cap);
            player.getInventory().setItem(slot, ItemBuilder.build(boots, now, stack.getAmount(), player));
            player.sendMessage(Utils.color("&aSet your maximum Speed to &f" + cap + "✦&a!"));
        });
    }

    /** A number typed on the sign's first line; null for none. */
    static Integer typed(String line) {
        String digits = line.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "").replace(",", "").trim();
        if (digits.isEmpty() || !digits.matches("-?\\d{1,9}")) return null;
        return Integer.parseInt(digits);
    }

    /** The cap the boots have (their data's; {@link #NONE} for none). */
    static int cap(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(CAP)) return NONE;
        int cap = tag.getInt(CAP);
        return cap <= 0 ? NONE : cap;
    }
}
