package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * What a player wears that bonuses count: their armor, and their equipment where there is some (see
 * {@link SetBonuses#setEquipment}), each piece with its blocks as its behaviour has them. A set is the
 * pieces with a block of the same {@link SetKey}; a piece counts once in each set it's in (Nutcracker
 * Armor is in two), however many of that set's blocks it has. No Bukkit in the counting.
 */
public final class Worn {
    /** A worn piece: its item, its data, its blocks, and its stack (null where there's none, in tests). */
    public record Piece(SkyBlockItem item, NBTTagCompound tag, List<ItemBlock> blocks, ItemStack stack) {
        public Piece {
            blocks = List.copyOf(blocks);
        }

        public String id() {
            return item.id();
        }

        /** Whether it has a block of this kind with this name. */
        public boolean has(String kind, String name) {
            for (ItemBlock block : blocks) if (kind.equals(block.kind()) && name.equals(block.name())) return true;
            return false;
        }

        /** Its block of this set; null if it isn't in it. */
        public ItemBlock block(SetKey set) {
            for (ItemBlock block : blocks) if (set.equals(SetKey.of(block))) return block;
            return null;
        }
    }

    public static final Worn NOTHING = new Worn(List.of());

    private final List<Piece> pieces;
    private final Map<SetKey, Integer> counts = new LinkedHashMap<>();

    public Worn(List<Piece> pieces) {
        this.pieces = List.copyOf(pieces);
        for (Piece piece : this.pieces) {
            Set<SetKey> sets = new HashSet<>();
            for (ItemBlock block : piece.blocks()) {
                SetKey set = SetKey.of(block);
                if (set != null && sets.add(set)) counts.merge(set, 1, Integer::sum);
            }
        }
    }

    public List<Piece> pieces() {
        return pieces;
    }

    /** How many worn pieces are in this set. */
    public int count(SetKey set) {
        return set == null ? 0 : counts.getOrDefault(set, 0);
    }

    /** The sets they wear pieces of, in the order they were first worn (helmet first). */
    public Set<SetKey> sets() {
        return counts.keySet();
    }

    /** The worn pieces in this set, in slot order. */
    public List<Piece> in(SetKey set) {
        List<Piece> in = new ArrayList<>();
        for (Piece piece : pieces) if (piece.block(set) != null) in.add(piece);
        return in;
    }

    /** The worn pieces with a block of this kind and name (a piece's own bonus). */
    public List<Piece> with(String kind, String name) {
        List<Piece> with = new ArrayList<>();
        for (Piece piece : pieces) if (piece.has(kind, name)) with.add(piece);
        return with;
    }

    /** The worn pieces whose item id passes. */
    public List<Piece> matching(Predicate<String> id) {
        List<Piece> matching = new ArrayList<>();
        for (Piece piece : pieces) if (id.test(piece.id())) matching.add(piece);
        return matching;
    }
}
