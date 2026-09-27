package net.icxd.dungeons.item.ability.weapons;

import java.util.function.BiConsumer;

import org.bukkit.entity.AbstractArrow;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;

/**
 * Weapons' abilities: the ones that hit (swords, bows, wands and staves), by ability name. What each
 * does, and where Hypixel's is only approximated, is in ABILITIES_WEAPONS.md.
 */
public final class WeaponAbilities {
    private WeaponAbilities() {
    }

    /** Hands each of them to {@code to}, by ability name as items' ABILITY blocks have it. */
    public static void register(BiConsumer<String, AbilityHandler> to) {
        // Necron's Blade and its swords (the scrolls' abilities, all three together Wither Impact).
        to.accept("Implosion", new WitherBlade.Implosion());
        to.accept("Wither Shield", new WitherBlade.WitherShield());
        to.accept("Shadow Warp", new WitherBlade.ShadowWarp());
        to.accept("Wither Impact", new WitherBlade.WitherImpact());
        to.accept("Giant's Slam", new GiantsSlam());
        to.accept(Salvation.NAME, new Salvation());
        // Spells: magic damage from Intelligence.
        to.accept("Guided Bat", new GuidedBat());
        to.accept("Showtime", new Showtime());
        to.accept("Ice Spray", new Cones.IceSpray());
        to.accept("Dragon Rage", new Cones.DragonRage());
        to.accept("Molten Wave", new MoltenWave());
        to.accept("Terrain Toss", new TerrainToss());
        to.accept("Rapid-fire", new JerryGun());
        to.accept("Dreadlord", new Skulls("Dreadlord", 1));
        to.accept("Witherlord", new Skulls("Witherlord", 3));
        // Hits worked out as the weapon's melee hit or arrow.
        to.accept("Throw", new ThrownBlade());
        to.accept("Shadow Fury", new ShadowFury());
        to.accept("Heat-Seeking Rose", new Roses(1, 3, true));
        to.accept("Petal Barrage", new Roses(3, 5, false));
        to.accept("Swing", new Bonemerang());
        to.accept("Flay", new Flay());
        to.accept("Reaving Strike", new ReavingStrike());
        to.accept("Rapid Fire", new MachineGun());
        to.accept("Ragnarock", new Ragnarock());
    }

    /** A shortbow shot this arrow: what the bow's abilities need to know of it (Salvation counts its hits). */
    public static void shortbowArrow(AbstractArrow arrow, SkyBlockItem bow) {
        Salvation.shot(arrow, bow);
    }
}
