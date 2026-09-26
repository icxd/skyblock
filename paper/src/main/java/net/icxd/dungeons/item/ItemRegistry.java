package net.icxd.dungeons.item;

import net.icxd.dungeons.item.items.CrimsonArmor;
import net.icxd.dungeons.item.items.Gemstones;
import net.icxd.dungeons.item.items.bow.Terminator;
import net.icxd.dungeons.item.items.drill.DivansDrill;
import net.icxd.dungeons.item.items.helmet.SubzeroHelmet;
import net.icxd.dungeons.item.items.longsword.DarkClaymore;
import net.icxd.dungeons.item.items.none.AttributeShard;
import net.icxd.dungeons.item.items.none.HeavyPearl;
import net.icxd.dungeons.item.items.none.Mithril;
import net.icxd.dungeons.item.items.none.PerfectHopper;
import net.icxd.dungeons.item.items.none.PerfectlyCutFuelTank;
import net.icxd.dungeons.item.items.sword.AspectOfTheVoid;
import net.icxd.dungeons.item.items.sword.Hyperion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every SkyBlock item there is, by id. Add new items to the list below. */
public final class ItemRegistry {
    private static final Map<String, SkyBlockItem> registry = new LinkedHashMap<>();

    static {
        List<SkyBlockItem> items = new ArrayList<>();
        items.addAll(List.of(new Hyperion(), new AspectOfTheVoid(), new DarkClaymore(), new Terminator(), new DivansDrill(),
                new SubzeroHelmet(), new AttributeShard(), new HeavyPearl(), new Mithril(), new PerfectHopper(), new PerfectlyCutFuelTank()));
        items.addAll(CrimsonArmor.all());
        items.addAll(Gemstones.all());
        for (SkyBlockItem item : items) {
            if (registry.put(item.id().toUpperCase(), item) != null) throw new IllegalStateException("two items are " + item.id());
        }
    }

    private ItemRegistry() {
    }

    /** Null if there's no such item. */
    public static SkyBlockItem get(String id) {
        return id == null ? null : registry.get(id.toUpperCase());
    }

    public static Map<String, SkyBlockItem> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }
}
