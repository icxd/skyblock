package net.icxd.dungeons.item.ability.utility;

import java.util.function.BiConsumer;

import net.icxd.dungeons.item.ability.AbilityHandler;

/** Abilities that don't hit: movement, healing, summons, tools and the rest, by ability name. */
public final class UtilityAbilities {
    private UtilityAbilities() {
    }

    /** Hands each of them to {@code to}, by ability name as items' ABILITY blocks have it. */
    public static void register(BiConsumer<String, AbilityHandler> to) {
    }
}
