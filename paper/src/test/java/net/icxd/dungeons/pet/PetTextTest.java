package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.collection.PrivateData;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.pet.PetText.Where;

/**
 * The recorded pets (2026-10-25: the Pets menu at 00:21.3, a bought Bee as an item at 00:59.1), line for line, as the
 * plugin shows them from the private data (pets.json and items.json, for the held items; skipped without them). The
 * recorded lines' glyphs are the plugin's symbols here, as the data has them (tools/items/data/glyphs.tsv).
 */
class PetTextTest {
    private static PetData data;

    private static final List<String> GUARDIAN = List.of(
            "&8Enchanting Pet",
            "",
            "&7Defense: &a+47",
            "&7Intelligence: &b+94",
            "",
            "&6Laserbeam",
            "&7Zaps your enemies for &b112.8x &7your",
            "&b✎ Intelligence &7every &a3s&7.",
            "",
            "&6Enchanting Wisdom Boost",
            "&7Grants &3+28.2☯ Enchanting Wisdom&7.",
            "",
            "&6Mana Pool",
            "&7Regenerate &b28.2% &7extra mana,",
            "&7doubled when near or in water",
            "",
            "&6Lucky Seven",
            "&7Gain &b+6.6% &7chance to find &5ultra rare",
            "&7books in &dSuperpairs&7.",
            "",
            "&6Held Item: &fAll Skills Exp Boost",
            "&7Gives &a+10% &7pet exp for all skills.",
            "",
            "&7Progress to Level 95: &e2.2%",
            "&2&l&m &f&l&m                        &r &e28,101&6/&e1.3M",
            "",
            "&eLeft-click to summon!",
            "&eShift Left-click to toggle as favorite!",
            "&eRight-click to convert to an item!");

    private static final List<String> SPIDER = List.of(
            "&8Combat Pet",
            "",
            "&7True Defense: &f+10",
            "&7Strength: &c+34.5",
            "&7Crit Chance: &9+6.9%",
            "",
            "&6One With The Spider",
            "&7Applies &c+11.9❁ Strength &7to all",
            "&4Arachnal Ж &7weapons, armor, and",
            "&7equipment you have equipped.",
            "",
            "&6Web-weaver",
            "&7Upon hitting a monster, it becomes",
            "&7slowed by &a27.6%&7.",
            "",
            "&6Spider Whisperer",
            "&7Spider, Cave Spider and Tarantula",
            "&7minions work &a20.7% &7faster while on",
            "&7your island.",
            "",
            "&6Web Battlefield",
            "&7Killing mobs grants &c+4.14❁ Strength",
            "&7and &b+0.69✯ Magic Find &7for &a40s &7to all",
            "&7players staying within &a20 &7blocks of",
            "&7where they died. &8Stacks up to 10",
            "&8times.",
            "",
            "&6Held Item: &5Dwarf Turtle Shelmet",
            "&7Grants &f+10❂ True Defense &7and",
            "&7makes the pet's owner immune to",
            "&7knockback.",
            "",
            "&a(1/10) Pet Candy Used",
            "",
            "&7Progress to Level 70: &e29.4%",
            "&2&l&m        &f&l&m                 &r &e56,618.6&6/&e192.7k",
            "",
            "&eLeft-click to summon!",
            "&eShift Left-click to toggle as favorite!",
            "&eRight-click to convert to an item!");

    private static final List<String> LION = List.of(
            "&8Foraging Pet",
            "",
            "&7True Defense: &f+10",
            "&7Strength: &c+50",
            "&7Ferocity: &c+5",
            "&7Speed: &f+25",
            "",
            "&6Primal Force",
            "&7Adds &c+20 ❁ Damage &7and &c+20 ❁",
            "&cStrength &7to your weapons.",
            "",
            "&6First Pounce",
            "&7First Strike, Triple-Strike, and",
            "&d&lCombo &7are &a100% &7more effective.",
            "",
            "&6King of the Jungle",
            "&7Deal &c+150% ❁ Damage &7against mobs",
            "&7that have attacked you.",
            "",
            "&6Held Item: &5Dwarf Turtle Shelmet",
            "&7Grants &f+10❂ True Defense &7and",
            "&7makes the pet's owner immune to",
            "&7knockback.",
            "",
            "&b&lMAX LEVEL",
            "&8▸ 27,503,303 XP",
            "",
            "&cClick to despawn!",
            "&eShift Left-click to toggle as favorite!",
            "&eRight-click to convert to an item!");

    private static final List<String> WOLF = List.of(
            "&8Combat Pet",
            "",
            "&7Health: &c+50",
            "&7True Defense: &f+20",
            "&7Crit Damage: &9+10%",
            "&7Speed: &f+20",
            "",
            "&6Alpha Dog",
            "&7Take &a30% &7less damage from wolves",
            "",
            "&6Pack Leader",
            "&7Gain &a15 &9☠ Crit Damage &7for every",
            "&7nearby wolf monster",
            "&8Max 10 wolves",
            "",
            "&6Combat Wisdom Boost",
            "&7Grants &3+30☯ Combat Wisdom&7.",
            "",
            "&6Held Item: &5Dwarf Turtle Shelmet",
            "&7Grants &f+10❂ True Defense &7and",
            "&7makes the pet's owner immune to",
            "&7knockback.",
            "",
            "&b&lMAX LEVEL",
            "&8▸ 25,656,361 XP",
            "",
            "&eLeft-click to summon!",
            "&eShift Left-click to toggle as favorite!",
            "&eRight-click to convert to an item!");

