package net.icxd.dungeons.item.ability.weapons;

import java.util.function.BiConsumer;

import net.icxd.dungeons.item.ability.AbilityHandler;

/** Weapons' abilities: the ones that hit (swords, bows, wands and staves), by ability name. */
public final class WeaponAbilities {
    private WeaponAbilities() {
    }

    /** Hands each of them to {@code to}, by ability name as items' ABILITY blocks have it. */
    public static void register(BiConsumer<String, AbilityHandler> to) {
    }
}
