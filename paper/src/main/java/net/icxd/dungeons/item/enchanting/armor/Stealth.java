package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.goals.FleeGoal;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stealth, on boots: "Timid mobs have a 60% chance to remain still instead of fleeing". A Timid mob that would
 * run from them (the Scared Skeleton, see {@link FleeGoal}) rolls it once each time they come near; if it doesn't
 * notice them, it stays where it is, and they're told with the wiki's message. UNKNOWN: when Hypixel rolls it,
 * and when it sends the message (the wiki's "InfoNeeded"): here, each time the roll goes their way. Main thread.
 */
final class Stealth {
    static final String ID = "stealth";

    private Stealth() {
    }

    static void register() {
        FleeGoal.setUnnoticed(Stealth::unnoticed);
    }

    private static boolean unnoticed(Mob mob, Player player) {
        Mobs.Live live = Mobs.of(mob);
        if (live == null || !live.type().getTypes().contains(MobType.TIMID)) return false;
        WornEnchants.Piece boots = WornEnchants.ofType(WornEnchants.of(player), SpecificItemType.BOOTS);
        int level = boots == null ? 0 : boots.level(ID);
        if (level <= 0 || !stays(EnchantNumbers.get(ID, level, 0), ThreadLocalRandom.current().nextDouble())) return false;
        player.sendMessage(Utils.color(message(live.type().getName())));
        return true;
    }

    /** Whether the mob stays still: {@code roll} (0 to 1) under {@code chance}%. */
    static boolean stays(double chance, double roll) {
        return roll * 100 < chance;
    }

    /** "&7&oThe Scared Skeleton didn't notice your presence thanks to your Stealth enchantment!" (the wiki's Stealth). */
    static String message(String mob) {
        return "&7&oThe " + mob + " didn't notice your presence thanks to your Stealth enchantment!";
    }
}
