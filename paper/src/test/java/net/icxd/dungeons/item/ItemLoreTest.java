package net.icxd.dungeons.item;

import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Item names and lore against Hypixel's (the NEU item repository's dumps, with the resource pack's
 * icons as their classic symbols). Items have no owner here, so no requirement lines.
 */
class ItemLoreTest {
    private static NBTTagCompound data(String id) {
        NBTTagCompound tag = ItemBuilder.newData(ItemRegistry.get(id));
        // Attributes are random; these tests leave them out.
        tag.remove("attribute_1");
        tag.remove("attribute_2");
        return tag;
    }

    private static List<String> lore(String id, NBTTagCompound tag) {
        return ItemBuilder.lore(ItemRegistry.get(id), tag);
    }

    private static void enchant(NBTTagCompound tag, String name, int level) {
        NBTTagList list = tag.getList("enchantments", 10);
        NBTTagCompound enchantment = new NBTTagCompound();
        enchantment.setString("name", name);
        enchantment.setInt("lvl", level);
        list.add(enchantment);
        tag.set("enchantments", list);
    }

    @Test
    void aspectOfTheVoid() {
        NBTTagCompound tag = data("ASPECT_OF_THE_VOID");
        assertEquals("§5Aspect of the Void", ItemBuilder.name(ItemRegistry.get("ASPECT_OF_THE_VOID"), tag));
        assertEquals(List.of(
                "&7Damage: &c+120",
                "&7Strength: &c+100",
                "&7Gemstones: &8[&7✎&8]",
                "",
                "&6Ability: Instant Transmission  &e&lRIGHT CLICK",
                "&7Teleport &a8 blocks&7 ahead of you and",
                "&7gain &a+50 &f✦ Speed&7 for &a3 seconds&7.",
                "&8Mana Cost: &b45✎",
                "",
                "&8This item can be reforged!",
                "§5§lEPIC SWORD"), lore("ASPECT_OF_THE_VOID", tag));
    }

    @Test
    void gemstone() {
        NBTTagCompound tag = data("FINE_RUBY_GEM");
        assertEquals("§9❤ Fine Ruby Gemstone", ItemBuilder.name(ItemRegistry.get("FINE_RUBY_GEM"), tag));
        assertEquals(List.of(
                "&8Collection Item",
                "",
                "&7A type of &cRuby &7that has clearly",
                "&7been treated with care.",
                "",
                "&7Some say that when &eharnessed",
                "&eproperly&7, it can give its owner extra",
                "&c❤ Health&7.",
                "",
                "§9§lRARE GEMSTONE"), lore("FINE_RUBY_GEM", tag));
    }

    /** Hypixel's dungeon items show what each stat comes to in a dungeon: +10% at Catacombs 0. */
    @Test
    void dungeonItemStats() {
        List<String> lore = lore("HYPERION", data("HYPERION"));
        assertEquals("&7Gear Score: &d615", lore.get(0));
        assertEquals("&7Damage: &c+260 &8(+286)", lore.get(1));
        assertEquals("&7Ferocity: &c+30 &8(+30)", lore.get(3));
        assertEquals("§6§lLEGENDARY DUNGEON SWORD", lore.get(lore.size() - 1));
    }

    @Test
    void oneEnchantWithItsDescription() {
        NBTTagCompound tag = data("ASPECT_OF_THE_VOID");
        enchant(tag, "sharpness", 5);
        List<String> lore = lore("ASPECT_OF_THE_VOID", tag);
        assertEquals(List.of("", "&9Sharpness V", "&7Increases melee damage dealt by &a30%", ""), lore.subList(3, 7));
    }

    /** On a dungeon item even two enchantments share a line, without descriptions. */
    @Test
    void dungeonItemEnchantsAreCompact() {
        NBTTagCompound tag = data("HYPERION");
        enchant(tag, "smite", 7);
        enchant(tag, "critical", 6);
        List<String> lore = lore("HYPERION", tag);
        assertEquals("&9Critical VI, &9Smite VII", lore.get(7));
    }

    @Test
    void stars() {
        NBTTagCompound tag = data("HYPERION");
        tag.setInt("upgrade_count", 7);
        assertEquals(" &6✪✪✪✪✪&c➋", ItemBuilder.stars(ItemRegistry.get("HYPERION"), tag));
        NBTTagCompound crimson = data("INFERNAL_CRIMSON_HELMET");
        crimson.setInt("upgrade_count", 6);
        assertEquals(" &d✪&6✪✪✪✪", ItemBuilder.stars(ItemRegistry.get("INFERNAL_CRIMSON_HELMET"), crimson));
    }

    @Test
    void recombobulated() {
        NBTTagCompound tag = data("HYPERION");
        tag.setString("rarity", "MYTHIC");
        tag.setBoolean("recombobulated", true);
        assertEquals("§d§l&ka&r §d§lMYTHIC DUNGEON SWORD §d§l&ka", ItemBuilder.rarityLine(ItemRegistry.get("HYPERION"), tag,
                net.icxd.dungeons.item.enums.Rarity.MYTHIC));
    }

    @Test
    void catacombsBoost() {
        assertEquals(0.10, ItemBuilder.catacombsBoost(0), 1e-9);
        assertEquals(0.31, ItemBuilder.catacombsBoost(4), 1e-9);
        assertEquals(1.95, ItemBuilder.catacombsBoost(21), 1e-9);
        assertEquals(4.85, ItemBuilder.catacombsBoost(50), 1e-9);
    }
}
