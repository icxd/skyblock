package net.icxd.dungeons.item.ability.weapons;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.LastHit;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * The Ragnarock Axe's Ragnarock: "Begin a channel. After not taking damage for 3s, gain 1.5x this weapon's
 * Strength for 10s." The weapon's Strength is what it gives them as they cast (its reforge and books too),
 * and they gain 1.5 times that (UNKNOWN whether "1.5x" is that or half as much again); a SkyBlock hit on them
 * during the channel breaks it, and nothing is gained ("Falling into the void does not count as taking
 * damage", the wiki: vanilla damage doesn't). Its look, sounds and any message are UNKNOWN (flames while it
 * channels, a blaze's roar when it's done, no message).
 */
final class Ragnarock implements AbilityHandler {
    static final int CHANNEL_TICKS = 60;
    static final long BUFF_MILLIS = 10_000;
    static final double STRENGTH_TIMES = 1.5;

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        double strength = STRENGTH_TIMES * ItemStats.of(player.getInventory().getItemInMainHand(), player).get(Stat.STRENGTH);
        long start = System.currentTimeMillis();
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || LastHit.within(player, System.currentTimeMillis() - start) != null) {
                    cancel();
                    return;
                }
                ticks += 5;
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 6, 0.4, 0.6, 0.4, 0.01);
                if (ticks < CHANNEL_TICKS) return;
                cancel();
                PlayerSession.of(player).buff("Ragnarock", new Stats().set(Stat.STRENGTH, strength), BUFF_MILLIS);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 0.6f);
            }
        }.runTaskTimer(Dungeons.getInstance(), 5, 5);
    }
}
