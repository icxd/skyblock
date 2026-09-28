package net.icxd.dungeons.economy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.PlayerInventory;

import net.icxd.dungeons.item.enchanting.EnchantmentType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.storage.AccessoryBag;
import net.icxd.dungeons.user.User;

/**
 * Coins for a kill, straight into the killer's purse, on Normal and Sandbox profiles alike (research
 * coins.md 1.3): the mob's own (its kind's where it spawned: 1 for the Entrance's, 20 for the Hub's
 * Magma Cube, 1,000 for the Bladesoul), and per level of the mob, the Scavenger enchantment on what
 * they hold (0.3 a tier: I-VI give 0.3-1.8, the wiki's Scavenger) and their best Scavenger accessory
 * in their inventory or Accessory Bag (Talisman 0.5, Ring 0.6, Artifact 0.75, as the items' lore
 * says; see storage/AccessoryBag#countedIds). They add up: "2.3 per level with
 * Scavenger VI and the Accessory" (the wiki's Scavenger Talisman).
 *
 * <p>Only the killer gets them (whether a party shares them isn't known), and nothing's said: the
 * sidebar's "(+N)" is all Hypixel shows. In a dungeon the sidebar hides the purse, but it still
 * fills. Not here yet: the bestiary's +2% a tier, Kill Combo (a pet's perk) and the other modifiers.
 */
public final class KillCoins implements Listener {
    private static final BigDecimal SCAVENGER_PER_TIER = new BigDecimal("0.3");
    /** Coins per level of the killed mob; only the best one counts, as with any accessory family. */
    static final Map<String, Double> ACCESSORIES = Map.of("SCAVENGER_TALISMAN", 0.5, "SCAVENGER_RING", 0.6, "SCAVENGER_ARTIFACT", 0.75);

    @EventHandler
    public void onDeath(SkyBlockMobDeathEvent event) {
        Player killer = event.killer();
        User user = killer == null ? null : User.ifLoaded(killer.getUniqueId());
        if (user == null || user.isReleased()) return;
        PlayerInventory inventory = killer.getInventory();
        double coins = coins(event.variant().coins(), event.variant().level(), scavenger(ItemNBT.read(inventory.getItemInMainHand())),
                accessory(AccessoryBag.countedIds(killer)));
        if (coins > 0) Purse.add(user, coins);
    }

    /** What a kill of a mob at this level is worth, with this Scavenger tier (0 for none) and accessory's coins per level. */
    static double coins(double base, int level, int scavenger, double accessoryPerLevel) {
        BigDecimal perLevel = SCAVENGER_PER_TIER.multiply(BigDecimal.valueOf(scavenger)).add(BigDecimal.valueOf(accessoryPerLevel));
        return BigDecimal.valueOf(base).add(perLevel.multiply(BigDecimal.valueOf(level))).doubleValue();
    }

    /** The item's Scavenger tier; 0 without it (or for no SkyBlock item). */
    static int scavenger(NBTTagCompound tag) {
        if (tag == null) return 0;
        NBTTagList enchantments = tag.getList("enchantments", 10);
        for (int i = 0; i < enchantments.size(); i++) {
            if (EnchantmentType.SCAVENGER.getNamespace().equalsIgnoreCase(enchantments.get(i).getString("name"))) {
                return enchantments.get(i).getInt("lvl");
            }
        }
        return 0;
    }

    /** The best Scavenger accessory's coins per level among these item ids; 0 for none. */
    static double accessory(List<String> ids) {
        double best = 0;
        for (String id : ids) best = Math.max(best, ACCESSORIES.getOrDefault(id, 0.0));
        return best;
    }

}
