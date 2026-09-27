package net.icxd.dungeons.item.ability.weapons;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Player;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * The Spirit Sceptre's Guided Bat: "Shoots a guided spirit bat, following your aim and exploding for 2,000
 * damage", "guided by the wielder's crosshair. Upon hitting a mob or block, the bat creates an explosion
 * with a radius of 6 blocks" (the wiki), base 2,000 (2,250 fragged: its data) with Intelligence scaling
 * 0.2. Each tick it heads for what their crosshair is on. The chat line is the mods' "Your Spirit Sceptre
 * hit 3 enemies for 141,760.1 damage." How fast and how far it flies is UNKNOWN (0.8 blocks a tick, 40 blocks).
 */
final class GuidedBat implements AbilityHandler {
    static final Magic.Spell BAT = new Magic.Spell(2_000, 0.2);
    static final double RADIUS = 6;
    private static final double SPEED = 0.8;
    private static final double RANGE = 40;
    /** How far along their aim it looks for what they're aiming at. */
    private static final double AIM = 48;

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Magic.Spell spell = Hits.spellOf(item, BAT);
        Location eye = player.getEyeLocation();
        Bat bat = eye.getWorld().spawn(eye, Bat.class, b -> {
            b.setPersistent(false);
            b.setAI(false);
            b.setInvulnerable(true);
            b.setSilent(true);
            b.setGravity(false);
            b.setCollidable(false);
        });
        new Missile(player, eye, eye.getDirection().multiply(SPEED))
                .range(RANGE)
                .look(bat)
                .steer(missile -> Hits.aimed(missile.caster(), AIM).toVector().subtract(missile.at().toVector()))
                .onEnd((missile, at, impact) -> {
                    if (impact) explode(missile.caster(), item, tag, spell, at);
                })
                .launch();
        player.getWorld().playSound(eye, Sound.ENTITY_BAT_TAKEOFF, 1, 1);
    }

    private static void explode(Player caster, SkyBlockItem item, NBTTagCompound tag, Magic.Spell spell, Location at) {
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 3, 1, 1, 1, 0);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
        Hits.report(caster, "Spirit Sceptre", Hits.spell(caster, item, tag, spell, Hits.near(at, RADIUS)));
    }
}
