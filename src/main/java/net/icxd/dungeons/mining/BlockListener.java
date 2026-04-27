package net.icxd.dungeons.mining;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.utils.Tuple;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockBreakAnimation;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;

public class BlockListener implements Listener {

  @EventHandler
  public void onBreak(BlockBreakEvent event) {
    event.setCancelled(true);
  }

  @EventHandler
  public void onPlayerAnimation(PlayerAnimationEvent event) {
    Player player = event.getPlayer();
    if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
    if (player.getGameMode() != GameMode.SURVIVAL) return;

    Block block = player.getTargetBlock((HashSet<Byte>) null, 5);
    if (block == null) return;
    MaterialData data = block.getState().getData();
    MinableBlock minableBlock = BlockRegistry.getMinableBlock(new ItemStack(data.getItemType(),1,data.getData()));
    if (minableBlock == null) return;

    ItemStack itemInHand = player.getInventory().getItemInHand();
    if (itemInHand == null) return;
    net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemInHand);
    if (nmsItem == null) return;
    NBTTagCompound tag = nmsItem.getTag();
    if (tag == null) return;
    String id = tag.getString("id");
    SkyBlockItem skyBlockItem = ItemRegistry.get(id);
    if (skyBlockItem == null) return;
    int breakingPower = (int) skyBlockItem.stats().getBreakingPower();
    if (breakingPower < minableBlock.minBreakingPower()) {
      player.sendMessage("You need a pickaxe with at least " + minableBlock.minBreakingPower() + " breaking power to break this block.");
      return;
    }
    int miningSpeed = (int) skyBlockItem.stats().getMiningSpeed();

    int miningFortune = (int) skyBlockItem.stats().getMiningFortune();
    double amountMultiplier = 1 + (miningFortune * 0.01);

    if (minableBlock.instaBreakStrength() != -1 && miningSpeed >= minableBlock.instaBreakStrength()) {
      block.setType(minableBlock.blockWhenBroken());
      minableBlock.onBreak(block, player);
      return;
    }

    int timeToBreakInTicks = (minableBlock.blockStrength() * 30) / miningSpeed;

    MiningManager.updateAndNextPhase(player, timeToBreakInTicks);
    int breakProgress = MiningManager.getBlockBreakProgress(block.getLocation());
    MiningManager.sendBlockDamage(player, block.getLocation());
    breakProgress = ((breakProgress) + 1) % 10;
    MiningManager.setBlockBreakProgress(block.getLocation(), breakProgress);

    if (breakProgress == 0) {
      MiningManager.removeBlockBreakProgress(block.getLocation());

      block.setType(minableBlock.blockWhenBroken());

      if (minableBlock.drops() != null) {
        for (Tuple<SkyBlockItem, Integer> drop : minableBlock.drops()) {
          int amount = drop.second();
          if (amountMultiplier > 1) amount *= (int)amountMultiplier;
          ItemStack stack = ItemBuilder.build(drop.first());
          stack.setAmount(amount);
          if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
          } else {
            player.getInventory().addItem(stack);
          }
        }
      }

      minableBlock.onBreak(block, player);

      new BukkitRunnable() {
        @Override
        public void run() {
          Packet<?> packet = new PacketPlayOutBlockBreakAnimation(0, new BlockPosition(block.getX(), block.getY(), block.getZ()), -1);
          for (Player p : Bukkit.getOnlinePlayers())
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
          minableBlock.place(block.getLocation());
        }
      }.runTaskLater(Dungeons.getInstance(), minableBlock.regenTime());
    }
  }
}
