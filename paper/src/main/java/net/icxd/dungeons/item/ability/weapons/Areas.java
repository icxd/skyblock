package net.icxd.dungeons.item.ability.weapons;

import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Spells that hurt everything in an area: at once, or a share of their damage at a time for a while. Base
 * damage is the item's own (its data), scaling its wiki page's (where its page and the wiki's table differ,
 * the page's); sizes, speeds and looks the wiki doesn't give are UNKNOWN, and none has a chat line (UNKNOWN).
 */
final class Areas {
    private static final Pattern SECONDS = Pattern.compile("for (\\d+)s");

    private Areas() {
    }

    /** A ring of particles around a spot. */
    static void ring(Location at, double radius, Particle particle) {
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            at.getWorld().spawnParticle(particle, at.clone().add(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius), 1, 0, 0, 0, 0);
        }
    }

    /**
     * The Staff of the Volcano's Explode: "Creates an explosion around you dealing 24,000 damage and setting all
     * mobs on fire in a 4 block radius over 1s", 1,000 health a cast (not from the last of it), scaling 1.
     */
    static final class Explode implements AbilityHandler {
        static final Magic.Spell BLAST = new Magic.Spell(24_000, 1);
        static final double RADIUS = 4;

        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            return Hits.canPayHealth(player, block.healthCost());
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Hits.payHealth(player, block.healthCost());
            Location at = player.getLocation();
            List<LivingEntity> caught = Hits.near(at, RADIUS);
            Hits.spell(player, item, tag, Hits.spellOf(item, BLAST), caught);
            for (LivingEntity mob : caught) if (mob.isValid()) mob.setFireTicks(20);
            at.getWorld().spawnParticle(Particle.EXPLOSION, at, 4, 1.5, 0.5, 1.5, 0);
            at.getWorld().spawnParticle(Particle.LAVA, at, 20, 2, 0.5, 2, 0);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.7f);
        }
    }

    /** The Golem Sword's Iron Punch: "damaging enemies in a hexagon around you for 250 base Magic Damage", scaling 0.1; the hexagon's size is UNKNOWN (4 blocks across the middle, as a ball). */
    static final class IronPunch implements AbilityHandler {
        static final Magic.Spell PUNCH = new Magic.Spell(250, 0.1);
        static final double RADIUS = 4;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Location at = player.getLocation();
            Hits.spell(player, item, tag, Hits.spellOf(item, PUNCH), Hits.near(at, RADIUS));
            at.getWorld().spawnParticle(Particle.BLOCK, at, 40, 2, 0.1, 2, at.clone().subtract(0, 1, 0).getBlock().getBlockData());
            at.getWorld().playSound(at, Sound.ENTITY_IRON_GOLEM_ATTACK, 1, 0.8f);
        }
    }

    /**
     * The Celeste Wand's Lightning Strike: "Strikes lightning up to 10 blocks away, dealing ... damage to the
     * nearest monster"; "on the location of the caster's crosshair up to 10 blocks away" (the wiki); base 250
     * (its data; its text says 40), scaling 0.3. The nearest monster within 3 blocks of the strike (UNKNOWN).
     */
    static final class LightningStrike implements AbilityHandler {
        static final Magic.Spell BOLT = new Magic.Spell(250, 0.3);

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Location at = Hits.aimed(player, 10);
            at.getWorld().strikeLightningEffect(at);
            List<LivingEntity> near = Hits.near(at, 3);
            if (!near.isEmpty()) Hits.spell(player, item, tag, Hits.spellOf(item, BOLT), near.subList(0, 1));
        }
    }

    /**
     * Leap (Leaping Sword, Silk-Edge Sword): "Leap into the air and deal 350 damage to nearby enemies upon
     * landing on the ground. Damaged enemies will also be frozen for 1 second"; "all enemies within a 4-block
     * radius" (the wiki); scaling 1. How high and far the leap goes is UNKNOWN (as a strong jump forward).
     * Landing is the first tick back on the ground (at most 5 seconds on).
     */
    static final class Leap implements AbilityHandler {
        static final Magic.Spell LANDING = new Magic.Spell(350, 1);
        static final double RADIUS = 4;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Magic.Spell spell = Hits.spellOf(item, LANDING);
            Vector forward = player.getLocation().getDirection().setY(0);
            if (forward.lengthSquared() > 0) forward.normalize().multiply(0.9);
            player.setVelocity(forward.setY(1.0));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0.8f);
            new BukkitRunnable() {
                private int ticks;

                @Override
                public void run() {
                    if (++ticks > 100 || !Hits.canStillHit(player)) {
                        cancel();
                        return;
                    }
                    // A few ticks in, so the jump has left the ground.
                    if (ticks < 4 || !player.isOnGround()) return;
                    cancel();
                    player.setFallDistance(0);
                    Location at = player.getLocation();
                    List<LivingEntity> caught = Hits.near(at, RADIUS);
                    Hits.spell(player, item, tag, spell, caught);
                    for (LivingEntity mob : caught) if (Hits.hittable(mob)) Hits.root(mob, 20);
                    at.getWorld().spawnParticle(Particle.BLOCK, at, 40, 2, 0.1, 2, at.clone().subtract(0, 1, 0).getBlock().getBlockData());
                    at.getWorld().playSound(at, Sound.ENTITY_GENERIC_BIG_FALL, 1, 0.8f);
                }
            }.runTaskTimer(Dungeons.getInstance(), 1, 1);
        }
    }

    /**
     * The Fire Veil Wand's Fire Veil: "Creates a veil of fire around you for 5s, dealing 15,000 damage per
     * second to mobs within", "in a 5 block radius" (the wiki, whose text has it "every 0.5 seconds": the
     * item's text wins, so once a second); scaling 0.3. It goes where they go.
     */
    static final class FireVeil implements AbilityHandler {
        static final Magic.Spell VEIL = new Magic.Spell(15_000, 0.3);
        static final double RADIUS = 5;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Hits.overTime(player, item, tag, Hits.spellOf(item, VEIL), 1, 20, 5, () -> Hits.near(player.getLocation(), RADIUS));
            new BukkitRunnable() {
                private int ticks;

                @Override
                public void run() {
                    if ((ticks += 5) > 100 || !Hits.canStillHit(player)) {
                        cancel();
                        return;
                    }
                    ring(player.getLocation(), RADIUS, Particle.FLAME);
                }
            }.runTaskTimer(Dungeons.getInstance(), 0, 5);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 0.8f);
        }
    }

    /**
     * The Fire Fury Staff's Firestorm: "Shoots a projectile that on impact creates a firestorm, dealing up to
     * 42,000 damage over 10 seconds to mobs in a 7 block radius" (a tenth each second), scaling 0.3.
     */
    static final class Firestorm implements AbilityHandler {
        static final Magic.Spell STORM = new Magic.Spell(42_000, 0.3);
        static final double RADIUS = 7;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Magic.Spell spell = Hits.spellOf(item, STORM);
            Location eye = player.getEyeLocation();
            new Missile(player, eye, eye.getDirection().multiply(1.2))
                    .range(40)
                    .look(Missile.display(eye, new ItemStack(Material.FIRE_CHARGE), 0.8f, 0))
                    .trail(at -> at.getWorld().spawnParticle(Particle.FLAME, at, 3, 0.1, 0.1, 0.1, 0))
                    .onEnd((missile, at, impact) -> storm(missile.caster(), item, tag, spell, at))
                    .launch();
            player.getWorld().playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 1, 0.7f);
        }

        private static void storm(Player caster, SkyBlockItem item, NBTTagCompound tag, Magic.Spell spell, Location at) {
            Hits.overTime(caster, item, tag, spell, 0.1, 20, 10, () -> Hits.near(at, RADIUS));
            new BukkitRunnable() {
                private int ticks;

                @Override
                public void run() {
                    if ((ticks += 10) > 200) {
                        cancel();
                        return;
                    }
                    at.getWorld().spawnParticle(Particle.FLAME, at, 40, RADIUS / 2, 1.5, RADIUS / 2, 0.02);
                }
            }.runTaskTimer(Dungeons.getInstance(), 0, 10);
        }
    }

    /**
     * The Starlight Wand's Starfall: "Shower stars in a 3 blocks area for 5s, dealing 300 damage per second to
     * mobs within"; "places down a cloud on the position of the caster's crosshair up to 10 blocks away ...
     * dealing damage every 0.25 seconds" (the wiki): a quarter of it every 5 ticks; scaling 0.3. The area as a
     * ball 1.5 blocks across the middle.
     */
    static final class Starfall implements AbilityHandler {
        static final Magic.Spell STARS = new Magic.Spell(300, 0.3);

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Location at = Hits.aimed(player, 10);
            Hits.overTime(player, item, tag, Hits.spellOf(item, STARS), 0.25, 5, 20, () -> Hits.near(at, 1.5));
            new BukkitRunnable() {
                private int ticks;

                @Override
                public void run() {
                    if ((ticks += 5) > 100) {
                        cancel();
                        return;
                    }
                    at.getWorld().spawnParticle(Particle.END_ROD, at.clone().add(0, 3, 0), 6, 1, 0.2, 1, 0.05);
                }
            }.runTaskTimer(Dungeons.getInstance(), 0, 5);
            player.getWorld().playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1.2f);
        }
    }

    /**
     * The Pigman Sword's Burning Souls: "Gain +75 Defense for 5s and cast vortex of flames towards enemies,
     * dealing up to 30,000 over 5 seconds"; "summons slow moving vortexes of flame to all nearby mobs. When one
     * sticks to a mob, it deals 30,000 base ability damage over 5 seconds to it" (the wiki); scaling 0.1, "Not
     * affected by the Additive Multiplier". "Nearby" is UNKNOWN (10 blocks).
     */
    static final class BurningSouls implements AbilityHandler {
        static final Magic.Spell SOULS = new Magic.Spell(30_000, 0.1, false, false);
        static final double NEAR = 10;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            PlayerSession.of(player).buff("Burning Souls", new Stats().set(Stat.DEFENSE, 75), 5_000);
            Magic.Spell spell = Hits.spellOf(item, SOULS);
            Location from = player.getLocation().add(0, 1, 0);
            for (LivingEntity target : Hits.near(from, NEAR)) {
                new Missile(player, from, target.getBoundingBox().getCenter().subtract(from.toVector()).normalize().multiply(0.5))
                        .range(NEAR * 2)
                        .throughBlocks()
                        .steer(missile -> target.isValid() ? target.getBoundingBox().getCenter().subtract(missile.at().toVector()) : null)
                        .trail(at -> at.getWorld().spawnParticle(Particle.FLAME, at, 3, 0.1, 0.1, 0.1, 0.01))
                        .onHit((missile, mob) -> {
                            Hits.overTime(missile.caster(), item, tag, spell, 0.2, 20, 5, () -> List.of(mob));
                            return false;
                        })
                        .launch();
            }
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_BURN, 1, 0.8f);
        }
    }

    /**
     * The Voodoo Dolls' Acupuncture: "Shoot arrows from every direction around the targeted monster. Monsters
     * hit by at least one arrow are slowed and receive 1,500 damage/s for 10s" (the seconds read off its text);
     * base its data's, scaling 1 (the wiki). The target is the monster in their crosshair (within 30 blocks,
     * UNKNOWN), and those within 2 blocks of it are hit too (UNKNOWN); without one it isn't cast and costs
     * nothing. How much it slows is UNKNOWN (Slowness I).
     */
    static final class Acupuncture implements AbilityHandler {
        static final Magic.Spell NEEDLES = new Magic.Spell(1_500, 1);

        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            return Hits.aimedMob(player, 30) != null;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            LivingEntity target = Hits.aimedMob(player, 30);
            if (target == null) return;
            double seconds = Bolts.number(block.text(), SECONDS);
            int times = (int) Math.max(1, seconds);
            Location at = target.getBoundingBox().getCenter().toLocation(target.getWorld());
            for (LivingEntity mob : Hits.near(at, 2)) {
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, times * 20, 0, false, false, false));
                Hits.overTime(player, item, tag, Hits.spellOf(item, NEEDLES), 1, 20, times, () -> List.of(mob));
            }
            at.getWorld().spawnParticle(Particle.CRIT, at, 40, 1.5, 1.5, 1.5, 0.3);
            at.getWorld().playSound(at, Sound.ENTITY_ARROW_HIT, 1, 0.8f);
        }
    }
}
