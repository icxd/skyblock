package net.icxd.dungeons.item.gemstone;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * An item's gemstone slots as its data keeps them: which are locked, and the gem in each. What the item data says
 * (its slots' types and unlock costs) is the SkyBlock item's; what's done to this one is its data's
 * {@code gemstone_slots}, a compound a slot in the same order (ItemBuilder makes them when the item is first
 * built): {@code locked} while its unlock cost isn't paid (a slot without one never is) with that cost
 * ({@code costs}, as it was then), and once a gem is in it, {@code gem} (the gem: "JASPER", which a Combat slot
 * needs, taking several) and {@code quality} ("FINE"). Hypixel keeps a {@code gems} compound by slot key
 * ({@code COMBAT_0: "FINE"}, {@code COMBAT_0_gem: "JASPER"}, {@code unlocked_slots}); this one keeps the plugin's
 * list, so items made before gems keep working.
 *
 * <p>A slot is open if it isn't locked or has a gem in it (Hypixel's older items had gems in slots never paid for).
 * A gem gives its stat at the item's rarity now ({@link GemstoneTable#value}), half of it on the Talisman of Power
 * line and the Shimmersparkle Chestplate (the wiki's Gemstone Slot); a gem in a chisel's slot gives a Fossil
 * Excavator perk instead (LATER: there's no Fossil Excavator), no stat.
 */
public final class GemSlots {
    public static final String KEY = "gemstone_slots";
    /** Whose gems give half (the wiki's Gemstone Slot): the Talisman of Power's line by id. */
    private static final Set<String> HALVED = Set.of("POWER_TALISMAN", "POWER_RING", "POWER_ARTIFACT", "POWER_RELIC");
    /** And the Shimmersparkle Chestplate, by name: UNKNOWN its id (the item data doesn't have it yet). */
    private static final String HALVED_NAME = "Shimmersparkle Chestplate";

    private GemSlots() {
    }

    /**
     * One of an item's slots as it is now.
     *
     * @param index  its place in the item's slots, from 0
     * @param locked not unlocked yet (never with a gem in it)
     * @param gem    the gem in it; null for none
     * @param costs  what unlocking it costs, as the item data has it now (none for a slot that starts open)
     */
    public record Slot(int index, GemstoneType type, boolean locked, Gem gem, List<Cost> costs) {
        /** Open, with no gem in it: a gem it takes can go in. */
        public boolean empty() {
            return !locked && gem == null;
        }

        /** Whether its gem is one it takes (a gem put in before the item's slots changed may not be). */
        public boolean fits() {
            return gem != null && type.accepts(gem.type());
        }

        /** "Combat Gemstone Slot", as the grinder names it (with its icon and colour there). */
        public String name() {
            return type.getName() + " Gemstone Slot";
        }
    }

    /** The item's slots, as its data has them (empty for an item without slots). */
    public static List<Slot> of(SkyBlockItem item, NBTTagCompound tag) {
        if (item.gemstoneSlots() == null) return List.of();
        List<GemstoneSlot> kinds = item.gemstoneSlots().getSlots();
        NBTTagList list = tag == null ? new NBTTagList() : tag.getList(KEY, 10);
        List<Slot> slots = new ArrayList<>(kinds.size());
        for (int i = 0; i < kinds.size(); i++) {
            GemstoneSlot kind = kinds.get(i);
            // Not in the data yet (the item wasn't built since it got slots): as ItemBuilder would make it.
            boolean has = i < list.size();
            NBTTagCompound entry = list.get(i);
            Gem gem = has ? gem(entry) : null;
            boolean locked = gem == null && (has ? entry.getBoolean("locked") : !kind.getCosts().isEmpty());
            slots.add(new Slot(i, kind.getType(), locked, gem, List.copyOf(kind.getCosts())));
        }
        return List.copyOf(slots);
    }

    private static Gem gem(NBTTagCompound entry) {
        GemstoneType type = GemstoneType.of(entry.getString("gem"));
        GemstoneQuality quality = GemstoneQuality.of(entry.getString("quality"));
        return type == null || !type.gem() || quality == null ? null : new Gem(type, quality);
    }

    /**
     * Where a gem clicked in the grinder goes: the first open, empty slot that takes it, in the item's order (the
     * recreations'; UNKNOWN which Hypixel fills when several could); -1 for none.
     */
    public static int target(List<Slot> slots, GemstoneType gem) {
        for (Slot slot : slots) if (slot.empty() && slot.type().accepts(gem)) return slot.index();
        return -1;
    }

    // Changing the data

    /** Unlocks a slot (its cost paid), as /unlock always did: no longer locked, and its stored cost gone. */
    public static void unlock(NBTTagCompound tag, SkyBlockItem item, int index) {
        NBTTagCompound entry = entry(tag, item, index);
        entry.setBoolean("locked", false);
        entry.remove("costs");
    }

    /** Puts a gem in a slot (open by then). */
    public static void apply(NBTTagCompound tag, SkyBlockItem item, int index, Gem gem) {
        NBTTagCompound entry = entry(tag, item, index);
        entry.setBoolean("locked", false);
        entry.remove("costs");
        entry.setString("gem", gem.type().name());
        entry.setString("quality", gem.quality().name());
    }

    /** Takes a slot's gem out; the slot stays open (the wiki: removing a gem never locks its slot again). */
    public static void remove(NBTTagCompound tag, SkyBlockItem item, int index) {
        NBTTagCompound entry = entry(tag, item, index);
        entry.remove("gem");
        entry.remove("quality");
        entry.setBoolean("locked", false);
    }

    /** A slot's compound in the data, the list made up to it first as ItemBuilder makes it (so an older item's too). */
    private static NBTTagCompound entry(NBTTagCompound tag, SkyBlockItem item, int index) {
        List<GemstoneSlot> kinds = item.gemstoneSlots() == null ? List.of() : item.gemstoneSlots().getSlots();
        if (index < 0 || index >= kinds.size()) throw new IllegalArgumentException(item.id() + " has no gemstone slot " + index);
        NBTTagList list = tag.getList(KEY, 10);
        while (list.size() <= index) {
            NBTTagCompound slot = new NBTTagCompound();
            slot.setBoolean("locked", !kinds.get(list.size()).getCosts().isEmpty());
            list.add(slot);
        }
        tag.set(KEY, list);
        return list.get(index);
    }

    // What shows

    /**
     * A slot in the "Gemstones:" line, with its type's icon (⚔ for a Combat slot whatever its gem): locked dark
     * gray, "&8[✎]" (Hypixel's "&8[&8✎&8]" looks the same; the plugin's as it always was); open and empty with a
     * gray icon, "&8[&7✎&8]"; with a gem, the icon in the gem's colour between brackets in the quality's,
     * "&9[&d⚔&9]" for a Fine Jasper (the recordings, live items).
     */
    public static String glyph(Slot slot) {
        char icon = slot.type().getIcon();
        if (slot.gem() != null) {
            String quality = "&" + slot.gem().quality().getColor();
            return quality + "[&" + slot.gem().type().getColor() + icon + quality + "]";
        }
        return slot.locked() ? "&8[" + icon + "]" : "&8[&7" + icon + "&8]";
    }

    /** Every slot's, a space between. */
    public static String glyphs(List<Slot> slots) {
        List<String> glyphs = new ArrayList<>(slots.size());
        for (Slot slot : slots) glyphs.add(glyph(slot));
        return String.join(" ", glyphs);
    }

    /** "&7Gemstones: &8[✎] &9[&d⚔&9]", the item's line; null for an item without slots. */
    public static String line(SkyBlockItem item, NBTTagCompound tag) {
        List<Slot> slots = of(item, tag);
        return slots.isEmpty() ? null : "&7Gemstones: " + glyphs(slots);
    }

    // Stats

    /** What the item's gems give (see the class's doc); nothing until the table is read. */
    public static Stats stats(SkyBlockItem item, NBTTagCompound tag) {
        return stats(GemstoneTable.get(), item, tag);
    }

    public static Stats stats(GemstoneTable table, SkyBlockItem item, NBTTagCompound tag) {
        Stats stats = new Stats();
        if (table == null || item.gemstoneSlots() == null) return stats;
        Rarity rarity = ItemBuilder.rarity(item, tag);
        double share = halved(item) ? 0.5 : 1;
        for (Slot slot : of(item, tag)) {
            // UNKNOWN: a gem its slot no longer takes gives nothing (Hypixel's "unused Gemstones").
            if (!slot.fits() || slot.type() == GemstoneType.CHISEL) continue;
            Stat stat = table.stat(slot.gem().type());
            if (stat != null) stats.add(stat, table.value(slot.gem(), rarity) * share);
        }
        return stats;
    }

    /** Whether its gems give half. */
    public static boolean halved(SkyBlockItem item) {
        return HALVED.contains(item.id()) || HALVED_NAME.equals(item.name());
    }
}
