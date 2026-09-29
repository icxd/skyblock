package net.icxd.dungeons.mining;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.entity.Player;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enchanting.EnchantmentType;
import net.icxd.dungeons.item.enchanting.StackingEnchants;
import net.icxd.dungeons.utils.Utils;

/**
 * Compact, on mining tools: "Gain +1☯ Mining Wisdom and a 0.25% chance to drop an enchanted item" at I, a stacking
 * enchantment that tiers up by the blocks the tool mines ("&8100 blocks to tier up!"; the wiki's Compact). Its Mining
 * Wisdom is its text's stat (EnchantmentType); here, each block broken with it counts on the tool (see {@link
 * StackingEnchants}) and rolls its chance, per block ("Compact now uses random chance to proc, instead of guarantee on
 * blocks mined", the wiki's history) for one of the block's enchanted item, as well as its drops (UNKNOWN whether it
 * takes their place; fortune doesn't touch it here). Main thread.
 */
final class Compact {
    static final String ID = "compact";

    private Compact() {
    }

    /** A block broken: counted on what they hold if it has Compact, and its chance rolled. */
    static void broke(Player player, MinableBlock block) {
        int level = StackingEnchants.addHeld(player, ID, 1);
        if (level <= 0) return;
        EnchantmentType type = EnchantmentType.getByNamespace(ID);
        SkyBlockItem drop = block.compactDrop() == null ? null : ItemRegistry.get(block.compactDrop());
        if (type == null || drop == null || !procs(type.percent(level), ThreadLocalRandom.current().nextDouble())) return;
        BlockListener.give(player, ItemBuilder.build(drop));
        player.sendMessage(Utils.color(message(drop.rarity().getColor(), drop.name())));
    }

    /** Whether a {@code chance} percent roll comes up, with {@code roll} (0 to 1) as the random. */
    static boolean procs(double chance, double roll) {
        return roll < chance / 100;
    }

    /**
     * "COMPACT! You found an Enchanted Hard Stone!" (SkyHanni's powder tracker, without its colours: UNKNOWN, so gold
     * and bold as Hypixel's other finds, and the item in its rarity's).
     */
    static String message(String itemColor, String itemName) {
        return "&6&lCOMPACT! &fYou found an " + itemColor + itemName + "&f!";
    }
}
