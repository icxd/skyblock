package net.icxd.dungeons.item.ability.weapons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.ability.utility.Heals;
import net.icxd.dungeons.item.ability.utility.UtilityListener;
import net.icxd.dungeons.item.bonus.Bonus;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.bonus.Worn;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.PlayerAttributes;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Worn armor's and equipment's own passive abilities that hit (ABILITIES_WEAPONS.md, "Worn"), found by their ABILITY
 * block's name on a worn piece as the utility ones are (item/ability/utility/WornPassives): the Pufferfish Hat's
 * Spiky, the Vampire masks' Rejuvenate, the Creeper Pants' Detonate, the Gauntlet of Contagion's Contaminate, the
 * Witch masks' Bat Swarm and the Warden Helmet's Brute Force; and the Miniature Nuke's Detonate (a click). Each reads
 * its numbers from its text. Their damage is an effect's ({@link MobHits#deal}: no hit listeners, no Ferocity) unless
 * said. Main thread.
 */
public final class WornStrikes {
    static final String KIND = "ABILITY";
    /** Spiky: "your attacks have a chance to deal 10 damage plus 20% of your Strength to nearby enemies": the chance and how near are UNKNOWN. */
    static final double SPIKY_CHANCE = 0.25;
    static final double SPIKY_RADIUS = 3;
    /** Detonate: "Causes an explosion when dropping below 20% HP, damaging and knocking back all monsters around you": how far is UNKNOWN. */
    static final double DETONATE_RADIUS = 5;
    private static final double DETONATE_KNOCKBACK = 1.5;
    /** Bat Swarm: how far its bats look for a mob, how long one takes to come back after it explodes, and its blast (all UNKNOWN). */
    static final double SWARM_RANGE = 8;
    static final long SWARM_BACK_MILLIS = 5_000;
    static final double SWARM_BLAST = 3;
    private static final int SWARM_BATS = 2;

    private static final Pattern SPIKY = Pattern.compile("deal ([\\d.]+) damage plus ([\\d.]+)% of your Strength");
    private static final Pattern DRAINS = Pattern.compile("drains ([\\d.]+) health per second from all monsters within ([\\d.]+) blocks");
    private static final Pattern BELOW = Pattern.compile("dropping below ([\\d.]+)% HP");
    private static final Pattern CONTAMINATE = Pattern.compile("dealing ([\\d.]+)% of their total ❤ Health as damage to all enemies within ([\\d.]+) blocks");
    private static final Pattern SWARM = Pattern.compile("Summons (\\w+) bats that will follow you and attack nearby enemies, dealing ([\\d,]+) damage upon exploding");
    private static final Pattern HALVES = Pattern.compile("grants \\+([\\d.]+)% base weapon damage for every \\+([\\d.]+)✦ Speed");
    private static final Pattern NUKE = Pattern.compile("dealing ([\\d.]+)% max health true damage to all enemies in a ([\\d.]+) block radius");

    /** Mobs a Contaminate blast has caught, and whose kill it'll be when they die, with what they'll explode for. */
    private record Contaminated(LivingEntity mob, UUID by) {
    }

    private static final List<Contaminated> CONTAMINATED = new ArrayList<>();
    /** Each wearer's Bat Swarm: its bats, and when each is back after exploding (0 while it's there). */
    private static final Map<UUID, Swarm> SWARMS = new HashMap<>();

    private static final class Swarm {
        final Bat[] bats = new Bat[SWARM_BATS];
        final long[] backAt = new long[SWARM_BATS];
    }

    private WornStrikes() {
    }

    /** The bonuses these are, for SetBonuses (see WornAbilities). */
    public static List<Bonus> all() {
        return List.of(new Rejuvenate(), new Detonate(), new Contaminate(), new BatSwarm(), new BruteForce());
    }

    /** The hooks they need besides being bonuses: Spiky on hits, a tick for the bats. */
    static void register() {
        Combat.addHitListener(WornStrikes::landed);
        Bukkit.getScheduler().runTaskTimer(Dungeons.getInstance(), WornStrikes::tick, 1, 1);
    }

    abstract static class Passive implements Bonus {
        private final String name;

        Passive(String name) {
            this.name = name;
        }

        @Override
        public String kind() {
            return KIND;
        }

        @Override
        public String name() {
            return name;
        }

        ItemBlock block(Active active) {
            for (Worn.Piece piece : active.pieces()) {
                for (ItemBlock block : piece.blocks()) if (block.isAbility() && name.equals(block.name())) return block;
            }
            return null;
        }

        String text(Active active) {
            ItemBlock block = block(active);
            return block == null ? "" : AbilityText.plain(block.text());
        }
    }

