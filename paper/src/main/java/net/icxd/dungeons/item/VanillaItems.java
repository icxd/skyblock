package net.icxd.dungeons.item;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.item.nbt.ItemNBT;

/**
 * The SkyBlock item a vanilla one is, for what the world drops on an island (an oak log breaks into
 * Oak Log, {@code LOG}). Several of Hypixel's items share a material, so it's the plain one among
 * them: one that sells to NPCs, doesn't glow and has no lore of its own, then the shortest id (LOG
 * rather than ENCHANTED_OAK_LOG or the Tavern Barn Skin, WOOD rather than MIRRORED_WOOD).
 */
public final class VanillaItems {
    /** Worked out from the items the first time it's needed after they're loaded. */
    private static volatile Map<Material, String> ids;

    private VanillaItems() {
    }

    /** The plain item of this material's id; null if no item has the material. */
    public static String idOf(Material material) {
        Map<Material, String> known = ids;
        if (known == null) ids = known = build(ItemRegistry.getRegistry().values());
        return known.get(material);
    }

    /** A vanilla item as its SkyBlock item, the same number of it; as it is if it's one already, or if there's none for it. */
    public static ItemStack asSkyBlock(ItemStack vanilla) {
        if (vanilla == null || vanilla.isEmpty() || ItemNBT.read(vanilla) != null) return vanilla;
        SkyBlockItem item = ItemRegistry.get(idOf(vanilla.getType()));
        if (item == null) return vanilla;
        ItemStack stack = ItemBuilder.build(item);
        stack.setAmount(vanilla.getAmount());
        return stack;
    }

    /** The items changed (ItemRegistry reloads them). */
    static void forget() {
        ids = null;
    }

    static Map<Material, String> build(Collection<SkyBlockItem> items) {
        Comparator<SkyBlockItem> plainest = Comparator.<SkyBlockItem>comparingInt(i -> i.npcSellPrice() > 0 ? 0 : 1)
                .thenComparingInt(i -> i.glowing() ? 1 : 0)
                .thenComparingInt(i -> i.lore().isEmpty() ? 0 : 1)
                .thenComparingInt(i -> i.id().length())
                .thenComparing(SkyBlockItem::id);
        Map<Material, SkyBlockItem> best = new EnumMap<>(Material.class);
        for (SkyBlockItem item : items) {
            if (item.material() == null) continue;
            best.merge(item.material(), item, (a, b) -> plainest.compare(a, b) <= 0 ? a : b);
        }
        Map<Material, String> out = new EnumMap<>(Material.class);
        best.forEach((material, item) -> out.put(material, item.id()));
        return out;
    }
}
