package net.icxd.dungeons.item.ability.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * Worn pieces' sneak abilities that move them (ABILITIES_UTILITY.md, "Worn"): the Salmon Armor's Water Burst, the
 * Spring Boots' To the Moon! and the Slug Boots' Bouncy. How far each throws them is UNKNOWN (no source gives a
 * number): the values here are guesses of a size that does what the text says. Main thread.
 */
final class Movement {
    /** Water Burst's push forward. */
    static final double BURST = 1.6;
    /** To the Moon!: the jump's speed up at no charge, what a second of sneaking adds, and the most. */
    static final double MOON_BASE = 0.6;
    static final double MOON_PER_SECOND = 0.5;
    static final double MOON_MOST = 2.5;
    /** Bouncy: from how many blocks' fall it bounces, and what each block fallen gives back. */
    static final float BOUNCY_FROM = 5;
    static final double BOUNCY_PER_BLOCK = 0.1;
    static final double BOUNCY_MOST = 2;

    /** To the Moon!'s charges: since when they've been sneaking with it. */
    private static final Map<UUID, Long> CHARGING = new HashMap<>();

    private Movement() {
    }

    /**
     * The Salmon Armor's Water Burst: "When wearing the full set, grants the wearer the ability to burst forward when
     * sneaking in water." (its 20 mana the block's): only with its 4 pieces on, and only in water.
     */
    static final class WaterBurst implements AbilityHandler {
        static final String NAME = "Water Burst";

        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            return player.isInWater() && SetBonuses.worn(player).with(WornPassives.KIND, NAME).size() >= 4;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            player.setVelocity(player.getLocation().getDirection().multiply(BURST));
            player.getWorld().spawnParticle(Particle.BUBBLE, player.getLocation(), 30, 0.4, 0.4, 0.4, 0.1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_DOLPHIN_SWIM, 1, 1.2f);
        }
    }

    /**
     * The Spring Boots' To the Moon!: "Charge your jump by sneaking. The longer you sneak, the higher you will jump!":
     * starting to sneak starts the charge, and letting go on the ground jumps (see {@link #released}).
     */
    static final class ToTheMoon implements AbilityHandler {
        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            CHARGING.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    /** They let go of sneak: a charged To the Moon! jumps them, if they're on the ground and still wear the boots. */
    static void released(Player player) {
        Long since = CHARGING.remove(player.getUniqueId());
        if (since == null || !player.isOnGround() || SetBonuses.worn(player).with(WornPassives.KIND, "To the Moon!").isEmpty()) return;
        double up = moonJump((System.currentTimeMillis() - since) / 1000.0);
        player.setVelocity(player.getVelocity().setY(up));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 1, 1.4f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.3, 0.1, 0.3, 0.05);
    }

    /** How fast up a jump charged for this long goes. */
    static double moonJump(double seconds) {
        return Math.min(MOON_MOST, MOON_BASE + MOON_PER_SECOND * Math.max(0, seconds));
    }

    /**
     * The Slug Boots' Bouncy ("When falling from a great height, you'll bounce straight up!", a SNEAK ability): a fall
     * of 5 blocks or more that ends while they're sneaking with the boots on doesn't hurt, and bounces them up by how
     * far they fell. Whether it needs the sneak, and what a great height is, are UNKNOWN. True if it bounced.
     */
    static boolean fell(Player player, EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !player.isSneaking() || player.getFallDistance() < BOUNCY_FROM) return false;
        if (SetBonuses.worn(player).with(WornPassives.KIND, "Bouncy").isEmpty()) return false;
        event.setCancelled(true);
        player.setVelocity(new Vector(0, bounce(player.getFallDistance()), 0));
        player.setFallDistance(0);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SLIME_BLOCK_FALL, 1, 1);
        return true;
    }

    /** How fast up a fall of this many blocks bounces them. */
    static double bounce(double fallen) {
        return Math.min(BOUNCY_MOST, BOUNCY_PER_BLOCK * Math.max(0, fallen));
    }

    static void forget(UUID player) {
        CHARGING.remove(player);
    }
}
