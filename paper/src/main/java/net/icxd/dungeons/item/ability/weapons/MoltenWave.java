package net.icxd.dungeons.item.ability.weapons;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * The Midas Staff's Molten Wave: "Cast a wave of molten gold in the direction you are facing!"; "a wave of
 * gold blocks in front of the player, similar to the Tank class ability Seismic Wave. All enemies caught
 * within the wave take the full ability damage. The wave has a length of 14 blocks and stops upon impact
 * with walls and carpets"; "The wave cannot hit enemies too close to the player, about 1 block away" (the
 * wiki); base 6,000 (its data) with Intelligence scaling 0.3, and its chat line "Your Molten Wave hit ..."
 * (the mods') once the wave is done. Greed's bonus from the price paid at the Dark Auction isn't there
 * (no Dark Auction yet: LATER). The wave goes a block a tick, reaching 1.5 blocks to the sides (UNKNOWN,
 * as Seismic Wave's).
 */
final class MoltenWave implements AbilityHandler {
    static final Magic.Spell WAVE = new Magic.Spell(6_000, 0.3);
    static final int LENGTH = 14;
    /** "about 1 block away". */
    static final int FIRST = 2;
    private static final double REACH = 1.5;
    private static final int GOLD_TICKS = 10;

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Magic.Spell spell = Hits.spellOf(item, WAVE);
        Vector step = player.getLocation().getDirection().setY(0);
        if (step.lengthSquared() < 1e-6) step = new Vector(1, 0, 0);
        step.normalize();
        Location start = player.getLocation();
        Vector along = step;
        Set<UUID> struck = new HashSet<>();
        Tally tally = new Tally();
        new BukkitRunnable() {
            private int i = FIRST;

            @Override
            public void run() {
                Location at = start.clone().add(along.clone().multiply(i));
                if (i > LENGTH || !Hits.canStillHit(player) || stops(at.getBlock())) {
                    cancel();
                    Hits.report(player, block.name(), tally);
                    return;
                }
                gold(at);
                for (LivingEntity mob : Hits.near(at.clone().add(0, 0.5, 0), REACH)) {
                    if (!struck.add(mob.getUniqueId())) continue;
                    double damage = Hits.magic(player, item, tag, spell, mob) * Hits.takenFactor(mob);
                    if (Hits.hurt(player, mob, damage, DamageIndicators.Look.NORMAL)) tally.add(damage);
                }
                i++;
            }
        }.runTaskTimer(Dungeons.getInstance(), 0, 1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 1, 0.6f);
    }

    /** "stops upon impact with walls and carpets". */
    private static boolean stops(Block feet) {
        Material type = feet.getType();
        return type.isSolid() || type == Material.MOSS_CARPET || Tag.WOOL_CARPETS.isTagged(type);
    }

    /** A gold block rising out of the ground there for a moment. */
    private static void gold(Location at) {
        Location spot = at.getBlock().getLocation();
        BlockDisplay gold = spot.getWorld().spawn(spot, BlockDisplay.class, display -> {
            display.setPersistent(false);
            display.setBlock(Material.GOLD_BLOCK.createBlockData());
        });
        spot.getWorld().playSound(spot, Sound.BLOCK_METAL_PLACE, 0.4f, 1.2f);
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), gold::remove, GOLD_TICKS);
    }
}
