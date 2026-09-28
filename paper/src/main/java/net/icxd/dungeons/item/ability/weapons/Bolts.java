package net.icxd.dungeons.item.ability.weapons;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Color;
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
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.modifier.ItemModifiers;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;

/**
 * Spells that send one thing at their enemies, or a ray. Their base damage is the item's own (its data's
 * Weapon Ability Damage), their Intelligence scaling the wiki's; how fast and far what they send goes, and
 * how it looks, are UNKNOWN unless said. None has a chat line (UNKNOWN: none recorded or in the mods).
 */
final class Bolts {
    private Bolts() {
    }

    /** A number in the ability's text, by a pattern with one group ("explosion of 3 blocks"); -1 if it isn't there. */
    static double number(List<String> text, Pattern pattern) {
        Matcher matcher = pattern.matcher(String.join(" ", text).replaceAll("[&§].", "").replace(",", ""));
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : -1;
    }

    /** The spell's damage on one mob, with its gray number; false if it couldn't be hurt. */
    private static boolean hit(Player caster, SkyBlockItem item, NBTTagCompound tag, Magic.Spell spell, LivingEntity mob, double share) {
        return Hits.hurt(caster, mob, Hits.magic(caster, item, tag, spell, mob) * share * Hits.takenFactor(mob), DamageIndicators.Look.NORMAL, tag);
    }

    /**
     * The Frozen and Glacial Scythes' Ice Bolt: "Shoots 1 Ice Bolt that deals 1,000 damage and slows enemies
     * hit for 5 seconds!", the Glacial's "When hitting the ground, also creates an explosion of 3 blocks,
     * dealing the same damage" (read off its text); Intelligence scaling 0.3 (the wiki). How much it slows is
     * UNKNOWN (Slowness II).
     */
    static final class IceBolt implements AbilityHandler {
        static final Magic.Spell BOLT = new Magic.Spell(1_000, 0.3);
        private static final Pattern EXPLOSION = Pattern.compile("explosion of (\\d+) blocks");
        private static final int SLOW_TICKS = 100;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Magic.Spell spell = Hits.spellOf(item, BOLT);
            double explosion = number(block.text(), EXPLOSION);
            Location eye = player.getEyeLocation();
            new Missile(player, eye, eye.getDirection().multiply(1.5))
                    .range(30)
                    .width(0.5)
                    .look(Missile.display(eye, new ItemStack(Material.PACKED_ICE), 0.4f, 0))
                    .trail(at -> at.getWorld().spawnParticle(Particle.SNOWFLAKE, at, 2, 0.1, 0.1, 0.1, 0))
                    .onHit((missile, mob) -> {
                        if (hit(missile.caster(), item, tag, spell, mob, 1)) slow(mob);
                        return false;
                    })
                    .onEnd((missile, at, impact) -> {
                        if (!impact || explosion <= 0) return;
                        at.getWorld().spawnParticle(Particle.SNOWFLAKE, at, 30, explosion / 2, 0.5, explosion / 2, 0.05);
                        for (LivingEntity mob : Hits.near(at, explosion)) {
                            if (hit(missile.caster(), item, tag, spell, mob, 1)) slow(mob);
                        }
                    })
                    .launch();
            player.getWorld().playSound(eye, Sound.ENTITY_SNOW_GOLEM_SHOOT, 1, 0.8f);
        }