    /** The ABILITY block of this name on a piece they wear; null for none (nothing made: every melee hit asks). */
    static ItemBlock worn(Player player, String name) {
        for (Worn.Piece piece : SetBonuses.worn(player).pieces()) {
            for (ItemBlock block : piece.blocks()) if (block.isAbility() && name.equals(block.name())) return block;
        }
        return null;
    }

    private static double number(Pattern pattern, String plain, int group, double otherwise) {
        Matcher m = pattern.matcher(plain);
        return m.find() ? Double.parseDouble(m.group(group).replace(",", "")) : otherwise;
    }

    // ---------- Spiky ----------

    /**
     * The Pufferfish Hat's Spiky: "While wearing, your attacks have a chance to deal 10 damage plus 20% of your
     * Strength to nearby enemies!": a melee hit's chance to hurt each other mob near them that much.
     */
    private static void landed(Player player, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
        if (landing.kind() != HitKind.MELEE) return;
        ItemBlock spiky = worn(player, "Spiky");
        if (spiky == null || ThreadLocalRandom.current().nextDouble() >= SPIKY_CHANCE) return;
        String plain = AbilityText.plain(spiky.text());
        double strength = PlayerSession.of(player).stats().get(Stat.STRENGTH);
        double dealt = spiky(number(SPIKY, plain, 1, 10), number(SPIKY, plain, 2, 20), strength);
        for (LivingEntity mob : Hits.near(player.getLocation(), SPIKY_RADIUS)) {
            if (!mob.equals(landing.entity())) MobHits.deal(player, mob, dealt, DamageIndicators.Look.NORMAL, HitKind.OTHER, null);
        }
    }

    /** "10 damage plus 20% of your Strength". */
    static double spiky(double flat, double percent, double strength) {
        return flat + percent / 100 * Math.max(0, strength);
    }

    // ---------- Rejuvenate ----------

    /**
     * The Vampire masks' Rejuvenate: "While wearing, drains 5 health per second from all monsters within 8 blocks",
     * and "The wearer heals for the amount the ability damages mobs" (the wiki's Vampire Mask).
     */
    static final class Rejuvenate extends Passive {
        Rejuvenate() {
            super("Rejuvenate");
        }

        @Override
        public void second(Player player, Active active) {
            String plain = text(active);
            double drain = number(DRAINS, plain, 1, 5);
            double range = number(DRAINS, plain, 2, 8);
            double drained = 0;
            for (LivingEntity mob : Hits.near(player.getLocation(), range)) {
                if (MobHits.deal(player, mob, drain, DamageIndicators.Look.NORMAL, HitKind.OTHER, null)) drained += drain;
            }
            if (drained > 0) Heals.give(player, player, drained);
        }
    }

    // ---------- Detonate ----------

    /**
     * The Creeper Pants' Detonate: "Causes an explosion when dropping below 20% HP, damaging and knocking back all
     * monsters around you", every 60 seconds (its cooldown): when a hit takes them below it. What it deals is UNKNOWN:
     * a melee hit of what they hold on each mob around them (an ability's), times Consolidated.
     */
    static final class Detonate extends Passive {
        Detonate() {
            super("Detonate");
        }

        @Override
        public void hurt(Player player, Active active, Entity by, double taken) {
            ItemBlock block = block(active);
            if (block == null) return;
            double share = number(BELOW, AbilityText.plain(block.text()), 1, 20) / 100;
            double max = PlayerHealth.max(player);
            double now = PlayerHealth.get(player);
            if (!droppedBelow(now + taken, now, max, share)) return;
            PlayerSession session = PlayerSession.of(player);
            String key = "ability:" + block.name();
            if (session.cooldownLeft(key) > 0) return;
            session.startCooldown(key, Abilities.cooldownMillis(block, player));
            Location at = player.getLocation();
            at.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
            Hits.Strike strike = Hits.Strike.melee(Explosions.factor(player));
            NBTTagCompound held = Combat.skyBlockData(player.getInventory().getItemInMainHand());
            for (LivingEntity mob : Hits.near(at, DETONATE_RADIUS)) {
                Hits.weaponHit(player, held, mob, strike);
                Vector away = mob.getLocation().toVector().subtract(at.toVector()).setY(0);
                if (away.lengthSquared() > 0) mob.setVelocity(away.normalize().multiply(DETONATE_KNOCKBACK).setY(0.5));
            }
        }
    }

    /** Whether going from {@code before} to {@code after} health (of {@code max}) drops them below {@code share} of it. */
    static boolean droppedBelow(double before, double after, double max, double share) {
        return max > 0 && after > 0 && before >= share * max && after < share * max;
    }

