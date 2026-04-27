package net.icxd.dungeons.item;

import lombok.Getter;
import org.reflections.Reflections;

import java.util.HashMap;

public class ItemRegistry {
    @Getter private static final HashMap<String, SkyBlockItem> registry = new HashMap<>();
    public ItemRegistry() {
        try {
            for (Class<?> item : new Reflections().getSubTypesOf(SkyBlockItem.class)) {
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
