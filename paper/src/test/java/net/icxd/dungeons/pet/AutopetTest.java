package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bson.Document;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.item.enums.Rarity;

/** Autopet's rules: their words (the wiki's), their exceptions, and as the profile keeps them. */
class AutopetTest {
    private static final PetData DATA = PetFixtures.fixture();

    private static Autopet.Rule rule(Autopet.Trigger trigger, String arg, Autopet.Except... exceptions) {
        return new Autopet.Rule("uuid", trigger, arg, List.of(exceptions));
    }

    @Test
    void theirWords() {
        assertEquals("On gain Farming XP", rule(Autopet.Trigger.SKILL_XP, "FARMING").when(DATA));
        assertEquals("On enter Dwarven Mines", rule(Autopet.Trigger.ENTER_ISLAND, "DWARVEN_MINES").when(DATA));
        assertEquals("On kill a Iron Golem", rule(Autopet.Trigger.KILL_MOB, "IRON_GOLEM").when(DATA));
        assertEquals("When you enter Catacombs Floor VII", rule(Autopet.Trigger.ENTER_FLOOR, "7").when(DATA));
        assertEquals("On login", rule(Autopet.Trigger.LOGIN, null).when(DATA));
        assertEquals("Except if in Hub", new Autopet.Except(Autopet.Unless.IN_ISLAND, "LOBBY").text(DATA));
        assertEquals("Except if NOT in Private Island", new Autopet.Except(Autopet.Unless.NOT_IN_ISLAND, "ISLAND").text(DATA));
        assertEquals("Except if already has Bee equipped", new Autopet.Except(Autopet.Unless.HAS_PET, "BEE").text(DATA));
    }

    @Test
    void theirExceptions() {
        OwnedPet bee = OwnedPet.of("BEE", Rarity.COMMON);
        assertTrue(Autopet.stopped(rule(Autopet.Trigger.LOGIN, null, new Autopet.Except(Autopet.Unless.IN_ISLAND, "LOBBY")), ServerType.LOBBY, null));
        assertFalse(Autopet.stopped(rule(Autopet.Trigger.LOGIN, null, new Autopet.Except(Autopet.Unless.IN_ISLAND, "LOBBY")), ServerType.ISLAND, null));
        assertTrue(Autopet.stopped(rule(Autopet.Trigger.LOGIN, null, new Autopet.Except(Autopet.Unless.NOT_IN_ISLAND, "LOBBY")), ServerType.ISLAND, null));
        assertTrue(Autopet.stopped(rule(Autopet.Trigger.LOGIN, null, new Autopet.Except(Autopet.Unless.HAS_PET, "BEE")), ServerType.LOBBY, bee));
        // Two "NOT in"s cancel each other out (the wiki's): it's never let through.
        Autopet.Rule both = rule(Autopet.Trigger.LOGIN, null, new Autopet.Except(Autopet.Unless.NOT_IN_ISLAND, "LOBBY"),
                new Autopet.Except(Autopet.Unless.NOT_IN_ISLAND, "ISLAND"));
        assertTrue(Autopet.stopped(both, ServerType.LOBBY, null));
        assertTrue(Autopet.stopped(both, ServerType.ISLAND, null));
    }

    @Test
    void keptWithTheProfile() {
        Document profile = new Document();
        assertEquals(0, Autopet.slots(profile));
        for (int i = 0; i < 20; i++) Autopet.addSlots(profile);
        assertEquals(28, Autopet.slots(profile), "28 at most");
        List<Autopet.Rule> rules = List.of(rule(Autopet.Trigger.SKILL_XP, "COMBAT", new Autopet.Except(Autopet.Unless.IN_ISLAND, "LOBBY")),
                rule(Autopet.Trigger.FISHING_HOOK, null));
        Autopet.put(profile, rules);
        assertEquals(rules, Autopet.rules(profile));
    }

    @Test
    void stepping() {
        List<String> choices = List.of("A", "B", "C");
        assertEquals("B", AutopetRuleMenu.step(choices, "A", ClickType.LEFT));
        assertEquals("C", AutopetRuleMenu.step(choices, "A", ClickType.RIGHT));
        assertEquals("B", AutopetRuleMenu.step(choices, "A", ClickType.SHIFT_LEFT), "ten on, around");
        assertEquals("A", AutopetRuleMenu.step(choices, null, ClickType.LEFT), "from none, the first");
        assertEquals("C", AutopetRuleMenu.step(choices, "Z", ClickType.RIGHT), "from one that's gone, back to the last");
    }

    @Test
    void itsLines() {
        OwnedPet bee = OwnedPet.of("BEE", Rarity.COMMON);
        assertEquals("&7Equip: &7[Lvl 1] &fBee", Autopet.hover(bee, DATA));
        assertEquals(List.of("&7Equip: &7[Lvl 1] &fBee", "&7When: &eOn gain Farming XP", "&7Except if&c in Hub"),
                AutopetMenu.lines(rule(Autopet.Trigger.SKILL_XP, "FARMING", new Autopet.Except(Autopet.Unless.IN_ISLAND, "LOBBY")), bee, DATA));
    }
}
