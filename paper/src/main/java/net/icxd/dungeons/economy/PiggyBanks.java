package net.icxd.dungeons.economy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.storage.AccessoryBag;

/**
 * The Piggy Banks, a {@link DeathCoins} saver: "Saves your coins from death. Only when in inventory or Accessory
 * Bag. Fragile!" and the Cracked one "Saves 75% of your coins on death. Very fragile!", both "Triggers when losing
 * 20k+ coins" (their own text). "Results in the Piggy Bank becoming a Cracked Piggy Bank ... Upon a second death,
 * the Cracked Piggy Bank will become a Broken Piggy Bank and will no longer save coins" (the wiki's Piggy Bank). The
 * lines are Hypixel's, from SkyHanni's chat patterns: "You died and your piggy bank cracked!" and "You died, lost
 * 50,000 coins and your piggy bank broke!". Only the one that counts (the Accessory Bag's rule: one of a line) works.
 */
final class PiggyBanks implements DeathCoins.Saver {
    static final double TRIGGER = 20_000;
    static final String PIGGY = "PIGGY_BANK";
    static final String CRACKED = "CRACKED_PIGGY_BANK";
    static final String BROKEN = "BROKEN_PIGGY_BANK";

    /** Which bank each dying player's saving is from, between the two calls of one death. */
    private final Map<UUID, String> saving = new HashMap<>();

    @Override
    public double saved(Player player, double losing) {
        saving.remove(player.getUniqueId());
        String bank = bank(AccessoryBag.countedIds(player));
        double share = share(bank, losing);
        if (share > 0) saving.put(player.getUniqueId(), bank);
        return share;
    }

    @Override
    public String saving(PlayerDeathEvent event, double lost) {
        Player player = event.getPlayer();
        String bank = saving.remove(player.getUniqueId());
        if (bank == null) return null;
        String next = PIGGY.equals(bank) ? CRACKED : BROKEN;
        breakOne(event, bank, next);
        return PIGGY.equals(bank) ? "&cYou died and your piggy bank cracked!"
                : "&cYou died, lost " + DeathCoins.wholeCoins(lost) + " coins and your piggy bank broke!";
    }

    /** The bank that saves them, of those that count: a whole one before a cracked one; null for none. */
    static String bank(List<String> counted) {
        if (counted.contains(PIGGY)) return PIGGY;
        if (counted.contains(CRACKED)) return CRACKED;
        return null;
    }

    /** The share of a loss this bank saves: all or 75%, when the loss is 20k or more. */
    static double share(String bank, double losing) {
        if (bank == null || losing < TRIGGER) return 0;
        return PIGGY.equals(bank) ? 1 : 0.75;
    }

    /**
     * Turns the bank into the next one where it is: the Accessory Bag, else the inventory (in the drops when a death
     * drops it, as a hub's does). UNKNOWN: which one cracks when both hold one; the bag's here.
     */
    private static void breakOne(PlayerDeathEvent event, String bank, String next) {
        Player player = event.getPlayer();
        if (AccessoryBag.rewrite(player, bank, item -> changed(item, next, player))) return;
        List<ItemStack> drops = event.getDrops();
        for (int i = 0; i < drops.size(); i++) {
            if (!is(drops.get(i), bank)) continue;
            drops.set(i, changed(drops.get(i), next, player));
            return;
        }
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (!is(contents[i], bank)) continue;
            player.getInventory().setItem(i, changed(contents[i], next, player));
            return;
        }
    }

    private static boolean is(ItemStack stack, String id) {
        NBTTagCompound tag = stack == null || stack.isEmpty() ? null : ItemNBT.read(stack);
        return tag != null && id.equalsIgnoreCase(tag.getString("id"));
    }

    /** The same item as the next bank, keeping what's on it (a recombobulation, an enrichment). */
    private static ItemStack changed(ItemStack stack, String next, Player holder) {
        SkyBlockItem item = ItemRegistry.get(next);
        NBTTagCompound tag = ItemNBT.read(stack);
        if (item == null || tag == null) return stack;
        tag.setString("id", next);
        tag.setString("name", item.name());
        return ItemBuilder.build(item, tag, stack.getAmount(), holder);
    }
}
