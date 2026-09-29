package net.icxd.dungeons.item.ability.weapons;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.bonus.Worn;
import net.icxd.dungeons.item.data.ItemBlock;

/**
 * The Implosion Belt's Consolidated: "Increases all explosion damage dealt by 25%.", a multiplicative multiplier
 * (the wiki's Damage Calculation/Multiplicative Sources: 1.25), while the belt is worn (it's equipment, which
 * {@link SetBonuses#worn} counts). Which damage is "explosion" damage is UNKNOWN: here what its text calls one
 * (an explosion, exploding, an implosion): Implosion (and Wither Impact's and Shadow Warp's), the Spirit Sceptre's
 * bat, Bonzo's Staff's balloons, the Staff of the Volcano's Explode, the Glacial Scythe's explosion, Ray of Hope, the
 * Crypt swords' and the Crypt Bow's skulls, the Explosive Bow's, Detonate, Contaminate, the Witch Masks' bats and
 * the Dungeon Trap. Public for the utility abilities that explode.
 */
public final class Explosions {
    public static final String CONSOLIDATED = "Consolidated";
    private static final Pattern MORE = Pattern.compile("Increases all explosion damage dealt by ([\\d.]+)%");

    private Explosions() {
    }

    /** The factor on the explosions they deal: 1.25 with an Implosion Belt on (its text's number), else 1. */
    public static double factor(Player player) {
        double factor = 1;
        for (Worn.Piece piece : SetBonuses.worn(player).with("ABILITY", CONSOLIDATED)) {
            for (ItemBlock block : piece.blocks()) {
                if (!CONSOLIDATED.equals(block.name())) continue;
                factor = Math.max(factor, factor(AbilityText.plain(block.text())));
            }
        }
        return factor;
    }

    /** "Increases all explosion damage dealt by 25%": 1.25 (1 if it doesn't say). */
    static double factor(String plain) {
        Matcher m = MORE.matcher(plain);
        return m.find() ? 1 + Double.parseDouble(m.group(1)) / 100 : 1;
    }
}
