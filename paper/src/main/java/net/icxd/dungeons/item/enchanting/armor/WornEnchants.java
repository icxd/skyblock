package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.bonus.Worn;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.reforge.Reforge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What a player wears, as armor and equipment enchantments and reforge bonuses count it: the pieces {@link
 * SetBonuses#worn} has (their armor, helmet first, then their equipment; armor held in the hand never counts),
 * each with its enchantments (by the plugin's id: "legion", not "ultimate_legion") and its reforge. Each
 * enchantment on each piece is its own: what they do adds up piece by piece, each piece's own cap for itself
 * (the wiki's Refrigerate: "up to 150 Defense per armor piece, up to a maximum of 600 (4 armor pieces)"; the
 * wiki's Last Stand: it "will stack across multiple armor pieces"). Worked out at most once a tick. Main thread.
 */
public final class WornEnchants {
    /**
     * A worn piece: where it is among them ({@code index}), the piece, its enchantments, and its reforge (null for
     * none) at the piece's rarity.
     */
    public record Piece(int index, Worn.Piece worn, Map<String, Integer> enchantments, Reforge reforge, Rarity rarity) {
        /** Its level of this enchantment; 0 without it. */
        public int level(String id) {
            Integer level = enchantments.get(id);
            return level == null ? 0 : level;
        }

        /** Whether its reforge is this one (Hypixel's modifier id: "renowned"). */
        public boolean reforged(String id) {
            return reforge != null && reforge.id().equals(id);
        }

        public SpecificItemType type() {
            return worn.item().specificItemType();
        }
    }

    private record Now(int tick, List<Piece> pieces) {
    }

    private static final Map<UUID, Now> NOW = new HashMap<>();

    private WornEnchants() {
    }

    /** Their worn pieces now. */
    public static List<Piece> of(Player player) {
        int tick = Bukkit.getCurrentTick();
        Now now = NOW.get(player.getUniqueId());
        if (now != null && now.tick() == tick) return now.pieces();
        List<Piece> pieces = pieces(SetBonuses.worn(player));
        if (player.isOnline()) NOW.put(player.getUniqueId(), new Now(tick, pieces));
        return pieces;
    }

    /** These worn pieces, with their enchantments and reforges. */
    public static List<Piece> pieces(Worn worn) {
        List<Worn.Piece> worns = worn.pieces();
        if (worns.isEmpty()) return List.of();
        List<Piece> pieces = new ArrayList<>(worns.size());
        for (int i = 0; i < worns.size(); i++) {
            Worn.Piece piece = worns.get(i);
            NBTTagCompound tag = piece.tag();
            pieces.add(new Piece(i, piece, Combat.enchantments(tag), Reforge.of(tag), ItemBuilder.rarity(piece.item(), tag)));
        }
        return List.copyOf(pieces);
    }

    /** Their levels of an enchantment on all their pieces, added up; 0 without it. */
    public static int total(List<Piece> pieces, String id) {
        int total = 0;
        for (Piece piece : pieces) total += piece.level(id);
        return total;
    }

    /** The piece of this type they wear (their helmet, boots, necklace...); null for none. */
    public static Piece ofType(List<Piece> pieces, SpecificItemType type) {
        for (Piece piece : pieces) if (piece.type() == type) return piece;
        return null;
    }

    /** They've left. */
    static void forget(UUID player) {
        NOW.remove(player);
    }
}
