package net.icxd.dungeons.mob;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Something a mob can drop: {@code chance} percent (before Magic Find, see {@link #withMagicFind}),
 * {@code min} to {@code max} of the item, or of one of several items picked at random (the wiki's
 * "Rotten Armor Piece" is one piece of the set; which one is UNKNOWN, so each is as likely). Items are
 * named by id, so mobs can be defined before the items are.
 */
public record MobDrop(List<String> itemIds, MobDropType type, double chance, int min, int max) {
    /** "Magic Find does not affect drops from most Mobs with a drop chance of 5% or above" (the wiki's Magic Find). */
    static final double MAGIC_FIND_BELOW = 5;
    /** The most Magic Find there is (the wiki's Stats, "Cap Value" 900). */
    static final double MAGIC_FIND_CAP = 900;

    public MobDrop {
        itemIds = List.copyOf(itemIds);
        if (itemIds.isEmpty()) throw new IllegalArgumentException("a drop needs an item");
    }

    public MobDrop(String itemId, MobDropType type, double chance, int min, int max) {
        this(List.of(itemId), type, chance, min, max);
    }

    public MobDrop(String itemId, MobDropType type, double chance) {
        this(itemId, type, chance, 1, 1);
    }

    /** One of these items, each as likely. */
    public static MobDrop oneOf(MobDropType type, double chance, String... itemIds) {
        return new MobDrop(List.of(itemIds), type, chance, 1, 1);
    }

    /**
     * The chance, in percent, that a drop this likely (before Magic Find) drops for a killer with this
     * Magic Find and Pet Luck: times 1 + Magic Find / 100, and for a pet times 1 + (Magic Find + Pet Luck)
     * / 100 (the wiki's Magic Find and Pet Luck), Magic Find at most 900. Other drops of 5% and more stay
     * as they are: the wiki says "most", which drops are the exceptions it doesn't say, so it's all of
     * them. Pets aren't among them: Pet Luck "increases the chance of all pets dropping from enemies" by
     * that formula (the wiki's Pet Luck), and the 5% rule is only on the Magic Find page.
     */
    public static double withMagicFind(double base, double magicFind, double petLuck, boolean pet) {
        if (!pet && base >= MAGIC_FIND_BELOW) return base;
        return base * (1 + (magicFind(magicFind) + (pet ? Math.max(0, petLuck) : 0)) / 100);
    }

    /** The Magic Find that counts: between 0 and its cap. */
    public static double magicFind(double magicFind) {
        return Math.max(0, Math.min(magicFind, MAGIC_FIND_CAP));
    }

    /** The item this drop gives (one of its items, at random); null if there's no such item. */
    public SkyBlockItem item() {
        return ItemRegistry.get(itemIds.get(ThreadLocalRandom.current().nextInt(itemIds.size())));
    }
}
