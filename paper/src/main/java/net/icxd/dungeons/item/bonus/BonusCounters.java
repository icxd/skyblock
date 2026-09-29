package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.accessory.CrestCounter;
import net.icxd.dungeons.item.behaviour.ItemBehaviour;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The lines of the bonuses that count on their piece ({@link CountedSets}: Training's "Zombies Killed", a Bulwark's
 * "Piece Bonus" and "Next Upgrade", Absorb's "Magma Cubes Killed", Riches' "Scavenger Coins Gained"), from what
 * the piece has counted. A piece that has counted nothing keeps the data's text, so new items (and the golden
 * test) are as they were. Registered with the other item behaviours, by the counting pieces' ids.
 */
public final class BonusCounters implements ItemBehaviour {
    /** The pieces that keep a count their text shows. */
    static final List<String> IDS = List.of("ZOMBIE_COMMANDER_CHESTPLATE", "ARMOR_OF_MAGMA_CHESTPLATE",
            "REVENANT_CHESTPLATE", "REVENANT_LEGGINGS", "REVENANT_BOOTS", "REAPER_CHESTPLATE", "REAPER_LEGGINGS", "REAPER_BOOTS",
            "TARANTULA_HELMET", "TARANTULA_CHESTPLATE", "TARANTULA_LEGGINGS", "TARANTULA_BOOTS",
            "PRIMORDIAL_HELMET", "PRIMORDIAL_CHESTPLATE", "PRIMORDIAL_LEGGINGS", "PRIMORDIAL_BOOTS",
            "FINAL_DESTINATION_HELMET", "FINAL_DESTINATION_CHESTPLATE", "FINAL_DESTINATION_LEGGINGS", "FINAL_DESTINATION_BOOTS",
            "ELEANOR_CAP", "ELEANOR_TUNIC", "ELEANOR_TROUSERS", "ELEANOR_SLIPPERS");

    private static Map<String, CountedSets.Counted> counted;

    private BonusCounters() {
    }

    /** Puts the behaviour in for each counting piece (see ItemBehaviours), and the accessories' that count (the Blood God Crest). */
    public static void register(BiConsumer<String, ItemBehaviour> put) {
        BonusCounters behaviour = new BonusCounters();
        for (String id : IDS) put.accept(id, behaviour);
        CrestCounter.register(put);
    }

    private static CountedSets.Counted counted(String name) {
        if (counted == null) counted = CountedSets.byName();
        return counted.get(name);
    }

    @Override
    public List<ItemBlock> blocks(SkyBlockItem item, NBTTagCompound tag, List<ItemBlock> blocks) {
        if (tag == null) return blocks;
        List<ItemBlock> out = null;
        for (int i = 0; i < blocks.size(); i++) {
            ItemBlock block = blocks.get(i);
            CountedSets.Counted bonus = block.name() == null ? null : counted(block.name());
            if (bonus == null || !tag.hasKey(bonus.key())) continue;
            List<String> text = bonus.counted(block.text(), CountedSets.count(tag, bonus), item);
            if (text.equals(block.text())) continue;
            if (out == null) out = new ArrayList<>(blocks);
            out.set(i, CountedSets.withText(block, text));
        }
        return out == null ? blocks : out;
    }
}
