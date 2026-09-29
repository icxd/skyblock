package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.Mana;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;

/**
 * Armor's sneak abilities that hit (worn pieces' SNEAK blocks come through Activations): the Aurora Armor's Homing
 * Missiles, the Fervor Armor's Ground Pound and the Precursor Eye's (and the Giant's Eye Sword's) Eye Beam. Each
 * reads its numbers from its text. Main thread.
 */
final class ArmorAbilities {
    private ArmorAbilities() {
    }

    /**
     * The Aurora Armor's Homing Missiles: "At 10 stacks, sneak to reset your stacks and shoot 3 homing missiles dealing
     * 5,000 Damage each" (4 or 5 on the better tiers, off their text): magic damage, base 5,000 with Intelligence
     * scaling 0.3 (the armor inventory's reading of the wiki's Aurora Armor), each missile after the nearest mob in 20
     * blocks. The stacks are the Aurora Armor's Arcane Energy, the armor bonuses' (BONUSES.md): {@link
     * WeaponAbilities#arcaneEnergy} says where they come from, and until something gives them there are none, so it
     * never goes off. How the missiles
     * fly and look is UNKNOWN.
     */
    static final class HomingMissiles implements AbilityHandler {
        static final String NAME = "Homing Missiles";
        static final int STACKS = 10;
        static final Magic.Spell MISSILE = new Magic.Spell(5_000, 0.3);
        private static final Pattern SHOOT = Pattern.compile("shoot (\\d+) homing missiles dealing ([\\d,]+) Damage each");
        private static final double SEEK = 20;
        private static ToIntFunction<Player> stacks = player -> 0;
        private static Consumer<Player> spend = player -> {
        };

