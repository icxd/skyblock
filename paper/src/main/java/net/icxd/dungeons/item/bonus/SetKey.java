package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.item.data.ItemBlock;

/**
 * Which set a FULL_SET or TIERED block belongs to: its kind, its bonus's name and how many pieces its
 * header counts to ("(0/4)", 0 for a header without a count). Items whose blocks have the same key are
 * one set, so Hot and Infernal Crimson Armor count together (the wiki's Kuudra sets use "the lowest
 * tier" worn), and Nutcracker Armor's Cold Thumb (of 4) is apart from the Snow Suit's (of 8).
 */
public record SetKey(String kind, String name, int pieces) {
    public static final String FULL_SET = "FULL_SET";
    public static final String TIERED = "TIERED";

    /** The set the block is a bonus of; null if it isn't a set's (an ability, a piece's own bonus). */
    public static SetKey of(ItemBlock block) {
        if (block == null || block.name() == null || !(FULL_SET.equals(block.kind()) || TIERED.equals(block.kind()))) return null;
        return new SetKey(block.kind(), block.name(), block.pieces());
    }

    public boolean tiered() {
        return TIERED.equals(kind);
    }
}
