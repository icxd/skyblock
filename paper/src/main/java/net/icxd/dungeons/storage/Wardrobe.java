package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Swapping what a player wears for a stored set, the way Hypixel's Wardrobe does it (the wiki's
 * Wardrobe: "The player cannot modify their current set"), for the four armor pieces and the four
 * equipment pieces alike. At most one set is worn: its pieces are on the player and its column shows
 * them, and it's stored empty. Putting on another set puts what they wear back in the set it came from
 * (as it is now: a piece taken off, or another put on, goes with it), or, when it came from none, into
 * their inventory; then the new set's pieces go on, and it's the worn one. Nothing is made or lost: the
 * pieces only move. No server in it: {@code T} is an item.
 */
final class Wardrobe {
    private Wardrobe() {
    }

    /**
     * What putting on a set does.
     *
     * @param worn        the set that's worn now, afterwards
     * @param wearing     what they wear afterwards
     * @param stored      the stored pieces of the set that was worn before, afterwards (what they wore); null if
     *                    none was worn or it's the same set
     * @param toInventory what goes into their inventory (what they wore, when it came from no set)
     */
    record Swap<T>(int worn, List<T> wearing, List<T> stored, List<T> toInventory) {
    }

    /**
     * Putting on set {@code target} while wearing {@code wearing}, which came from set {@code worn} (-1: none).
     * {@code targetPieces} is what's stored in the target set. Wearing the target already: nothing changes.
     */
    static <T> Swap<T> swap(int worn, int target, List<T> wearing, List<T> targetPieces, Predicate<T> empty) {
        if (worn == target) return new Swap<>(worn, new ArrayList<>(wearing), null, List.of());
        List<T> toInventory = new ArrayList<>();
        List<T> stored = null;
        if (worn >= 0) {
            stored = new ArrayList<>(wearing);
        } else {
            for (T piece : wearing) if (piece != null && !empty.test(piece)) toInventory.add(piece);
        }
        return new Swap<>(target, new ArrayList<>(targetPieces), stored, toInventory);
    }
}
