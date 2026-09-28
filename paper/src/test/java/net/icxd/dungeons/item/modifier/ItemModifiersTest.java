package net.icxd.dungeons.item.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** What the modifiers in an item's data do to its stats, lore and abilities, on made-up items. */
class ItemModifiersTest {
    private static ItemBlock ability(String name, String activation, List<String> text, double mana, double manaPercent) {
        return new ItemBlock("ABILITY", name, "&6Ability: " + name + "  &e&lRIGHT CLICK", activation, text, mana, manaPercent, 0, 0, 0, 0, 0);
    }

    private static NBTTagCompound tag(String key, int value) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInt(key, value);
        return tag;
    }

    @Test
    void transmissionTunersReachFurther() {
        // The first number of blocks, whatever colours sit between it and "blocks".
        assertEquals(List.of("&7Teleport &a12 blocks&7 ahead of you.", "&7Then &a8 blocks&7 more."),
                ItemModifiers.farther(List.of("&7Teleport &a8 blocks&7 ahead of you.", "&7Then &a8 blocks&7 more."), 4));
        assertEquals(List.of("&7Zap a line &56 &7blocks forward."), ItemModifiers.farther(List.of("&7Zap a line &54 &7blocks forward."), 2));
        // A colour that's a digit isn't part of the number: 8 and 4 are 12, not "&58" and 4 "&62".
        assertEquals(List.of("&7Zap a line &512 &7blocks forward."), ItemModifiers.farther(List.of("&7Zap a line &58 &7blocks forward."), 4));
        assertEquals(List.of("&7Go &a&l9 blocks&7."), ItemModifiers.farther(List.of("&7Go &a&l8 blocks&7."), 1));
        assertEquals(List.of("&7Teleport to your block", "&7up to &a60 blocks &7away."),
                ItemModifiers.farther(List.of("&7Teleport to your block", "&7up to &a57 blocks &7away."), 3));
        assertEquals(List.of("&7No range here."), ItemModifiers.farther(List.of("&7No range here."), 4));

        ItemBlock blink = ability("Test Transmission", "RIGHT_CLICK", List.of("&7Teleport &a8 blocks&7 ahead."), 45, 0);
        ItemBlock other = ability("Test Blink", "RIGHT_CLICK", List.of("&7Teleport &a8 blocks&7 ahead."), 45, 0);
        List<ItemBlock> shown = ItemModifiers.blocks(tag(ItemModifiers.TUNERS, 4), List.of(blink, other));
        assertEquals(List.of("&7Teleport &a12 blocks&7 ahead."), shown.get(0).text());
        // Only a Transmission.
        assertEquals(List.of("&7Teleport &a8 blocks&7 ahead."), shown.get(1).text());
        assertEquals(4, ItemModifiers.tuners(tag(ItemModifiers.TUNERS, 4)));
    }

    @Test
    void howManyTunersAnItemTakes() {
        assertEquals(4, ItemModifiers.maxTuners(List.of(ability("Instant Transmission", "RIGHT_CLICK", List.of(), 0, 0))));
        // The Aspect of the Leech's takes one (the wiki's Transmission Tuner).
        assertEquals(1, ItemModifiers.maxTuners(List.of(ability("Weird Transmission", "RIGHT_CLICK", List.of(), 0, 0))));
        assertEquals(0, ItemModifiers.maxTuners(List.of(ability("Test Blink", "RIGHT_CLICK", List.of(), 0, 0))));
        assertEquals(0, ItemModifiers.maxTuners(List.of()));
    }

    @Test
    void aPowerScrollMarksTheRightClickAbilities() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(ItemModifiers.POWER_SCROLL, "SAPPHIRE_POWER_SCROLL");
        ItemBlock right = ability("Test Blink", "RIGHT_CLICK", List.of(), 45, 0);
        ItemBlock sneak = new ItemBlock("ABILITY", "Test Sneak", "&6Ability: Test Sneak  &e&lSNEAK RIGHT CLICK", "SHIFT_RIGHT_CLICK",
                List.of(), 0, 0, 0, 0, 0, 0, 0);
        List<ItemBlock> shown = ItemModifiers.blocks(tag, List.of(right, sneak));
        assertEquals("&b&l⦾ &6Ability: Test Blink  &e&lRIGHT CLICK", shown.get(0).header());
        assertEquals(sneak.header(), shown.get(1).header());
        // Nothing to change: the same list.
        List<ItemBlock> blocks = List.of(right);
        assertSame(blocks, ItemModifiers.blocks(new NBTTagCompound(), blocks));
    }

    @Test
    void manaDisintegrators() {
        NBTTagCompound ten = tag(ItemModifiers.MANA_DISINTEGRATORS, 10);
        assertEquals(0.9, ItemModifiers.manaFactor(ten), 1e-9);
        assertEquals(0.8, ItemModifiers.shareFactor(ten), 1e-9);
        // No more than 10 count.
        assertEquals(0.9, ItemModifiers.manaFactor(tag(ItemModifiers.MANA_DISINTEGRATORS, 30)), 1e-9);
        assertEquals(1, ItemModifiers.manaFactor(new NBTTagCompound()), 1e-9);

        // A share of max mana shows less, in the data's cost and in a Lantern's text; a mana cost gets the mark.
        ItemBlock orb = ability("Test Deploy", "RIGHT_CLICK", List.of("&7Place a test orb.", "&8Costs 50% of max mana."), 0, 50);
        ItemBlock shown = ItemModifiers.blocks(ten, List.of(orb)).getFirst();
        assertEquals(40, shown.manaPercent(), 1e-9);
        assertEquals(List.of("&7Place a test orb.", "&8Costs 40% of max mana."), shown.text());
        assertEquals(List.of("&6Ability: Test", "&8Mana Cost: &b300✎&8 (&910&9ᛃ&8)", "&8Mana Cost: &b40% of max", "&8Cooldown: &a20s"),
                ItemModifiers.blockLore(ten, List.of("&6Ability: Test", "&8Mana Cost: &b300✎", "&8Mana Cost: &b40% of max", "&8Cooldown: &a20s")));
        List<String> none = List.of("&8Mana Cost: &b300✎");
        assertSame(none, ItemModifiers.blockLore(new NBTTagCompound(), none));
    }

    @Test
    void aJalapenoBookEndsTheBuff() {
        List<String> buff = List.of("&5Orb Buff: Test", "&5• &7Heal yourself for &c5❤&7 per second.", "&5• &7Heal others for &c2❤&7 per",
                "&7second.", "", "&8Only one deployable buff applies.");
        assertEquals(List.of("&5Orb Buff: Test", "&5• &7Heal yourself for &c5❤&7 per second.", "&5• &7Heal others for &c2❤&7 per",
                "&7second.", "&5• &7Grants &9+5☠ Crit Damage&7. &a⒥", "&5• &7Grants &9+1☣ Crit Chance&7. &a⒥", "",
                "&8Only one deployable buff applies."), ItemModifiers.lore(tag(ItemModifiers.JALAPENO, 1), buff));
        assertSame(buff, ItemModifiers.lore(new NBTTagCompound(), buff));
        // No buff: as it was.
        List<String> text = List.of("&7A test item.");
        assertEquals(text, ItemModifiers.withJalapeno(text));
        assertEquals(new Stats().set(Stat.CRIT_DAMAGE, 5).set(Stat.CRIT_CHANCE, 1), ItemModifiers.jalapeno(tag(ItemModifiers.JALAPENO, 1)));
        assertEquals(new Stats(), ItemModifiers.jalapeno(new NBTTagCompound()));
    }

    @Test
    void aWoodSingularity() {
        NBTTagCompound tag = tag(ItemModifiers.WOOD_SINGULARITY, 1);
        assertEquals(new Stats().set(Stat.FORAGING_FORTUNE, 25), ItemModifiers.stats(tag));
        assertEquals(25, ItemModifiers.woodSingularity(tag, Stat.FORAGING_FORTUNE), 1e-9);
        assertEquals(0, ItemModifiers.woodSingularity(tag, Stat.STRENGTH), 1e-9);
        assertEquals(new Stats(), ItemModifiers.stats(new NBTTagCompound()));
    }

    @Test
    void masterStarsAreTheStarsPastFive() {
        assertEquals(0, ItemModifiers.masterStars(tag("upgrade_count", 5)));
        assertEquals(2, ItemModifiers.masterStars(tag("upgrade_count", 7)));
        assertEquals(5, ItemModifiers.masterStars(tag("upgrade_count", 12)));
        assertEquals(0, ItemModifiers.masterStars(new NBTTagCompound()));
    }

    @Test
    void efficiencyForSilex() {
        NBTTagCompound tag = new NBTTagCompound();
        assertEquals(0, ItemModifiers.efficiency(tag));
        ItemModifiers.withEfficiency(tag, 5);
        assertEquals(5, ItemModifiers.efficiency(tag));
        ItemModifiers.withEfficiency(tag, 6);
        assertEquals(6, ItemModifiers.efficiency(tag));
        // Once, as /addenchantment keeps it.
        assertEquals(1, tag.getList("enchantments", 10).size());
        assertEquals("efficiency", tag.getList("enchantments", 10).get(0).getString("name"));
    }

    @Test
    void enrichmentsByKey() {
        assertEquals(Enrichment.MAGIC_FIND, Enrichment.of("magic_find"));
        // A few live accessories have it in upper case.
        assertEquals(Enrichment.MAGIC_FIND, Enrichment.of("MAGIC_FIND"));
        assertNull(Enrichment.of(""));
        assertNull(Enrichment.of("swapper"));
        assertEquals("walk_speed", Enrichment.WALK_SPEED.key());
        assertEquals("TALISMAN_ENRICHMENT_WALK_SPEED", Enrichment.WALK_SPEED.itemId());
        assertEquals(Stat.SPEED, Enrichment.WALK_SPEED.stat());
        assertEquals(0.7, Enrichment.amount(List.of("&7Enriches a test with the", "&7power of &b+0.7✯ Magic Find &7when")), 1e-9);
        assertEquals(0, Enrichment.amount(List.of("&7No power here.")), 1e-9);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(ItemModifiers.ENRICHMENT, "critical_chance");
        assertEquals(List.of("&7&8Enriched with Crit Chance"), ItemModifiers.enrichmentLines(tag));
        assertEquals(List.of(), ItemModifiers.enrichmentLines(new NBTTagCompound()));
    }

    @Test
    void powerScrollsDoWhatTheirTextSays() {
        PowerScroll.Effect heal = PowerScroll.effect(List.of("&7Heal back &a3% &7of your missing &c❤", "&cHealth &7when using it. Has a &c4s",
                "&7cooldown."));
        assertEquals(0.03, heal.heal(), 1e-9);
        assertEquals(4000, heal.cooldown());
        assertEquals(new Stats(), heal.stats());
        PowerScroll.Effect mana = PowerScroll.effect(List.of("&7Gain &b+7✎ Mana &7when using it."));
        assertEquals(7, mana.mana(), 1e-9);
        assertEquals(0, mana.cooldown());
        PowerScroll.Effect buff = PowerScroll.effect(List.of("&7Gain &6+40⸕ Mining Speed &7for &a6s &7when using", "&7it."));
        assertEquals(new Stats().set(Stat.MINING_SPEED, 40), buff.stats());
        assertEquals(6000, buff.millis());
        // "❁" is Damage's too: the name says which.
        assertEquals(new Stats().set(Stat.STRENGTH, 9), PowerScroll.effect(List.of("&7Gain &c+9❁ Strength &7for &a2s &7now.")).stats());

        assertEquals(PowerScroll.RUBY, PowerScroll.of("ruby_power_scroll"));
        assertNull(PowerScroll.of("JADE_POWER_SCROLL"));
        assertEquals("&f", PowerScroll.OPAL.colour());
    }
}
