package net.icxd.dungeons.item.ability.weapons;

import java.util.function.BiConsumer;

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
    }
}
