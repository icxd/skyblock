package net.icxd.dungeons.session;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A player's Vitality: since the Healing Revamp (0.26.1) a pool that healing abilities spend, as other
 * abilities spend mana. "Your Vitality stat defines the maximum amount of Vitality (the resource) you
 * have. Activating an ability that costs Vitality depletes the resource", and it "no longer boosts healing
 * as a multiplier" (the wiki's June 10 changelog; the wiki's Vitality). It's kept on their session with
 * its fractions (the stats menu: "You will regenerate 5.2 Vitality per second" at 104). Main thread.
 */
public final class Vitality {
    /** "Vitality regenerates at a rate of 5% per second, based on your Vitality stat." */
    static final double REGEN_SHARE = 0.05;

    private Vitality() {
    }

    /** The pool's size: the Vitality stat. */
    public static double max(Player player) {
        return Math.max(0, PlayerSession.of(player).stats().get(Stat.VITALITY));
    }

    /** What's in it now: full until they've spent some, never more than the pool (which shrinks when gear comes off). */
    public static double get(Player player) {
        double vitality = PlayerSession.of(player).getVitality();
        double max = max(player);
        return vitality < 0 ? max : Math.min(vitality, max);
    }

    /** Whether they have this much to spend. */
    public static boolean has(Player player, double cost) {
        return cost <= 0 || get(player) >= cost;
    }

    /** Takes this much if they have it; returns whether they did (nothing is taken if not). */
    public static boolean spend(Player player, double cost) {
        if (cost <= 0) return true;
        double vitality = get(player);
        if (vitality < cost) return false;
        PlayerSession.of(player).setVitality(vitality - cost);
        return true;
    }

    /** A second's regeneration: 5% of the pool, never past it. */
    public static void regenerate(Player player) {
        double max = max(player);
        PlayerSession.of(player).setVitality(Math.min(max, get(player) + regenPerSecond(max)));
    }

    /** 5% of the pool a second: +5 at 100, +6 at 120, +10 at 200. */
    public static double regenPerSecond(double max) {
        return Math.max(0, max) * REGEN_SHARE;
    }

    /**
     * Whether the action bar shows Vitality in mana's place: "Vitality replaces Mana in your action bar
     * when holding an item that consumes Vitality" (the June 10 changelog), one with an ability that costs it.
     */
    public static boolean shown(Player player) {
        return consumesVitality(player.getInventory().getItemInMainHand());
    }

    private static boolean consumesVitality(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null) return false;
        for (ItemBlock block : ItemBehaviours.of(item).blocks(item, tag, item.blocks())) {
            if (block.vitality() > 0) return true;
        }
        return false;
    }
}
