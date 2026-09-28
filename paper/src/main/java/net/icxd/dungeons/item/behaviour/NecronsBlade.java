package net.icxd.dungeons.item.behaviour;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * Necron's Blade and the swords made from it: the scrolls applied to one (flags in its data) are its
 * abilities, each as its scroll's item shows it, and all three together make Wither Impact. With none, its
 * text ends in the class ability line, as the data has it; with any, that line goes: right-click is theirs.
 */
final class NecronsBlade implements ItemBehaviour {
    static final List<String> IDS = List.of("NECRON_BLADE", "HYPERION", "ASTRAEA", "SCYLLA", "VALKYRIE");
    /** In the order their abilities are listed. */
    private static final List<Scroll> SCROLLS = List.of(new Scroll("implosion", "IMPLOSION_SCROLL"),
            new Scroll("wither_shield", "WITHER_SHIELD_SCROLL"), new Scroll("shadow_warp", "SHADOW_WARP_SCROLL"));
    private static final List<String> CLASS_ABILITY = List.of("", "&eRight-click to use your class ability!");
    /**
     * No item shows it on its own, so it's written here: as live blades word it (auction items, September 2026),
     * with the damage the plugin's text had from before items were data.
     */
    private static final ItemBlock WITHER_IMPACT = new ItemBlock("ABILITY", "Wither Impact", "&6Ability: Wither Impact  &e&lRIGHT CLICK",
            "RIGHT_CLICK", List.of(
                    "&7Teleport &a10 blocks&7 ahead of you",
                    "&7dealing &c13,961.2 &7damage to nearby",
                    "&7enemies. Also reduces your damage",
                    "&7taken and grants an absorption",
                    "&7shield for &e5 seconds&7."), 300, 0, 0, 0, 0, 0, 0);

    /** A scroll: its flag in the blade's data, and its item. */
    private record Scroll(String flag, String itemId) {
    }

    @Override
    public NBTTagCompound nbt(SkyBlockItem item) {
        NBTTagCompound nbt = new NBTTagCompound();
        for (Scroll scroll : SCROLLS) nbt.setBoolean(scroll.flag(), false);
        return nbt;
    }

    @Override
    public List<String> lore(SkyBlockItem item, NBTTagCompound tag, List<String> lore) {
        if (scrolls(tag).isEmpty() || lore.size() < CLASS_ABILITY.size()) return lore;
        List<String> end = lore.subList(lore.size() - CLASS_ABILITY.size(), lore.size());
        return end.equals(CLASS_ABILITY) ? lore.subList(0, lore.size() - CLASS_ABILITY.size()) : lore;
    }

    @Override
    public List<ItemBlock> blocks(SkyBlockItem item, NBTTagCompound tag, List<ItemBlock> blocks) {
        List<Scroll> scrolls = scrolls(tag);
        if (scrolls.isEmpty()) return blocks;
        List<ItemBlock> all = new ArrayList<>(blocks);
        if (scrolls.size() == SCROLLS.size()) {
            all.add(WITHER_IMPACT);
            return all;
        }
        for (Scroll scroll : scrolls) {
            // Without the scroll's item (no data), there's nothing to show.
            SkyBlockItem scrollItem = ItemRegistry.get(scroll.itemId());
            if (scrollItem == null) continue;
            scrollItem.blocks().stream().filter(ItemBlock::isAbility).findFirst().ifPresent(all::add);
        }
        return all;
    }

    /** The scrolls it has, in order. */
    private static List<Scroll> scrolls(NBTTagCompound tag) {
        return SCROLLS.stream().filter(scroll -> tag.getBoolean(scroll.flag())).toList();
    }
}
