package net.icxd.dungeons.mining;

import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.OnlyOn;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.collection.CollectionGains;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.dwarven.Powder;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Tuple;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mining: the mithril in the Dwarven Mines. Each block broken gives its drops (with their fortune), its Mining XP
 * (with Mining Wisdom) and its powder, and counts for the tool's Compact and the player's Flowstate streak.
 */
@OnlyOn({ServerType.DWARVEN_MINES})
public class BlockListener implements Listener {
    /** "The Softcap limits the breaking of a block to at least 4 ticks" (the wiki's Mining Speed). */
    static final int SOFTCAP_TICKS = 4;
    /** The crack stages a break is shown in, vanilla's ten. */
    static final int STAGES = 10;

    /** Flowstate's Mining Speed counts in their stats from now on (on this server, where the streaks are). */
    public BlockListener() {
        PlayerStats.addModifier(Flowstate::addSpeed);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Flowstate.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        Block block = player.getTargetBlock((Set<Material>) null, 5);
        if (block == null) return;
        MinableBlock minableBlock = BlockRegistry.getMinableBlock(block.getType());
        if (minableBlock == null) return;

        NBTTagCompound tag = ItemNBT.read(player.getInventory().getItemInMainHand());
        if (tag == null) return;
        String id = tag.getString("id");
        SkyBlockItem skyBlockItem = ItemRegistry.get(id);
        if (skyBlockItem == null) return;
        // The player's own stats (tool, armor and all), not just the tool's: its Breaking Power with its reforge's
        // (a Scraped tool's +1, as its lore counts it), and their Mining Speed and fortune.
        Stats stats = PlayerSession.of(player).stats();
        int breakingPower = (int) stats.get(Stat.BREAKING_POWER);
        if (breakingPower < minableBlock.minBreakingPower()) {
            player.sendMessage("You need a pickaxe with at least " + minableBlock.minBreakingPower() + " breaking power to break this block.");
            return;
        }
        double miningSpeed = stats.get(Stat.MINING_SPEED) * MiningTools.miningRate(player.isUnderWater(), aquaAffinity(player));
        if (miningSpeed <= 0) return;
        double fortune = fortune(stats, minableBlock);
        Location location = block.getLocation();

        if (breaksInstantly(minableBlock, miningSpeed)) {
            MiningManager.removeBlockBreakProgress(location);
            breakBlock(player, block, minableBlock, fortune);
            return;
        }

        // SkyBlock's break time, shown in ten crack stages: a stage per swing when it's due, and every
        // stage due by then at once.
        int ticks = breakTicks(minableBlock.blockStrength(), miningSpeed);
        int stage = reached(ticks, MiningManager.getBlockBreakProgress(location));
        if (!MiningManager.updatePhaseCooldown(player, wait(ticks, stage))) return;
        if (stage < STAGES) {
            MiningManager.setBlockBreakProgress(location, stage + 1);
            MiningManager.sendBlockDamage(player, location, stage);
            return;
        }
        MiningManager.removeBlockBreakProgress(location);
        breakBlock(player, block, minableBlock, fortune);
    }

    /**
     * Broken, instantly or not: its drops (with their fortune) to the player, its Mining XP (with their Mining Wisdom:
     * SkillGains) and powder, what it counts for (Compact on the tool, their Flowstate streak), and back after its
     * regen time.
     */
    private static void breakBlock(Player player, Block block, MinableBlock minableBlock, double fortune) {
        block.setType(minableBlock.blockWhenBroken());

        if (minableBlock.drops() != null) {
            for (Tuple<SkyBlockItem, Integer> drop : minableBlock.drops()) {
                // Null without the items' data.
                if (drop.first() == null) continue;
                ItemStack stack = ItemBuilder.build(drop.first());
                stack.setAmount(withFortune(drop.second(), fortune));
                give(player, stack);
            }
        }
        SkillGains.give(player, Skill.MINING, minableBlock.miningXp());
        if (minableBlock.powderType() != null) Powder.mined(player, minableBlock.powderType(), minableBlock.powder());
        Compact.broke(player, minableBlock);
        Flowstate.broke(player);

        minableBlock.onBreak(block, player);

        new BukkitRunnable() {
            @Override
            public void run() {
                MiningManager.sendBlockDamage(block, -1);
                minableBlock.place(block.getLocation());
            }
        }.runTaskLater(Dungeons.getInstance(), minableBlock.regenTime());
    }

    /** A mined block's item: collected as it's mined, into their inventory, and what doesn't fit lands at their feet (collected already). */
    static void give(Player player, ItemStack stack) {
        CollectionGains.collect(player, stack);
        for (ItemStack left : player.getInventory().addItem(stack).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
    }

    /** Whether the helmet they wear has Aqua Affinity. */
    private static boolean aquaAffinity(Player player) {
        return Combat.enchantments(Combat.skyBlockData(player.getInventory().getHelmet())).containsKey("aqua_affinity");
    }

    /**
     * Their Mining Fortune, and on a Dwarven Metal their Dwarven Metal Fortune too: "These stats are added
     * to regular Mining Fortune when mining their respective block types" (the wiki's Mining Fortune).
     */
    static double fortune(Stats stats, MinableBlock block) {
        return stats.get(Stat.MINING_FORTUNE) + (block.fortune() == null ? 0 : stats.get(block.fortune()));
    }

    /**
     * Whether it breaks at the first swing: at more than its threshold, which is ">30x" its Block Strength,
     * or ">60x" for ores and Dwarven Metals (the wiki's Mining Speed, "Instant Mining").
     */
    static boolean breaksInstantly(MinableBlock block, double miningSpeed) {
        return block.instaBreakStrength() != -1 && miningSpeed > block.instaBreakStrength();
    }

    /**
     * How long it takes to break, in ticks: "Block Strength × 30 / Mining Speed", rounded, and "at least 4
     * ticks" (the softcap; the wiki's Mining Speed).
     */
    static int breakTicks(int strength, double miningSpeed) {
        return (int) Math.max(SOFTCAP_TICKS, Math.round(strength * 30.0 / miningSpeed));
    }

    /**
     * The tick, counted from the first swing, that crack stage {@code stage} is due (at {@link #STAGES} it
     * breaks): the break's ticks spread over the stages, their fractions carried over, so the stages add up
     * to the break's time.
     */
    static int stageTick(int ticks, int stage) {
        return (int) Math.round((double) ticks * stage / STAGES);
    }

    /** The stage a swing reaches when {@code next} is due: that one, and the ones due on the same tick. */
    static int reached(int ticks, int next) {
        int stage = next;
        while (stage < STAGES && stageTick(ticks, stage + 1) == stageTick(ticks, stage)) stage++;
        return stage;
    }

    /** How many ticks after reaching {@code stage} the next is due (none once it's broken). */
    static int wait(int ticks, int stage) {
        return stage >= STAGES ? 0 : stageTick(ticks, stage + 1) - stageTick(ticks, stage);
    }

    /** Every 100 mining fortune is another drop; the rest is the chance of one more. */
    static int withFortune(int amount, double fortune) {
        return withFortune(amount, fortune, ThreadLocalRandom.current().nextDouble());
    }

    /** The same, with {@code roll} (0 to 1) as the random: one more when it's under the rest. */
    static int withFortune(int amount, double fortune, double roll) {
        double exact = amount * (1 + fortune / 100);
        int whole = (int) exact;
        return whole + (roll < exact - whole ? 1 : 0);
    }
}