        private static void slow(LivingEntity mob) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOW_TICKS, 1, false, false, false));
        }
    }

    /**
     * The Ink Wand's Ink Bomb: "Shoot an ink bomb in front of you dealing 10,000 damage and giving blindness!";
     * "fires an Ink Sac covered in black particles in an arc. The Ink Sac deals 10,000 damage to all mobs in a
     * 0.5 block radius" (the wiki), scaling 1 (the wiki's table). How long the blindness lasts is UNKNOWN (5 s).
     */
    static final class InkBomb implements AbilityHandler {
        static final Magic.Spell INK = new Magic.Spell(10_000, 1);

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Magic.Spell spell = Hits.spellOf(item, INK);
            Location eye = player.getEyeLocation();
            new Missile(player, eye, eye.getDirection().multiply(1.2).add(new Vector(0, 0.2, 0)))
                    .gravity(0.05)
                    .range(40)
                    .look(Missile.display(eye, new ItemStack(Material.INK_SAC), 0.6f, 0))
                    .trail(at -> at.getWorld().spawnParticle(Particle.SQUID_INK, at, 2, 0.1, 0.1, 0.1, 0))
                    .onEnd((missile, at, impact) -> {
                        if (!impact) return;
                        for (LivingEntity mob : Hits.near(at, 0.5)) {
                            if (hit(missile.caster(), item, tag, spell, mob, 1)) {
                                mob.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false, false));
                            }
                        }
                        at.getWorld().spawnParticle(Particle.SQUID_INK, at, 20, 0.4, 0.4, 0.4, 0.05);
                    })
                    .launch();
            player.getWorld().playSound(eye, Sound.ENTITY_SQUID_SQUIRT, 1, 1);
        }
    }

    /**
     * The Ember Rod's Fire Blast: "Shoot 3 Fireballs which deal 30 damage in rapid succession in front of you!",
     * scaling 1 (the wiki). Each fireball hits the first mob it meets. How rapid is UNKNOWN (every 5 ticks).
     */
    static final class FireBlast implements AbilityHandler {
        static final Magic.Spell FIREBALL = new Magic.Spell(30, 1);

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Magic.Spell spell = Hits.spellOf(item, FIREBALL);
            new BukkitRunnable() {
                private int shot;

                @Override
                public void run() {
                    if (shot++ >= 3 || !Hits.canStillHit(player)) {
                        cancel();
                        return;
                    }
                    Location eye = player.getEyeLocation();
                    new Missile(player, eye, eye.getDirection().multiply(1.2))
                            .range(30)
                            .look(Missile.display(eye, new ItemStack(Material.FIRE_CHARGE), 0.6f, 0))
                            .trail(at -> at.getWorld().spawnParticle(Particle.FLAME, at, 2, 0.1, 0.1, 0.1, 0))
                            .onHit((missile, mob) -> {
                                hit(missile.caster(), item, tag, spell, mob, 1);
                                return false;
                            })
                            .launch();
                    player.getWorld().playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
                }
            }.runTaskTimer(Dungeons.getInstance(), 0, 5);
        }
    }

    /**
     * The Alchemist's Staff's Coin Conversion: "Spend coins to cast a projectile which deals 10,000 damage on
     * hit and knocks enemies back. Coin Cost: 100,000" (the cost read off its text), scaling 0.3 (the wiki).
     * Without the coins it says so (UNKNOWN in what words) and nothing is spent.
     */
    static final class CoinConversion implements AbilityHandler {
        static final Magic.Spell COIN = new Magic.Spell(10_000, 0.3);
        private static final Pattern COST = Pattern.compile("Coin Cost: (\\d+)");

        private static double cost(ItemBlock block) {
            return Math.max(0, number(block.text(), COST));
        }

        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            User user = User.ifLoaded(player.getUniqueId());
            if (user != null && Purse.has(user, cost(block))) return true;
            player.sendMessage(Utils.color("&cYou don't have enough coins!"));
            return false;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            User user = User.ifLoaded(player.getUniqueId());
            if (user == null || !Purse.take(user, cost(block))) return;
            Magic.Spell spell = Hits.spellOf(item, COIN);
            Location eye = player.getEyeLocation();
            new Missile(player, eye, eye.getDirection().multiply(1.5))
                    .range(30)
                    .look(Missile.display(eye, new ItemStack(Material.GOLD_NUGGET), 0.6f, 0))
                    .onHit((missile, mob) -> {
                        if (hit(missile.caster(), item, tag, spell, mob, 1)) {
                            mob.setVelocity(missile.velocity().normalize().multiply(1.2).setY(0.4));
                        }
                        return false;
                    })
                    .launch();
            player.getWorld().playSound(eye, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 0.6f);
        }
    }

    /**
     * The Staff of the Rising Sun's Ray of Hope: "Channels your energy towards the target location, exploding
     * on impact for ... damage. Costs 10% of your total mana to use" (the share read off its text; its block
     * has no mana cost), 1% less for each of its Mana Disintegrators, as a wand's (0.20.5: "Fixed Mana
     * Disintegrators not working on the Staff Of The Rising Sun despite it being a wand"; UNKNOWN whether this
     * share goes down 1% or 2% a disintegrator, as a power orb's does). The blast's size and the scaling are
     * UNKNOWN (3 blocks, 0.3). The Staff of the Rising Moon's (only for Wizardman, an event's) does nothing.
     */
    static final class RayOfHope implements AbilityHandler {
        static final Magic.Spell RAY = new Magic.Spell(500, 0.3);
        private static final Pattern SHARE = Pattern.compile("Costs (\\d+)% of your total mana");

        static int cost(Player player, ItemBlock block, NBTTagCompound tag) {
            return cost(block, tag, PlayerSession.of(player).maxMana());
        }

        /** Its share of {@code maxMana}, less its Mana Disintegrators' (see ItemModifiers#manaFactor). */
        static int cost(ItemBlock block, NBTTagCompound tag, int maxMana) {
            double share = Math.max(0, number(block.text(), SHARE));
            return (int) Math.round(share / 100 * ItemModifiers.manaFactor(tag) * maxMana);
        }

        @Override
        public boolean casts(Player player, SkyBlockItem item, NBTTagCompound tag) {
            return !item.id().equals("STAFF_OF_THE_RISING_MOON");
        }

        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            return Hits.enoughMana(player, cost(player, block, tag));
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Hits.takeMana(player, cost(player, block, tag), block.name());
            Magic.Spell spell = Hits.spellOf(item, RAY);
            Location eye = player.getEyeLocation();
            Vector to = Hits.aimed(player, 40).toVector().subtract(eye.toVector());
            Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 200, 60), 1);
            new Missile(player, eye, to.normalize().multiply(1.5))
                    .range(40)
                    .trail(at -> at.getWorld().spawnParticle(Particle.DUST, at, 2, 0.05, 0.05, 0.05, 0, gold))
                    .onEnd((missile, at, impact) -> {
                        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 1);
                        for (LivingEntity mob : Hits.near(at, 3)) hit(missile.caster(), item, tag, spell, mob, 1);
                    })
                    .launch();
            player.getWorld().playSound(eye, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.6f);
        }
    }

    /**
     * The Aurora Staff's Runic Zap: "Fires a beam of energy, hitting the first enemy on its path for 10,000.
     * Deals reduced damage past 10 blocks"; "Damage decays at a rate of e^(-0.0225*Distance) per block" (the
     * wiki, from a forum thread), taken as from 10 blocks on; scaling 0.2 (the wiki's table). Its runes and
     * the Aurora Armor's matching bonus aren't built (LATER). The beam's reach is UNKNOWN (32 blocks).
     */
    static final class RunicZap implements AbilityHandler {
        static final Magic.Spell ZAP = new Magic.Spell(10_000, 0.2);
        static final double FULL = 10;
        static final double RANGE = 32;

        /** Its share of damage this far away: all of it up to 10 blocks, then e^(-0.0225 x blocks past 10). */
        static double decay(double distance) {
            return distance <= FULL ? 1 : Math.exp(-0.0225 * (distance - FULL));
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Location eye = player.getEyeLocation();
            LivingEntity mob = Hits.aimedMob(player, RANGE);
            Location end = mob != null ? mob.getBoundingBox().getCenter().toLocation(player.getWorld()) : Hits.aimed(player, RANGE);
            Particle.DustOptions blue = new Particle.DustOptions(Color.fromRGB(90, 200, 255), 0.8f);
            Vector step = end.toVector().subtract(eye.toVector());
            double length = step.length();
            step.normalize().multiply(0.4);
            Location at = eye.clone();
            for (double gone = 0; gone < length; gone += 0.4, at.add(step)) player.getWorld().spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0, blue);
            if (mob != null) hit(player, item, tag, Hits.spellOf(item, ZAP), mob, decay(length));
            player.getWorld().playSound(eye, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 1.6f);
        }
    }

    /**
     * The Bingo Blaster's Bingo Blast: "Shoots out a piercing ray of water that travels up to 15 blocks. Deals
     * ... damage to the first enemy hit. Damage is halved for each subsequent enemy hit"; base 625, scaling 0.2
     * (the wiki). Walls stop it (UNKNOWN).
     */
    static final class BingoBlast implements AbilityHandler {
        static final Magic.Spell RAY = new Magic.Spell(625, 0.2);
        static final double RANGE = 15;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Location eye = player.getEyeLocation();
            double length = Hits.aimed(player, RANGE).distance(eye);
            Magic.Spell spell = Hits.spellOf(item, RAY);
            double share = 1;
            for (LivingEntity mob : Hits.along(eye, eye.getDirection(), length, 0.3)) {
                if (hit(player, item, tag, spell, mob, share)) share /= 2;
            }
            Vector step = eye.getDirection().multiply(0.5);
            Location at = eye.clone();
            for (double gone = 0; gone < length; gone += 0.5, at.add(step)) player.getWorld().spawnParticle(Particle.SPLASH, at, 2, 0, 0, 0, 0);
            player.getWorld().playSound(eye, Sound.ENTITY_GENERIC_SPLASH, 1, 1.2f);
        }
    }
}