    private static final List<String> MITHRIL_GOLEM = List.of(
            "&8Mining Pet",
            "",
            "&7True Defense: &f+45.5",
            "&7Mining Fortune: &6+22.75",
            "",
            "&6Mithril Affinity",
            "&7Grants &6+182⸕ Mining Speed &7when",
            "&7mining &2Mithril&7.",
            "",
            "&6Subterranean Battler",
            "&7Increases all &cCombat Stats &7by &a+18.2%",
            "&7on &6Mining Islands&7.",
            "",
            "&6The Smell Of Powder",
            "&7Grants &2+18.2% ᠅ Mithril Powder &7from",
            "&7all sources.",
            "",
            "&6Held Item: &9Mining Exp Boost",
            "&7Gives &a+40% &7pet exp for &aMining&7.",
            "",
            "&7Progress to Level 92: &e69.7%",
            "&2&l&m                  &f&l&m       &r &e709,120.8&6/&e1M",
            "",
            "&8Can be upgraded at Kat in The Hub!",
            "",
            "&eLeft-click to summon!",
            "&eShift Left-click to toggle as favorite!",
            "&eRight-click to convert to an item!");

    private static final List<String> ZOMBIE = List.of(
            "&8Combat Pet",
            "",
            "&7Health: &c+72",
            "&7True Defense: &f+10",
            "&7Crit Damage: &9+21.6%",
            "",
            "&6Bite Shield",
            "&7Reduce the damage taken from",
            "&7zombies by &a22.2%&7.",
            "",
            "&6Rotten Blade",
            "&7Deal &a115% &7more damage to &2༕ Undead",
            "&7mobs",
            "",
            "&6Living Dead",
            "&7Increases all stats on &2Undead ༕",
            "&7armor by &a18%&7.",
            "",
            "&6Held Item: &5Dwarf Turtle Shelmet",
            "&7Grants &f+10❂ True Defense &7and",
            "&7makes the pet's owner immune to",
            "&7knockback.",
            "",
            "&7Progress to Level 73: &e42.6%",
            "&2&l&m           &f&l&m              &r &e101,261.2&6/&e237.7k",
            "",
            "&eLeft-click to summon!",
            "&eShift Left-click to toggle as favorite!",
            "&eRight-click to convert to an item!");

    private static final List<String> RABBIT = List.of(
            "&8Farming Pet",
            "",
            "&7Health: &c+67",
            "&7Speed: &f+13.4",
            "",
            "&6Happy Feet",
            "&7Jump potions also give &a+33.5 &7speed",
            "",
            "&6Farming Wisdom Boost",
            "&7Grants &3+20.1☯ Farming Wisdom&7.",
            "",
            "&6Efficient Farming",
            "&7Farming minions work &a20.1% &7faster",
            "&7while on your island",
            "",
            "&6Held Item: &5Farming Exp Boost",
            "&7Gives &a+50% &7pet exp for &aFarming&7.",
            "",
            "&7Progress to Level 68: &e55.4%",
            "&2&l&m              &f&l&m           &r &e92,886&6/&e167.7k",
            "",
            "&8Can be upgraded at Kat in The Hub!",
            "",
            "&eLeft-click to summon!",
            "&eShift Left-click to toggle as favorite!",
            "&eRight-click to convert to an item!");

    private static final List<String> GRANDMA_WOLF = List.of(
            "&8Combat Pet",
            "",
            "&7Health: &c+1",
            "&7Strength: &c+0.25",
            "",
            "&6Kill Combo",
            "&7Gain buffs for combo kills. Effects",
            "&7stack as you increase your combo.",
            "",
            "&a5 Combo &8(lasts &a8s&8)",
            "  &b+3% ✯ Magic Find",
            "&a10 Combo &8(lasts &a6s&8)",
            "  &8+&610 &7coins per kill",
            "&a15 Combo &8(lasts &a4s&8)",
            "  &b+3% ✯ Magic Find",
            "&a20 Combo &8(lasts &a3s&8)",
            "  &3+15☯ Combat Wisdom",
            "&a25 Combo &8(lasts &a3s&8)",
            "  &b+3% ✯ Magic Find",
            "&a30 Combo &8(lasts &a2s&8)",
            "  &8+&610 &7coins per kill",
            "",
            "&8This pet's perks are active even",
            "&8when the pet is not summoned!",
            "",
            "&7Progress to Level 2: &e0%",
            "&f&l&m                         &r &e0&6/&e660",
            "",
            "&eLeft-click to summon!",
            "&eShift Left-click to toggle as favorite!",
            "&eRight-click to convert to an item!");

