package net.icxd.dungeons.item.ability.weapons;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Shots;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * The Machine Gun Shortbow's Rapid Fire: "Shoots 5 Arrows per second for 8 seconds! Arrows deal 70% of what
 * they would normally deal." Each arrow goes where they look then, and hits as the bow's own shot would (see
 * {@link Shots}), times 0.7; like every player's arrow it can't be picked up. Its data has it on a right click
 * (the wiki says left; the data wins), with the bow's shot on the other. It stops once the bow isn't in their
 * hand (UNKNOWN whether Hypixel's goes on): each arrow is worked out with what they hold as it leaves, and
 * another item's Damage and Strength aren't the bow's.
 */
final class MachineGun implements AbilityHandler {
    static final int PER_SECOND = 5;
    static final int SECONDS = 8;
    static final double SHARE = 0.7;
    /** As fast as a shortbow's arrow leaves it (see InstantlyShoots). */
    private static final float SPEED = 5;

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        new BukkitRunnable() {
            private int shot;

            @Override
            public void run() {
                if (shot++ >= PER_SECOND * SECONDS || !Hits.canStillHit(player) || !holding(player, item)) {
                    cancel();
                    return;
                }
                Location eye = player.getEyeLocation();
                Arrow arrow = player.getWorld().spawnArrow(eye.clone().add(eye.getDirection().multiply(0.7)), eye.getDirection(), SPEED, 1);
                // Before Shots.record, which makes it an arrow nobody can pick up.
                arrow.setShooter(player);
                Shots.record(arrow, player, tag, true, SHARE);
                player.getWorld().playSound(eye, Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.4f);
            }
        }.runTaskTimer(Dungeons.getInstance(), 0, 20 / PER_SECOND);
    }

    /** Whether what's in their hand is this item. */
    private static boolean holding(Player player, SkyBlockItem item) {
        NBTTagCompound held = ItemNBT.read(player.getInventory().getItemInMainHand());
        return held != null && item.id().equals(held.getString("id"));
    }
}
