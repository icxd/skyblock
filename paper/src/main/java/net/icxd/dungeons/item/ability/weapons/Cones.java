package net.icxd.dungeons.item.ability.weapons;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.bonus.SetBonuses;

/** Abilities that hit everything in a cone in front of them, through walls. */
final class Cones {
    private Cones() {
    }

    /** Particles scattered through the cone (how each one's looks is UNKNOWN past the wiki's "cone of ..."). */
    private static void fill(Player player, double length, double degrees, Particle particle) {
        Location eye = player.getEyeLocation();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 60; i++) {
            Location at = eye.clone();
            at.setYaw((float) (eye.getYaw() + random.nextDouble(-degrees / 2, degrees / 2)));
            at.setPitch((float) (eye.getPitch() + random.nextDouble(-degrees / 4, degrees / 4)));
            Vector point = at.getDirection().multiply(random.nextDouble(1, length));
            player.getWorld().spawnParticle(particle, eye.clone().add(point), 1, 0, 0, 0, 0);
        }
    }

    /**
     * The Ice Spray Wand's: "Produces a cone of ice in front of the caster that deals 17,000 damage to mobs
     * and freezes them in place for 5 seconds! Frozen mobs take 10% increased damage!"; "a cone-shaped
     * radius 7 blocks long and 60º wide: this cone can pass through walls" (the wiki); base 17,000 (19,000
     * fragged) with Intelligence scaling 0.1. Its chat line is UNKNOWN (Ice Spray's, in the others' form).
     */
    static final class IceSpray implements AbilityHandler {
        static final Magic.Spell SPRAY = new Magic.Spell(17_000, 0.1);
        static final double LENGTH = 7;
        static final double DEGREES = 60;
        static final int FROZEN_TICKS = 100;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            List<LivingEntity> caught = Hits.inCone(player, LENGTH, DEGREES);
            // Hit first, then frozen: the 10% more is for what comes after.
            Hits.report(player, "Ice Spray", Hits.spell(player, item, tag, Hits.spellOf(item, SPRAY), caught));
            for (LivingEntity mob : caught) if (Hits.hittable(mob)) Hits.freeze(mob, FROZEN_TICKS);
            fill(player, LENGTH, DEGREES, Particle.SNOWFLAKE);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_POWDER_SNOW_PLACE, 1, 0.8f);
        }
    }

    /**
     * The Aspect of the Dragons' Dragon Rage: "All Monsters in front of you take 12,000 damage. Hit monsters
     * take large knockback"; "a cone of fire particles 7.5 blocks long and 60° wide" (the wiki); base 12,000
     * with Intelligence scaling 0.1, "Not affected by the Additive Multiplier". How far the knockback throws
     * them is UNKNOWN; so is its chat line (Dragon Rage's, in the others' form).
     */
    static final class DragonRage implements AbilityHandler {
        static final Magic.Spell RAGE = new Magic.Spell(12_000, 0.1, false, false);
        static final double LENGTH = 7.5;
        static final double DEGREES = 60;
        private static final double KNOCKBACK = 2;
        static final double SUPERIOR_BLOOD = 1.5;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            List<LivingEntity> caught = Hits.inCone(player, LENGTH, DEGREES);
            Magic.Spell rage = Hits.spellOf(item, RAGE);
            // Superior Dragon Armor: "Aspect of the Dragons ability deals 50% more damage".
            if (SetBonuses.active(player, "Superior Blood")) rage = rage.withBase(rage.base() * SUPERIOR_BLOOD);
            Hits.report(player, "Dragon Rage", Hits.spell(player, item, tag, rage, caught));
            Vector away = player.getLocation().getDirection().setY(0);
            if (away.lengthSquared() > 0) away.normalize().multiply(KNOCKBACK);
            for (LivingEntity mob : caught) {
                if (mob.isValid() && !mob.isDead()) mob.setVelocity(away.clone().setY(0.6));
            }
            fill(player, LENGTH, DEGREES, Particle.FLAME);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
        }
    }
}