    private static final List<String> BEE_ITEM = List.of(
            "&8Farming Pet",
            "",
            "&7Strength: &c+5.25",
            "&7Intelligence: &b+0.5",
            "&7Speed: &f+0.1",
            "",
            "&6Hive",
            "&7For each player within &a25 &7blocks:",
            " &b+1.02✎ Intelligence",
            " &c+1.02❁ Strength",
            " &a+1.01❈ Defense",
            "&8Max 15 players",
            "",
            "&7Progress to Level 2: &e0%",
            "&f&l&m                         &r &e0&6/&e100",
            "",
            "&eRight-click to add this pet to your pet menu!",
            "",
            "&8Can be upgraded at Kat in The Hub!",
            "&f&lCOMMON PET");

    @BeforeAll
    static void load() {
        Path folder = PetFixtures.dataFolder();
        assumeTrue(Files.isRegularFile(folder.resolve("pets/pets.json")), "no private pets.json");
        assumeTrue(Files.isRegularFile(PrivateData.itemsFile()), "no private items.json");
        assertNull(ItemRegistry.loadData(PrivateData.itemsFile()).failure(), "the items didn't load");
        data = PetData.load(folder);
        Pets.data(data);
    }

    @AfterAll
    static void unload() {
        Pets.data(PetData.empty());
    }

    /** A pet at a level, this much XP into the next. */
    private static OwnedPet at(String type, Rarity tier, int level, double into, String held, int candy) {
        Pet kind = data.pet(type);
        return new OwnedPet("test", type, tier, data.totalXp(kind, tier, level) + into, held, candy, null, false);
    }

    private static void menu(String name, List<String> lore, OwnedPet pet, boolean out) {
        assertEquals(name, PetText.name(pet, data, Where.MENU));
        assertEquals(lore, PetText.lore(pet, data, Where.MENU, out));
    }

    @Test
    void guardian() {
        menu("&7[Lvl 94] &dGuardian", GUARDIAN, at("GUARDIAN", Rarity.MYTHIC, 94, 28_101, "PET_ITEM_ALL_SKILLS_BOOST_COMMON", 0), false);
    }

    @Test
    void spider() {
        menu("&7[Lvl 69] &dSpider", SPIDER, at("SPIDER", Rarity.MYTHIC, 69, 56_618.6, "DWARF_TURTLE_SHELMET", 1), false);
    }

    @Test
    void lionOut() {
        OwnedPet lion = new OwnedPet("test", "LION", Rarity.LEGENDARY, 27_503_303, "DWARF_TURTLE_SHELMET", 0, null, false);
        menu("&7[Lvl 100] &6Lion", LION, lion, true);
    }

    @Test
    void wolf() {
        menu("&7[Lvl 100] &6Wolf", WOLF, new OwnedPet("test", "WOLF", Rarity.LEGENDARY, 25_656_361, "DWARF_TURTLE_SHELMET", 0, null, false), false);
    }

    @Test
    void mithrilGolem() {
        menu("&7[Lvl 91] &6Mithril Golem", MITHRIL_GOLEM, at("MITHRIL_GOLEM", Rarity.LEGENDARY, 91, 709_120.8, "PET_ITEM_MINING_SKILL_BOOST_RARE", 0), false);
    }

    @Test
    void zombie() {
        menu("&7[Lvl 72] &6Zombie", ZOMBIE, at("ZOMBIE", Rarity.LEGENDARY, 72, 101_261.2, "DWARF_TURTLE_SHELMET", 0), false);
    }

    @Test
    void rabbit() {
        menu("&7[Lvl 67] &6Rabbit", RABBIT, at("RABBIT", Rarity.LEGENDARY, 67, 92_886, "PET_ITEM_FARMING_SKILL_BOOST_EPIC", 0), false);
    }

    @Test
    void grandmaWolf() {
        menu("&7[Lvl 1] &6Grandma Wolf", GRANDMA_WOLF, at("GRANDMA_WOLF", Rarity.LEGENDARY, 1, 0, null, 0), false);
    }

    @Test
    void beeItem() {
        OwnedPet bee = at("BEE", Rarity.COMMON, 1, 0, null, 0);
        assertEquals("&7[Lvl 1] &fBee", PetText.name(bee, data, Where.ITEM));
        assertEquals(BEE_ITEM, PetText.lore(bee, data, Where.ITEM, false));
    }

    /** The recorded Bee's level-ups: 6.4 a wheat (4 Farming XP at Taming 60) took it to 2, 3 and 4 by the 28th, 58th and 90th. */
    @Test
    void recordedBee() {
        Pet bee = data.pet("BEE");
        double each = PetXp.petXp(bee, net.icxd.dungeons.skill.Skill.FARMING, 4, 60, 0);
        assertEquals(List.of(1, 2, 2, 3, 3, 4), List.of(data.level(bee, Rarity.UNCOMMON, 27 * each), data.level(bee, Rarity.UNCOMMON, 28 * each),
                data.level(bee, Rarity.UNCOMMON, 57 * each), data.level(bee, Rarity.UNCOMMON, 58 * each), data.level(bee, Rarity.UNCOMMON, 89 * each),
                data.level(bee, Rarity.UNCOMMON, 90 * each)));
    }
}
