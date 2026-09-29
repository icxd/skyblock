package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.stats.Stats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * The numbers in a bonus's own text, as the item's data has it ("Attacking a mob has a &c50%&7 chance"): what a
 * bonus does follows what its pieces say, read with {@link AbilityText}. Each text's numbers are read once and
 * kept (a hit asks again and again), by the text and what the number follows; the cache is let go of past a
 * few thousand, which no item data comes near. Main thread.
 */
final class BonusText {
    private static final int MOST = 4096;
    private static final Map<List<String>, Map<String, OptionalDouble>> READ = new HashMap<>();
    private static final Map<List<String>, Stats> STATS = new HashMap<>();

    private BonusText() {
    }

    /** The first number after {@code before} in the text (see {@link AbilityText#after}); {@code otherwise} if it isn't there. */
    static double after(List<String> text, String before, double otherwise) {
        return read(text, before).orElse(otherwise);
    }

    /** The same in a block's text; {@code otherwise} for no block. */
    static double after(ItemBlock block, String before, double otherwise) {
        return block == null ? otherwise : after(block.text(), before, otherwise);
    }

    /** How long its text says, in milliseconds ("for 5s"); {@code otherwise} if it doesn't. */
    static double millis(ItemBlock block, double otherwise) {
        return block == null ? otherwise : read(block.text(), "\0millis").orElse(otherwise);
    }

    /** How far its text says, in blocks ("within 30 blocks"); {@code otherwise} if it doesn't. */
    static double blocks(ItemBlock block, double otherwise) {
        return block == null ? otherwise : read(block.text(), "\0blocks").orElse(otherwise);
    }

    private static OptionalDouble read(List<String> text, String key) {
        if (READ.size() > MOST) READ.clear();
        return READ.computeIfAbsent(text, t -> new HashMap<>()).computeIfAbsent(key, k -> {
            String plain = AbilityText.plain(text);
            return switch (k) {
                case "\0millis" -> AbilityText.millis(plain);
                case "\0blocks" -> AbilityText.blocks(plain);
                default -> AbilityText.after(plain, k);
            };
        });
    }

    /** Every "+30❁ Strength" in the text (see {@link AbilityText#stats}), kept: read it, don't change it. */
    static Stats stats(List<String> text) {
        if (STATS.size() > MOST) STATS.clear();
        return STATS.computeIfAbsent(text, t -> AbilityText.stats(AbilityText.plain(t)));
    }

    /** The block this bonus's pieces carry: the first worn piece's with its kind and name; null if none has one. */
    static ItemBlock block(Bonus.Active active) {
        for (Worn.Piece piece : active.pieces()) {
            for (ItemBlock block : piece.blocks()) {
                if (active.bonus().name().equals(block.name()) && active.bonus().kind().equals(block.kind())) return block;
            }
        }
        return null;
    }

    /** A piece's block of this kind and name; null if it has none. */
    static ItemBlock block(Worn.Piece piece, String kind, String name) {
        for (ItemBlock block : piece.blocks()) if (name.equals(block.name()) && kind.equals(block.kind())) return block;
        return null;
    }
}
