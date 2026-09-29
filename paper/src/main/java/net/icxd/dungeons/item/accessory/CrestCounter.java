package net.icxd.dungeons.item.accessory;

import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.behaviour.ItemBehaviour;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.utils.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The Blood God Crest's and Sigil's "Counter" line from the kills counted on it ({@link AccessoryEffects}): "None
 * yet!" until it has one, as the data has it (so new ones, and the golden test, are as they were); then the count,
 * grouped by thousands (UNKNOWN how Hypixel writes it).
 */
public final class CrestCounter implements ItemBehaviour {
    private CrestCounter() {
    }

    /** Puts the behaviour in for the crests (see ItemBehaviours). */
    public static void register(BiConsumer<String, ItemBehaviour> put) {
        CrestCounter behaviour = new CrestCounter();
        put.accept(AccessoryEffects.CREST, behaviour);
        put.accept(AccessoryEffects.SIGIL, behaviour);
    }

    @Override
    public List<String> lore(SkyBlockItem item, NBTTagCompound tag, List<String> lore) {
        if (tag == null || !tag.hasKey(AccessoryEffects.KILLS)) return lore;
        return counter(lore, ItemCounters.get(tag, AccessoryEffects.KILLS));
    }

    /** The lines with the "Counter:" one showing this many kills. */
    static List<String> counter(List<String> lore, double kills) {
        List<String> out = new ArrayList<>(lore);
        for (int i = 0; i < out.size(); i++) {
            if (!out.get(i).replaceAll("[&§][0-9a-fk-or]", "").startsWith("Counter:")) continue;
            out.set(i, "&7Counter: &c" + (kills < 1 ? "None yet!" : Text.number(Math.floor(kills))));
            return out;
        }
        return out;
    }
}
