package net.icxd.dungeons.item.enchanting;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Each enchantment's name and description at each level, as Hypixel writes them (enchantments.json,
 * made from the in-game enchanted books in the NEU item repository; see tools/items).
 */
final class EnchantmentTexts {
    record Entry(String name, Map<String, String> levels) {
    }

    private static final Map<String, Entry> TEXTS = load();

    private EnchantmentTexts() {
    }

    static Entry get(String namespace) {
        return TEXTS.get(namespace);
    }

    private static Map<String, Entry> load() {
        try (InputStream in = EnchantmentTexts.class.getResourceAsStream("/enchantments.json")) {
            if (in == null) return Map.of();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Map<String, Entry> texts = new Gson().fromJson(reader, new TypeToken<Map<String, Entry>>() { }.getType());
                return texts == null ? Map.of() : texts;
            }
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("reading enchantments.json", e);
        }
    }
}
