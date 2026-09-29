package net.icxd.dungeons.item.ability;

import java.util.ArrayList;
import java.util.List;

import net.icxd.dungeons.item.ability.utility.WornPassives;
import net.icxd.dungeons.item.ability.weapons.WornStrikes;
import net.icxd.dungeons.item.bonus.Bonus;

/**
 * Worn armor's and equipment's own passive ABILITY blocks (Block Damage, Growth, Rejuvenate, Brute Force and the
 * rest), as {@link Bonus}es of the kind "ABILITY" that SetBonuses counts by their block's name, as it does a piece's
 * own bonus: the utility ones (ABILITIES_UTILITY.md) and the ones that hit (ABILITIES_WEAPONS.md).
 */
public final class WornAbilities {
    private WornAbilities() {
    }

    public static List<Bonus> all() {
        List<Bonus> all = new ArrayList<>(WornPassives.all());
        all.addAll(WornStrikes.all());
        return all;
    }
}
