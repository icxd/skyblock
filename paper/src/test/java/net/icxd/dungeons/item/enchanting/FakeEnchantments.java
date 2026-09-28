package net.icxd.dungeons.item.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonParser;

import net.icxd.dungeons.hex.PrivateHex;

/**
 * Enchantment tables for tests: a small made-up one in the private file's format (Hypixel's table stays out of this
 * repository; a few of its lines are here, as the lore tests had them), and the real one from the private data
 * ({@link #real}, skipped without it). A test that puts one in use puts {@link EnchantmentData#EMPTY} back after
 * ({@link #reset}), so the others see what the plugin has before the table is read.
 */
public final class FakeEnchantments {
    private FakeEnchantments() {
    }

    /** One enchantment's JSON: its name, ultimate or not, levels, table levels, Exp costs, what it goes on, conflicts and texts. */
    private static String entry(String id, String hypixel, String name, boolean ultimate, int min, int max, int table, String xp,
                                int enchanting, String applies, String conflicts, String levels) {
        return "\"" + id + "\":{\"name\":\"" + name + "\",\"hypixel\":\"" + hypixel + "\",\"ultimate\":" + ultimate + ",\"min\":" + min
                + ",\"max\":" + max + ",\"table\":" + (table == 0 ? "null" : table) + ",\"xp\":[" + xp + "],\"enchanting\":" + enchanting
                + ",\"applies\":[" + applies + "],\"conflicts\":[" + conflicts + "],\"levels\":{" + levels + "}}";
    }

    private static String level(int level, String text, String rarity) {
        return "\"" + level + "\":{\"text\":\"" + text + "\",\"rarity\":\"" + rarity + "\"}";
    }

    static final String SWORDS = "\"Sword\",\"Longsword\"";

    /** The made-up table, as a JSON object in the file's format. */
    public static String json() {
        List<String> entries = new ArrayList<>(List.of(
                entry("sharpness", "sharpness", "Sharpness", false, 1, 7, 5, "10,15,20,25,30,100,200", 0, SWORDS, "\"one_for_all\"",
                        level(1, "&7Increases melee damage dealt by &a5%", "COMMON") + ","
                                + level(5, "&7Increases melee damage dealt by &a30%", "UNCOMMON") + ","
                                + level(6, "&7Increases melee damage dealt by &a45%", "RARE")),
                entry("smite", "smite", "Smite", false, 1, 7, 5, "10,15,20,25,30,100,200", 0, SWORDS, "\"one_for_all\"",
                        level(7, "&7Increases damage dealt to Undead mobs by &a50%&7.", "EPIC")),
                entry("critical", "critical", "Critical", false, 1, 7, 5, "10,20,30,40,50,75,200", 9, SWORDS, "\"one_for_all\"",
                        level(6, "&7Increases &9☠ Crit Damage &7by &a70%&7.", "RARE") + ","
                                + level(7, "&7Increases &9☠ Crit Damage &7by &a100%&7.", "EPIC")),
                entry("life_steal", "life_steal", "Life Steal", false, 1, 5, 3, "20,25,30,50,200", 0, SWORDS,
                        "\"syphon\",\"mana_steal\",\"one_for_all\"", level(1, "&7Heals a little.", "COMMON")),
                entry("syphon", "syphon", "Drain", false, 1, 5, 3, "20,25,30,45,200", 15, SWORDS, "\"life_steal\",\"mana_steal\",\"one_for_all\"",
                        level(1, "&7Heals on crits.", "COMMON")),
                entry("mana_steal", "mana_steal", "Mana Steal", false, 1, 3, 0, "20,25,30", 0, SWORDS, "\"life_steal\",\"syphon\",\"one_for_all\"",
                        level(1, "&7Regain mana.", "COMMON")),
                entry("champion", "champion", "Champion", false, 1, 10, 0, "10,25,25,25,25,25,25,25,25,25", 0, SWORDS, "",
                        "\"1\":{\"text\":\"&7Gain &a3% &7extra Combat XP.\",\"lines\":[\"&7Gain &a3% &7extra\",\"&7Combat XP.\"],"
                                + "\"rarity\":\"COMMON\",\"tier_up\":\"&850k Combat XP to tier up!\"}"),
                entry("telekinesis", "telekinesis", "Telekinesis", false, 1, 1, 1, "5", 0, SWORDS, "", level(1, "&7Drops go to you.", "COMMON")),
                entry("overload", "overload", "Overload", false, 1, 5, 0, "50,100,150,200,250", 0, "\"Bow\"", "",
                        level(5, "&7Increases &9☠ Crit Damage &7by &a5%&7 and &9☣ Crit Chance &7by &a5%&7. Having more is good.", "UNCOMMON")),
                entry("growth", "growth", "Growth", false, 1, 7, 5, "10,20,30,40,50,95,199", 0, "\"Armor\"", "",
                        level(5, "&7Grants &a+75 &c❤ Health&7.", "UNCOMMON")),
                entry("one_for_all", "ultimate_one_for_all", "One For All", true, 1, 1, 0, "50", 0, SWORDS,
                        "\"sharpness\",\"smite\",\"critical\",\"life_steal\",\"syphon\",\"mana_steal\"",
                        level(1, "&7Removes all other enchants.", "COMMON")),
                entry("wise", "ultimate_wise", "Ultimate Wise", true, 1, 5, 0, "50,100,150,200,250", 20, SWORDS + ",\"Wand\"", "",
                        level(1, "&7Reduces mana costs.", "COMMON")),
                entry("bank", "ultimate_bank", "Bank", true, 1, 5, 0, "50,100,150,200,250", 0, "\"Armor\"", "",
                        level(1, "&7Saves coins.", "COMMON"))));
        return "{\"format\":1,\"order\":[\"sharpness\",\"champion\",\"smite\",\"critical\",\"life_steal\",\"syphon\",\"mana_steal\","
                + "\"one_for_all\",\"wise\"],\"enchantments\":{" + String.join(",", entries) + "}}";
    }

    /** The made-up table. */
    public static EnchantmentData table() {
        List<String> problems = new ArrayList<>();
        EnchantmentData data = EnchantmentData.parse(JsonParser.parseString(json()).getAsJsonObject(), problems);
        assertEquals(List.of(), problems);
        return data;
    }

    /** Puts the made-up table in use. */
    public static EnchantmentData use() {
        EnchantmentData data = table();
        EnchantmentData.use(data);
        return data;
    }

    /** The private data's table (the test is skipped without it), read as the plugin reads it. */
    public static EnchantmentData real() {
        List<String> problems = new ArrayList<>();
        EnchantmentData data = EnchantmentData.read(PrivateHex.folder(), problems);
        assertNotNull(data, problems.toString());
        assertEquals(List.of(), problems);
        return data;
    }

    public static void reset() {
        EnchantmentData.use(EnchantmentData.EMPTY);
    }
}
