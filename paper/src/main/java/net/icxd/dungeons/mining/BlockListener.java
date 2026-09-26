package net.icxd.dungeons.mining;

import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.OnlyOn;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Tuple;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

/** Mining: the mithril in the Dwarven Mines. */
@OnlyOn({ServerType.DWARVEN_MINES})
public class BlockListener implements Listener {

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
        int breakingPower = (int) skyBlockItem.stats().get(Stat.BREAKING_POWER);
        if (breakingPower < minableBlock.minBreakingPower()) {
            player.sendMessage("You need a pickaxe with at least " + minableBlock.minBreakingPower() + " breaking power to break this block.");
            return;
        }
        // The player's own mining speed and fortune (tool, armor and all), not just the tool's.
        Stats stats = PlayerSession.of(player).stats();
        int miningSpeed = (int) stats.get(Stat.MINING_SPEED);
        if (miningSpeed <= 0) return;
        double fortune = stats.get(Stat.MINING_FORTUNE);

        if (minableBlock.instaBreakStrength() != -1 && miningSpeed >= minableBlock.instaBreakStrength()) {
            block.setType(minableBlock.blockWhenBroken());
            minableBlock.onBreak(block, player);
            return;
        }

        // SkyBlock's break time, in ten crack stages: a stage per swing, but no faster than that.
        int timeToBreakInTicks = Math.max(1, (minableBlock.blockStrength() * 30) / miningSpeed);
        if (!MiningManager.updatePhaseCooldown(player, Math.max(1, timeToBreakInTicks / 10))) return;
        int breakProgress = MiningManager.getBlockBreakProgress(block.getLocation());
        MiningManager.sendBlockDamage(player, block.getLocation());
        breakProgress = ((breakProgress) + 1) % 10;
        MiningManager.setBlockBreakProgress(block.getLocation(), breakProgress);

        if (breakProgress == 0) {
            MiningManager.removeBlockBreakProgress(block.getLocation());

            block.setType(minableBlock.blockWhenBroken());

            if (minableBlock.drops() != null) {
                for (Tuple<SkyBlockItem, Integer> drop : minableBlock.drops()) {
                    ItemStack stack = ItemBuilder.build(drop.first());
                    stack.setAmount(withFortune(drop.second(), fortune));
                    for (ItemStack left : player.getInventory().addItem(stack).values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), left);
                    }
                }
            }

            minableBlock.onBreak(block, player);

            new BukkitRunnable() {
                @Override
                public void run() {
                    MiningManager.sendBlockDamage(block, -1);
                    minableBlock.place(block.getLocation());
                }
            }.runTaskLater(Dungeons.getInstance(), minableBlock.regenTime());
        }
    }

    /** Every 100 mining fortune is another drop; the rest is the chance of one more. */
    static int withFortune(int amount, double fortune) {
        double exact = amount * (1 + fortune / 100);
        int whole = (int) exact;
        return whole + (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < exact - whole ? 1 : 0);
    }
}