    /**
     * The Miniature Nuke's Detonate: "Detonate the Nuke, dealing 80.0% max health true damage to all enemies in a 20
     * block radius." (an admin item): each of SkyBlock's mobs in reach takes that share of its max health, through
     * nothing (true damage), times Consolidated. The Nuke stays (UNKNOWN whether it's used up).
     */
    static final class Nuke implements AbilityHandler {
        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            String plain = AbilityText.plain(block.text());
            double share = number(NUKE, plain, 1, 80) / 100 * Explosions.factor(player);
            double radius = number(NUKE, plain, 2, 20);
            Location at = player.getLocation();
            at.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, at, 3, 2, 1, 2, 0);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.5f);
            for (LivingEntity mob : Hits.near(at, radius)) {
                Damage.Target target = Hits.target(mob);
                if (target != null) MobHits.deal(player, mob, share * target.maxHealth(), DamageIndicators.Look.NORMAL, HitKind.OTHER, tag);
            }
        }
    }

    // ---------- Contaminate ----------

    /**
     * The Gauntlet of Contagion's Contaminate: "Killing an enemy causes an explosion dealing 10% of their total ❤ Health
     * as damage to all enemies within 2 blocks. Enemies in the blast radius will also be contaminated causing them to
     * explode on death." A contaminated mob's blast is the wearer's too, however it dies (UNKNOWN), and its own
     * blast contaminates again (a chain). Times Consolidated.
     */
    static final class Contaminate extends Passive {
        Contaminate() {
            super("Contaminate");
        }

        @Override
        public void killed(Player player, Active active, SkyBlockMobDeathEvent event) {
            String plain = text(active);
            double maxHealth = event.mob() != null ? event.mob().getMaxHealth() : event.variant().health();
            explode(player, event.location(), maxHealth, number(CONTAMINATE, plain, 1, 10) / 100, number(CONTAMINATE, plain, 2, 2));
        }
    }

    private static void explode(Player player, Location at, double maxHealth, double share, double radius) {
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 1);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.4f);
        double damage = share * maxHealth * Explosions.factor(player);
        for (LivingEntity mob : Hits.near(at, radius)) {
            MobHits.deal(player, mob, damage, DamageIndicators.Look.NORMAL, HitKind.OTHER, null);
            if (MobHits.alive(mob) && CONTAMINATED.stream().noneMatch(c -> c.mob().equals(mob))) CONTAMINATED.add(new Contaminated(mob, player.getUniqueId()));
        }
    }

    /**
     * A kill anywhere (see WeaponPassives): contaminated mobs that have died since explode where they fell, for their
     * wearer, while they wear the gauntlet (as its own blast's numbers).
     */
    static void killHappened() {
        if (CONTAMINATED.isEmpty()) return;
        List<Contaminated> died = new ArrayList<>();
        for (Iterator<Contaminated> it = CONTAMINATED.iterator(); it.hasNext(); ) {
            Contaminated c = it.next();
            if (MobHits.alive(c.mob())) continue;
            it.remove();
            died.add(c);
        }
        for (Contaminated c : died) {
            Player wearer = Bukkit.getPlayer(c.by());
            ItemBlock block = wearer == null ? null : worn(wearer, "Contaminate");
            if (block == null || !c.mob().getWorld().equals(wearer.getWorld())) continue;
            Damage.Target target = Hits.target(c.mob());
            double maxHealth = target != null ? target.maxHealth() : 0;
            String plain = AbilityText.plain(block.text());
            if (maxHealth > 0) explode(wearer, c.mob().getLocation(), maxHealth, number(CONTAMINATE, plain, 1, 10) / 100, number(CONTAMINATE, plain, 2, 2));
        }
    }

    // ---------- Bat Swarm ----------

    /**
     * The Witch masks' Bat Swarm: "Summons two bats that will follow you and attack nearby enemies, dealing 4,000 damage
     * upon exploding" (the Vampire Witch Mask's text says 3,000): two bats around their head; when a mob is near, one
     * flies at it and explodes (magic damage, the text's base with no Intelligence scaling: UNKNOWN, as the bats'
     * movement, their blast and when they're back are), times Consolidated. "Your bats will not spawn on your island":
     * there are no islands here.
     */
    static final class BatSwarm extends Passive {
        BatSwarm() {
            super("Bat Swarm");
        }

        @Override
        public void second(Player player, Active active) {
            ItemBlock block = block(active);
            if (block == null || active.pieces().isEmpty()) return;
            Worn.Piece piece = active.pieces().get(0);
            double damage = number(SWARM, AbilityText.plain(block.text()), 2, 0);
            Swarm swarm = SWARMS.computeIfAbsent(player.getUniqueId(), id -> new Swarm());
            long now = System.currentTimeMillis();
            List<LivingEntity> near = Hits.near(player.getLocation(), SWARM_RANGE);
            for (int i = 0; i < SWARM_BATS; i++) {
                if (swarm.bats[i] == null || !swarm.bats[i].isValid()) {
                    if (now < swarm.backAt[i]) continue;
                    swarm.bats[i] = bat(player.getEyeLocation());
                    continue;
                }
                if (near.isEmpty() || damage <= 0) continue;
                dive(player, piece, swarm.bats[i], near.get(ThreadLocalRandom.current().nextInt(near.size())), damage);
                swarm.bats[i] = null;
                swarm.backAt[i] = now + SWARM_BACK_MILLIS;
                // One bat a second.
                break;
            }
        }

        @Override
        public void ended(Player player) {
            forgetSwarm(player.getUniqueId());
        }
    }

    private static Bat bat(Location at) {
        return at.getWorld().spawn(at, Bat.class, b -> {
            b.setPersistent(false);
            b.setAI(false);
            b.setInvulnerable(true);
            b.setSilent(true);
            b.setGravity(false);
            b.setCollidable(false);
            b.addScoreboardTag(UtilityListener.NOT_A_MOB);
        });
    }

    private static void dive(Player player, Worn.Piece piece, Bat bat, LivingEntity at, double damage) {
        Magic.Spell spell = new Magic.Spell(Hits.base(piece.item(), damage), 0);
        new Missile(player, bat.getLocation(), at.getLocation().toVector().subtract(bat.getLocation().toVector()).normalize().multiply(0.8))
                .range(SWARM_RANGE * 3)
                .look(bat)
                .steer(missile -> at.isValid() ? at.getLocation().add(0, at.getHeight() / 2, 0).toVector().subtract(missile.at().toVector()) : null)
                .onEnd((missile, where, impact) -> {
                    where.getWorld().spawnParticle(Particle.EXPLOSION, where, 1);
                    where.getWorld().playSound(where, Sound.ENTITY_BAT_DEATH, 0.8f, 1.2f);
                    Hits.spell(missile.caster(), piece.item(), piece.tag(), spell.times(Explosions.factor(missile.caster())), Hits.near(where, SWARM_BLAST));
                })
                .launch();
    }

    /** Every tick: the bats waiting around their wearers stay with them. */
    private static void tick() {
        if (SWARMS.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Swarm> entry : SWARMS.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            Bat[] bats = entry.getValue().bats;
            for (int i = 0; i < bats.length; i++) {
                if (bats[i] == null || !bats[i].isValid()) continue;
                double angle = now / 300.0 + i * Math.PI;
                bats[i].teleport(player.getEyeLocation().add(Math.cos(angle) * 0.9, 0.6, Math.sin(angle) * 0.9));
            }
        }
    }

    private static void forgetSwarm(UUID player) {
        Swarm swarm = SWARMS.remove(player);
        if (swarm == null) return;
        for (Bat bat : swarm.bats) if (bat != null) bat.remove();
    }

    // ---------- Brute Force ----------

    /**
     * The Warden Helmet's Brute Force: "Halves your +25✦ Speed but grants +20% base weapon damage for every +25✦ Speed."
     * The wiki's Warden Helmet: the Speed cap counts before the halving, and "For every Speed 25 remaining, the helmet
     * grants a +20% additive damage multiplier, plus a final +1% additive damage on top of that"; the wiki's Additive
     * Sources: 20 x Speed / 25 + 1. So their Speed (up to their cap) is halved, and each hit gets 20% additive for each
     * whole 25 of what's left, and 1% more.
     */
    static final class BruteForce extends Passive {
        BruteForce() {
            super("Brute Force");
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            stats.set(Stat.SPEED, Math.min(stats.get(Stat.SPEED), PlayerAttributes.speedCap(player)) / 2);
        }

        @Override
        public Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
            String plain = text(active);
            double additive = bruteForce(PlayerSession.of(player).stats().get(Stat.SPEED), number(HALVES, plain, 1, 20), number(HALVES, plain, 2, 25));
            return new Combat.HitBuff(additive, 1);
        }
    }

    /** The additive buff for this (halved) Speed: {@code each}% for every whole {@code per}, and 1 more. */
    static double bruteForce(double speed, double each, double per) {
        return each * Math.floor(Math.max(0, speed) / per + 1e-9) + 1;
    }

    /** They've left: their bats and contaminations go. */
    static void forget(UUID player) {
        forgetSwarm(player);
        CONTAMINATED.removeIf(c -> c.by().equals(player));
    }

    /** The plugin is going: every bat goes. */
    static void removeAll() {
        for (UUID player : List.copyOf(SWARMS.keySet())) forgetSwarm(player);
    }
}
