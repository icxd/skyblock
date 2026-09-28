package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.dungeons.instance.RunBoosts;
import net.icxd.dungeons.dungeons.instance.Score;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.listeners.InventorySyncListener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.IntFunction;

/**
 * Hecatomb, the helmet's "stacking" enchantment: "Gain +1% Catacombs XP & +2% Class XP, doubled on S+ runs.
 * Grants +8❤ per 10 Catacombs levels" (the Health with the other stats, {@link StatEnchants}), and it tiers up
 * with S runs ("2 S runs to tier up!"), counted on the helmet under Hypixel's own key (live items carry
 * "hecatomb_s_runs"). Main thread.
 */
final class Hecatomb {
    static final String ID = StatEnchants.HECATOMB;
    static final String S_RUNS = "hecatomb_s_runs";

    private Hecatomb() {
    }

    static void register() {
        RunBoosts.addBoost((player, score) -> {
            WornEnchants.Piece helmet = WornEnchants.ofType(WornEnchants.of(player), SpecificItemType.HELMET);
            int level = helmet == null ? 0 : helmet.level(ID);
            return level > 0 ? boost(EnchantNumbers.of(ID, level), score.grade()) : null;
        });
        RunBoosts.addRewardedListener((player, floor, score, failed) -> {
            if (!failed && sRun(score.grade())) countRun(player);
        });
    }

    /** Its experience boost at a run with this grade: its text's two percents, doubled on an S+ run (a score of 300 or more). */
    static RunBoosts.Boost boost(double[] n, String grade) {
        if (n.length < 2) return null;
        double times = "S+".equals(grade) ? 2 : 1;
        return new RunBoosts.Boost(n[0] * times, n[1] * times);
    }

    /**
     * Whether a run counts for its tiers: a completed one with an S or S+ (UNKNOWN whether Hypixel's "S runs" count
     * S+ ones: here they do, S+ being a better S).
     */
    static boolean sRun(String grade) {
        return "S".equals(grade) || "S+".equals(grade);
    }

    /**
     * The level an item's Hecatomb comes to after this many S runs: each level's "N S runs to tier up!" is how many
     * it takes in all to reach the next (the wiki's table: 2 for II, 5 for III, 10 for IV, ...), up to the last
     * level, which has no such line. {@code tierUp} gives a level's numbers in that line (none for the last).
     */
    static int level(int level, double runs, IntFunction<double[]> tierUp) {
        for (double[] next = tierUp.apply(level); next.length > 0 && runs >= next[0]; next = tierUp.apply(level)) level++;
        return level;
    }

    /**
     * An S run for the Hecatomb helmet they wear: its count goes up, and its level with it; the helmet is made again
     * for them, so its lore follows. Nothing if their inventory can't change just now (it's being handed to
     * another server).
     */
    private static void countRun(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        NBTTagCompound tag = ItemNBT.read(helmet);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null || InventorySyncListener.frozen(player)) return;
        NBTTagList enchantments = tag.getList("enchantments", 10);
        for (int i = 0; i < enchantments.size(); i++) {
            NBTTagCompound enchantment = enchantments.get(i);
            if (!ID.equals(EnchantmentData.id(enchantment.getString("name")))) continue;
            int level = enchantment.getInt("lvl");
            double runs = ItemCounters.add(tag, S_RUNS, 1);
            int next = level(level, runs, at -> EnchantNumbers.tierUp(ID, at));
            if (next != level) {
                enchantment.setShort("lvl", (short) next);
                tag.set("enchantments", enchantments);
            }
            player.getInventory().setHelmet(ItemBuilder.build(item, tag, helmet.getAmount(), player));
            return;
        }
    }
}