        /** Where their Arcane Energy stacks come from, and how they're spent (the Aurora Armor's tiered bonus). */
        static void stacksFrom(ToIntFunction<Player> stacks, Consumer<Player> spend) {
            HomingMissiles.stacks = stacks;
            HomingMissiles.spend = spend;
        }

        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            return stacks.applyAsInt(player) >= STACKS;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            spend.accept(player);
            Matcher m = SHOOT.matcher(AbilityText.plain(block.text()));
            int missiles = 3;
            Magic.Spell spell = MISSILE;
            if (m.find()) {
                missiles = Integer.parseInt(m.group(1));
                spell = MISSILE.withBase(Double.parseDouble(m.group(2).replace(",", "")));
            }
            Magic.Spell each = spell;
            Location eye = player.getEyeLocation();
            for (int i = 0; i < missiles; i++) {
                Location from = eye.clone();
                from.setYaw(eye.getYaw() + (i - (missiles - 1) / 2f) * 20);
                from.setPitch(-30);
                Particle.DustOptions colour = new Particle.DustOptions(Color.fromRGB(120, 200, 255), 1);
                new Missile(player, eye, from.getDirection().multiply(0.8))
                        .range(40)
                        .trail(at -> at.getWorld().spawnParticle(Particle.DUST, at, 2, 0.05, 0.05, 0.05, 0, colour))
                        .steer(missile -> {
                            List<LivingEntity> near = Hits.near(missile.at(), SEEK);
                            return near.isEmpty() ? null : near.get(0).getBoundingBox().getCenter().subtract(missile.at().toVector());
                        })
                        .onHit((missile, mob) -> {
                            Hits.spell(missile.caster(), item, tag, each, List.of(mob));
                            return false;
                        })
                        .launch();
            }
            player.getWorld().playSound(eye, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.2f);
        }
    }

    /**
     * The Fervor Armor's Ground Pound: "At 10 stacks, sneak to reset your stacks and perform a Ground Pound dealing
     * damage to mobs within 6 blocks. (Damage scales with your EHP, max 25M)" (2 pounds and more blocks on the better
     * tiers, off their text; the 25M is the wiki's, "up to 25,000,000 per Ground Pound"), on the Fervor Armor's stacks
     * ({@link SetBonuses#fervor}). How it scales with EHP is UNKNOWN: each pound deals their EHP (max health x (1 +
     * Defense / 100)), as it is, to each mob in reach, as an ability's hit; a second pound half a second after the
     * first (UNKNOWN).
     */
    static final class GroundPound implements AbilityHandler {
        static final int STACKS = 10;
        static final double MOST = 25_000_000;
        private static final Pattern WITHIN = Pattern.compile("mobs within (\\d+) blocks");

        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            return SetBonuses.fervor(player) >= STACKS;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            SetBonuses.spendFervor(player);
            String plain = AbilityText.plain(block.text());
            int pounds = plain.contains("2 Ground Pounds") ? 2 : 1;
            Matcher m = WITHIN.matcher(plain);
            double radius = m.find() ? Double.parseDouble(m.group(1)) : 6;
            for (int i = 0; i < pounds; i++) {
                Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> pound(player, tag, radius), i * 10L);
            }
        }

        private static void pound(Player player, NBTTagCompound tag, double radius) {
            if (!Hits.canStillHit(player)) return;
            Location at = player.getLocation();
            at.getWorld().spawnParticle(Particle.EXPLOSION, at, 6, radius / 3, 0.2, radius / 3, 0);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.6f);
            double damage = damage(PlayerHealth.max(player), PlayerSession.of(player).stats().get(Stat.DEFENSE));
            for (LivingEntity mob : Hits.near(at, radius)) Hits.hurt(player, mob, damage * Hits.takenFactor(mob), DamageIndicators.Look.NORMAL, tag);
        }

        /** A pound's damage: their EHP, at most 25M. */
        static double damage(double maxHealth, double defense) {
            return Math.min(MOST, Math.max(0, maxHealth) * (1 + Math.max(0, defense) / 100));
        }
    }

    /**
     * The Precursor Eye's Eye Beam (SNEAK) and the Giant's Eye Sword's (right click): "Fire a laser in front of you
     * dealing 4000 damage and costing 40 mana. The damage increases by 100% every second for 5 seconds and the mana cost
     * increases by 25% every second. You can sneak again to de-activate the laser. Mana is consumed on damage." The
     * wiki's Precursor Eye: "a range of 30 blocks", "costing 40 mana per second", "Intelligence and Ability Damage do not
     * work with the Eye Beam ability". So once a second every mob on the beam takes its damage as it is (an ability's
     * hit), and if it hit any, that second's mana is paid; it stops when they can't pay, use it again, or the piece
     * that fires it is off. Both grow by their share of the first second's each second (UNKNOWN whether they compound),
     * the damage for 5 seconds. The sword's is x1.5 with a Precursor Eye on ("Damage of the laser is increased by x1.5
     * while wearing Precursor Eye"). The beam's width is UNKNOWN (half a block).
     */
    static final class EyeBeam implements AbilityHandler {
        static final String NAME = "Eye Beam";
        static final double RANGE = 30;
        private static final double WIDTH = 0.5;
        private static final Pattern NUMBERS = Pattern.compile(
                "dealing (\\d+) damage and costing (\\d+) mana\\. The damage increases by (\\d+)% every second for (\\d+) seconds and the mana cost increases by (\\d+)% every second");
        private static final Map<UUID, BukkitRunnable> FIRING = new HashMap<>();

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            BukkitRunnable firing = FIRING.remove(player.getUniqueId());
            if (firing != null) {
                firing.cancel();
                return;
            }
            Matcher m = NUMBERS.matcher(AbilityText.plain(block.text()));
            if (!m.find()) return;
            double damage = Double.parseDouble(m.group(1));
            double mana = Double.parseDouble(m.group(2));
            double damageGrowth = Double.parseDouble(m.group(3)) / 100;
            int growSeconds = Integer.parseInt(m.group(4));
            double manaGrowth = Double.parseDouble(m.group(5)) / 100;
            boolean worn = !item.statsWhenHeld();
            double factor = !worn && wearingEye(player) ? 1.5 : 1;
            String id = item.id();
            BukkitRunnable beam = new BukkitRunnable() {
                private int ticks;

                @Override
                public void run() {
                    if (!Hits.canStillHit(player) || !(worn ? wearing(player, id) : holding(player, id))) {
                        stop();
                        return;
                    }
                    Location eye = player.getEyeLocation();
                    Vector direction = eye.getDirection();
                    draw(eye, direction);
                    if (ticks++ % 20 != 0) return;
                    int second = ticks / 20;
                    List<LivingEntity> caught = Hits.along(eye, direction, RANGE, WIDTH);
                    if (caught.isEmpty()) return;
                    int cost = (int) Math.round(grown(mana, manaGrowth, second, Integer.MAX_VALUE));
                    if (Mana.get(player) < cost) {
                        stop();
                        return;
                    }
                    Hits.takeMana(player, cost, NAME);
                    double hit = grown(damage, damageGrowth, second, growSeconds) * factor;
                    for (LivingEntity mob : caught) Hits.hurt(player, mob, hit * Hits.takenFactor(mob), DamageIndicators.Look.NORMAL, tag);
                }

                private void stop() {
                    cancel();
                    FIRING.remove(player.getUniqueId(), this);
                }
            };
            FIRING.put(player.getUniqueId(), beam);
            beam.runTaskTimer(Dungeons.getInstance(), 0, 1);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 1.6f);
        }

        /** What starts at {@code first} and grows by {@code growth} of it each second is at second {@code second} (growing for {@code seconds} at most). */
        static double grown(double first, double growth, int second, int seconds) {
            return first * (1 + growth * Math.min(Math.max(0, second), seconds));
        }

        private static void draw(Location eye, Vector direction) {
            Particle.DustOptions red = new Particle.DustOptions(Color.RED, 0.8f);
            Location at = eye.clone().add(direction.clone().multiply(1));
            Vector step = direction.clone().multiply(1.5);
            for (int i = 0; i < RANGE / 1.5; i++) {
                if (at.getBlock().getType().isOccluding()) break;
                at.getWorld().spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0, red);
                at.add(step);
            }
        }

        private static boolean wearingEye(Player player) {
            return wearing(player, "PRECURSOR_EYE");
        }

        private static boolean wearing(Player player, String id) {
            return !SetBonuses.worn(player).matching(id::equals).isEmpty();
        }

        private static boolean holding(Player player, String id) {
            NBTTagCompound held = Combat.skyBlockData(player.getInventory().getItemInMainHand());
            return held != null && id.equals(held.getString("id"));
        }

        static void forget(UUID player) {
            BukkitRunnable firing = FIRING.remove(player);
            if (firing != null) firing.cancel();
        }
    }
}
