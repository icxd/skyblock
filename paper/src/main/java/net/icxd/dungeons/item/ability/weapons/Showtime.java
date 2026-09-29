package net.icxd.dungeons.item.ability.weapons;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;

/**
 * Bonzo's Staff: "Shoots balloons that create a large explosion on impact, dealing up to 1,000 damage":
 * "slow-moving balloons at a rate of 4/s. These balloons explode on contact with blocks and mobs, which
 * deals 1,000 damage to all mobs caught within 2 blocks of the explosion ... If the wielder is within range
 * of the explosion, they will be launched upwards and backwards up to 1.7 blocks upwards and 8 blocks
 * backwards" (the wiki); base 1,000 (1,100 fragged) with Intelligence scaling 0.2. At most 4 a second: a
 * click sooner does nothing. The balloon's look, speed and range are UNKNOWN (a coloured wool block,
 * 0.4 blocks a tick, 30 blocks), and so is its chat line (Showtime's, in the others' form).
 */
final class Showtime implements AbilityHandler {
    static final Magic.Spell BALLOON = new Magic.Spell(1_000, 0.2);
    static final double RADIUS = 2;
    static final long EVERY_MILLIS = 250;
    private static final double SPEED = 0.4;
    private static final double RANGE = 30;
    /** The launch: back and up (UNKNOWN how Hypixel's velocity is set; this carries about 8 blocks and 1.7 up). */
    private static final double BACK = 1.6;
    private static final double UP = 0.55;
    private static final List<Material> COLOURS = List.of(Material.RED_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
            Material.LIGHT_BLUE_WOOL, Material.MAGENTA_WOOL);

    @Override
    public boolean casts(Player player, SkyBlockItem item, NBTTagCompound tag) {
        return PlayerSession.of(player).cooldownLeft("showtime") <= 0;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        PlayerSession.of(player).startCooldown("showtime", EVERY_MILLIS);
        Magic.Spell spell = Hits.spellOf(item, BALLOON);
        Location eye = player.getEyeLocation();
        Material colour = COLOURS.get(ThreadLocalRandom.current().nextInt(COLOURS.size()));
        new Missile(player, eye, eye.getDirection().multiply(SPEED))
                .range(RANGE)
                .look(Missile.display(eye, new ItemStack(colour), 0.5f, 0))
                .onEnd((missile, at, impact) -> {
                    if (impact) explode(missile.caster(), item, tag, spell, at);
                })
                .launch();
        player.getWorld().playSound(eye, Sound.ENTITY_CHICKEN_EGG, 1, 1.4f);
    }

    private static void explode(Player caster, SkyBlockItem item, NBTTagCompound tag, Magic.Spell spell, Location at) {
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 2, 0.5, 0.5, 0.5, 0);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.6f);
        Hits.report(caster, "Showtime", Hits.spell(caster, item, tag, spell.times(Explosions.factor(caster)), Hits.near(at, RADIUS)));
        if (caster.getWorld().equals(at.getWorld()) && Shapes.inBall(at.toVector(), RADIUS, caster.getBoundingBox())) {
            Vector back = caster.getLocation().getDirection().setY(0);
            if (back.lengthSquared() > 0) back.normalize().multiply(-BACK);
            caster.setVelocity(back.setY(UP));
        }
    }
}
