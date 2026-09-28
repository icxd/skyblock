package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;

/**
 * The Jerry-chine Gun's Rapid-fire: "Shoots a Jerry bullet, dealing 500 damage on impact and knocking you
 * back. Each shot costs +30 mana more than the previous, resetting after 4s of not firing." "It has a
 * maximum fire rate of 5 per second and it has a 3° angle of spread. The bullets are destroyed on impact,
 * dealing 500+ to enemies which are directly hit. If the user is within 2.5 blocks of the impact, they are
 * knocked upwards 2.5 blocks and away in the flat plane up to 2.5 blocks ... The ability costs 30 mana to
 * use, and increases by 30 mana each shot" (the wiki); base 500 with Intelligence scaling 0.2. Its block
 * has no mana cost, so the growing one is taken here. The bullet's look and speed are UNKNOWN (a trail, 2
 * blocks a tick, 30 blocks), it stops at walls ("can shoot through 1-block thick walls": not yet), and
 * one bullet's hit gets no chat line (UNKNOWN).
 */
final class JerryGun implements AbilityHandler {
    static final Magic.Spell BULLET = new Magic.Spell(500, 0.2);
    static final int COST_STEP = 30;
    static final long RESET_MILLIS = 4_000;
    static final long EVERY_MILLIS = 200;
    private static final double SPREAD = 3;
    private static final double SPEED = 2;
    private static final double RANGE = 30;
    private static final double KNOCKED_WITHIN = 2.5;

    /** Each player's shots in a row, and when the last was. */
    private record Streak(int shots, long last) {
    }

    private static final Map<UUID, Streak> STREAKS = new HashMap<>();

    /** What the next shot costs after {@code shots} in a row: 30, 60, 90 ... */
    static int cost(int shots) {
        return COST_STEP * (Math.max(0, shots) + 1);
    }

    /** How many shots in a row there have been, as of {@code now}: none once 4 seconds went by without one. */
    static int shotsInARow(int shots, long last, long now) {
        return now - last >= RESET_MILLIS ? 0 : shots;
    }

    static void forget(UUID player) {
        STREAKS.remove(player);
    }

    private static int shots(Player player, long now) {
        Streak streak = STREAKS.get(player.getUniqueId());
        return streak == null ? 0 : shotsInARow(streak.shots(), streak.last(), now);
    }

    @Override
    public boolean casts(Player player, SkyBlockItem item, NBTTagCompound tag) {
        return PlayerSession.of(player).cooldownLeft("jerry_gun") <= 0;
    }

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        return Hits.enoughMana(player, cost(shots(player, System.currentTimeMillis())));
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        long now = System.currentTimeMillis();
        int shots = shots(player, now);
        Hits.takeMana(player, cost(shots), block.name());
        STREAKS.put(player.getUniqueId(), new Streak(shots + 1, now));
        PlayerSession.of(player).startCooldown("jerry_gun", EVERY_MILLIS);
        Magic.Spell spell = Hits.spellOf(item, BULLET);
        Location eye = player.getEyeLocation();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        eye.setYaw((float) (eye.getYaw() + random.nextDouble(-SPREAD / 2, SPREAD / 2)));
        eye.setPitch((float) (eye.getPitch() + random.nextDouble(-SPREAD / 2, SPREAD / 2)));
        new Missile(player, eye, eye.getDirection().multiply(SPEED))
                .range(RANGE)
                .trail(at -> at.getWorld().spawnParticle(Particle.CRIT, at, 1, 0, 0, 0, 0))
                .onHit((missile, mob) -> {
                    double damage = Hits.magic(missile.caster(), item, tag, spell, mob) * Hits.takenFactor(mob);
                    Hits.hurt(missile.caster(), mob, damage, DamageIndicators.Look.NORMAL, tag);
                    return false;
                })
                .onEnd((missile, at, impact) -> {
                    if (impact) knockBack(missile.caster(), at);
                })
                .launch();
        player.getWorld().playSound(eye, Sound.ENTITY_VILLAGER_NO, 0.5f, 1.6f);
    }

    /** Close enough to where it hit, they're thrown up and away from it. */
    private static void knockBack(Player player, Location at) {
        if (!player.getWorld().equals(at.getWorld()) || !Shapes.inBall(at.toVector(), KNOCKED_WITHIN, player.getBoundingBox())) return;
        Vector away = player.getLocation().toVector().subtract(at.toVector()).setY(0);
        if (away.lengthSquared() > 1e-6) away.normalize().multiply(0.6);
        player.setVelocity(away.setY(0.75));
    }
}
