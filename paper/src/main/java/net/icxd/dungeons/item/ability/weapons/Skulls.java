package net.icxd.dungeons.item.ability.weapons;

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

/**
 * The Crypt swords' wither skulls. Dreadlord: "Shoot a skull that deals 504 damage", "exploding on impact
 * for 500 damage to all mobs within 3 blocks" (the wiki), base 500 with Intelligence scaling 0.3.
 * Witherlord: "Shoot 3 skulls each dealing 304.5 Damage", base 300 (its data), the rest as Dreadlord's
 * (UNKNOWN: its page doesn't exist; the three go out 5° apart, as a Terminator's arrows). How fast and
 * how far a skull flies is UNKNOWN (a block a tick, 32 blocks).
 */
final class Skulls implements AbilityHandler {
    static final Magic.Spell DREADLORD = new Magic.Spell(500, 0.3);
    static final double RADIUS = 3;
    private static final double SPEED = 1;
    private static final float SPREAD = 5;

    private final String name;
    private final int skulls;

    Skulls(String name, int skulls) {
        this.name = name;
        this.skulls = skulls;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Magic.Spell spell = Hits.spellOf(item, DREADLORD);
        Location eye = player.getEyeLocation();
        for (int i = 0; i < skulls; i++) {
            Location from = eye.clone();
            from.setYaw(eye.getYaw() + (i - (skulls - 1) / 2f) * SPREAD);
            Vector direction = from.getDirection();
            new Missile(player, eye, direction.multiply(SPEED))
                    .look(Missile.display(eye, new ItemStack(Material.WITHER_SKELETON_SKULL), 0.6f, 0))
                    .trail(at -> at.getWorld().spawnParticle(Particle.SMOKE, at, 1, 0, 0, 0, 0))
                    .onEnd((missile, at, impact) -> {
                        if (impact) explode(missile.caster(), item, tag, spell, at);
                    })
                    .launch();
        }
        player.getWorld().playSound(eye, Sound.ENTITY_WITHER_SHOOT, 0.6f, 1);
    }

    /** Its chat line is UNKNOWN: in the form the others have, by the ability's name. */
    private void explode(Player caster, SkyBlockItem item, NBTTagCompound tag, Magic.Spell spell, Location at) {
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 1);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);
        Hits.report(caster, name, Hits.spell(caster, item, tag, spell, Hits.near(at, RADIUS)));
    }
}
