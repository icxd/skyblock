package net.icxd.dungeons.mining;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.utils.Tuple;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public interface MinableBlock {
    Material material();
    int minBreakingPower();
    int blockStrength();

    default int regenTime() { return 20*5; }
    default int instaBreakStrength() { return -1; } // -1 = no insta break
    default Material blockWhenBroken() { return Material.BEDROCK; }
    default ArrayList<Tuple<SkyBlockItem, Integer>> drops() { return null; }

    default void onBreak(Block block, Player player) {}

    default void place(Location location) {
        location.getBlock().setType(material());
    }

    default void breakBlock(Block block, Player player) {
        ItemStack itemInHand = player.getInventory().getItemInHand();
        if (itemInHand == null) return;
        ItemNBT nmsItem = ItemNBT.of(itemInHand);
        if (nmsItem == null) return;
        NBTTagCompound tag = nmsItem.getTag();
        if (tag == null) return;
        String id = tag.getString("id");
        SkyBlockItem skyBlockItem = ItemRegistry.get(id);
        if (skyBlockItem == null) return;
        int breakingPower = (int) skyBlockItem.stats().getBreakingPower();
        if (breakingPower < minBreakingPower()) {
            player.sendMessage("You need a pickaxe with at least " + minBreakingPower() + " breaking power to break this block.");
            return;
        }
        int miningSpeed = (int) skyBlockItem.stats().getMiningSpeed();

        int miningFortune = (int) skyBlockItem.stats().getMiningFortune();
        double amountMultiplier = 1 + (miningFortune * 0.01);

        if (instaBreakStrength() != -1 && miningSpeed >= instaBreakStrength()) {
            block.setType(blockWhenBroken());
            onBreak(block, player);
            return;
        }

        int timeToBreakInTicks = (blockStrength() * 30) / miningSpeed;

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                MiningManager.sendBlockDamage(block, (int) (ticks / (float) timeToBreakInTicks * 10));
                ticks++;
                if (ticks >= timeToBreakInTicks) {
                    block.setType(blockWhenBroken());

                    if (drops() != null) {
                        for (Tuple<SkyBlockItem, Integer> drop : drops()) {
                            int amount = drop.second();
                            if (amountMultiplier > 1) amount *= amountMultiplier;
                            ItemStack stack = ItemBuilder.build(drop.first());
                            stack.setAmount(amount);
                            if (player.getInventory().firstEmpty() == -1) {
                                player.getWorld().dropItemNaturally(player.getLocation(), stack);
                            } else {
                                player.getInventory().addItem(stack);
                            }
                        }
                    }

                    onBreak(block, player);
                    cancel();
                }

                Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), new BukkitRunnable() {
                    @Override
                    public void run() {
                        MiningManager.sendBlockDamage(block, -1);
                        place(block.getLocation());
                    }
                }, regenTime());
            }
        }.runTaskTimer(Dungeons.getInstance(), 0, 1);
    }
}
