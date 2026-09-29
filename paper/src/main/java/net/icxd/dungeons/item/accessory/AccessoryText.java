package net.icxd.dungeons.item.accessory;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.stats.Stats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * The numbers in an accessory's own text, by its id ("Gain a stack of &c2❁ Strength &7for every mob killed in the
 * last &a5 seconds"): what an accessory does follows what it says, read with {@link AbilityText} once and kept
 * (the text is the item data's, which doesn't change while it's loaded). An accessory the data doesn't have reads
 * as {@code otherwise}. Main thread.
 */
final class AccessoryText {
    private static final Map<String, Map<String, OptionalDouble>> READ = new HashMap<>();
    private static final Map<String, Stats> STATS = new HashMap<>();
    private static final Map<String, String> PLAIN = new HashMap<>();

    private AccessoryText() {
    }

    /** Its text as one line, without colour codes; "" for an accessory the data doesn't have. */
    static String plain(String id) {
        return PLAIN.computeIfAbsent(id, i -> {
            SkyBlockItem item = ItemRegistry.get(i);
            return item == null ? "" : AbilityText.plain(item.lore());
        });
    }

    /** The first number after {@code before} in its text; {@code otherwise} if it isn't there. */
    static double after(String id, String before, double otherwise) {
        return READ.computeIfAbsent(id, i -> new HashMap<>()).computeIfAbsent(before, b -> AbilityText.after(plain(id), b)).orElse(otherwise);
    }

    /** Every "+2❣ Health Regen" in its text (read it, don't change it). */
    static Stats stats(String id) {
        return STATS.computeIfAbsent(id, i -> AbilityText.stats(plain(i)));
    }

    /** Forgets what's been read (the item data was read again). */
    static void clear() {
        READ.clear();
        STATS.clear();
        PLAIN.clear();
    }

    /** The lines as they are, for tests: their numbers read as {@link #after} would. */
    static double after(List<String> lore, String before, double otherwise) {
        return AbilityText.after(AbilityText.plain(lore), before).orElse(otherwise);
    }
}
