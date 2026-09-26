package net.icxd.dungeons.item;

import net.icxd.dungeons.utils.Utils;
import lombok.Getter;
import org.reflections.Reflections;

import java.util.HashMap;

public class ItemRegistry {
    @Getter private static final HashMap<String, SkyBlockItem> registry = new HashMap<>();
    public ItemRegistry() {
        try {
            for (Class<?> item : Utils.instantiableSubTypesOf(SkyBlockItem.class)) {
                SkyBlockItem skyBlockItem = (SkyBlockItem) item.newInstance();
                registry.put(skyBlockItem.id().toUpperCase(), skyBlockItem);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    public static SkyBlockItem get(String id) {
        return registry.get(id.toUpperCase());
    }
}
