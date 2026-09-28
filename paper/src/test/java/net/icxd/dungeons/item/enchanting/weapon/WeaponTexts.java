package net.icxd.dungeons.item.enchanting.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonParser;

import net.icxd.dungeons.item.enchanting.EnchantmentData;

/**
 * A made-up enchantment table for the weapon enchantments' tests, in the private file's format: texts with their
 * numbers where the real books have them (Hypixel's own words stay in the private data; PrivateWeaponTextTest reads
 * those). A test that puts it in use puts {@link EnchantmentData#EMPTY} back after ({@link #reset}).
 */
final class WeaponTexts {
    private WeaponTexts() {
    }

    private static String level(int level, String text, String tierUp) {
        String tier = tierUp == null ? "" : ",\"tier_up\":\"" + tierUp + "\"";
        return "\"" + level + "\":{\"text\":\"" + text + "\",\"rarity\":\"COMMON\"" + tier + "}";
    }

    private static String entry(String id, String hypixel, int max, String levels) {
        return "\"" + id + "\":{\"name\":\"" + id + "\",\"hypixel\":\"" + hypixel + "\",\"ultimate\":" + hypixel.startsWith("ultimate_")
                + ",\"min\":1,\"max\":" + max + ",\"table\":null,\"xp\":[],\"enchanting\":0,\"applies\":[\"Sword\"],\"conflicts\":[],"
                + "\"levels\":{" + levels + "}}";
    }

    static String json() {
        List<String> entries = new ArrayList<>(List.of(
                entry("cleave", "cleave", 2, level(1, "&7Hits &a5% &7of it to others within &a3.3 &7blocks.", null) + ","
                        + level(2, "&7Hits &a10% &7of it to others within &a3.6 &7blocks.", null)),
                entry("life_steal", "life_steal", 2, level(1, "&7Heal &c2.4❤ &7a hit.", null) + "," + level(2, "&7Heal &c4.8❤ &7a hit.", null)),
                entry("syphon", "syphon", 1, level(1, "&7Regen &4+0.5♨ Vitality &7a hit. &8(1s Cooldown).", null)),
                entry("champion", "champion", 3,
                        level(1, "&7Gain &a3% &7Combat XP. The 2nd hit gives &6+1.4 coins &7& &3+7 &7orbs.", "&850k Combat XP to tier up!") + ","
                        + level(2, "&7Gain &a3.78% &7Combat XP. The 2nd hit gives &6+1.8 coins &7& &3+9 &7orbs.", "&8100k Combat XP to tier up!")
                        + "," + level(3, "&7Gain &a4.56% &7Combat XP. The 2nd hit gives &6+2.2 coins &7& &3+11 &7orbs.", null)),
                entry("toxophilite", "toxophilite", 1,
                        level(1, "&7Gain &a3% &7Combat XP. Grants &9+3.7☣ Crit Chance&7.", "&850k Combat XP to tier up!")),
                entry("tabasco", "tabasco", 2, level(2, "&7Grants &f+2 &7weapon damage without a &5Dragon &7pet.", null)),
                entry("wise", "ultimate_wise", 5, level(5, "&7Its abilities cost &a50%&7 less mana.", null)),
                entry("soul_eater", "ultimate_soul_eater", 1, level(1, "&7Gains &c2x &7a kill's Damage. &8(Max 1M outside Dungeons).", null)),
                entry("jerry", "ultimate_jerry", 1, level(1, "&7Base damage of &fAspect of the Jerry&7 &a1000%&7 more.", null))));
        return "{\"format\":1,\"enchantments\":{" + String.join(",", entries) + "}}";
    }

    /** Puts the made-up table in use. */
    static EnchantmentData use() {
        List<String> problems = new ArrayList<>();
        EnchantmentData data = EnchantmentData.parse(JsonParser.parseString(json()).getAsJsonObject(), problems);
        assertEquals(List.of(), problems);
        EnchantmentData.use(data);
        return data;
    }

    static void reset() {
        EnchantmentData.use(EnchantmentData.EMPTY);
    }
}
