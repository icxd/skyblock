package net.icxd.dungeons.dungeons.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.DungeonClass;

/** The Class Details menu against RUN2's (00:57.6 to 01:04.3). */
class ClassDetailsTest {
    @Test
    void berserkTwentyAsRecorded() {
        ClassDetails.Item passives = ClassDetails.passives(DungeonClass.BERSERK, 20, 0);
        assertEquals("&aClass Passives", passives.name());
        assertEquals(List.of(
                "&8∙ &cClass Passive: Bloodlust",
                "&7Your next hit after killing a monster deals &c35%",
                "&7increased damage. This bonus will expire after &e5",
                "&7seconds. Reduces the cooldown remaining on",
                "&7Throwing Axe by &b1 &7second on activation. Heals",
                "&7you for &a3% &7of your missing health every hit.",
                "",
                "&8∙ &cClass Passive: Lust For Blood",
                "&7Every time you hit an enemy, increases the",
                "&7damage you deal to that enemy by &c75% &7up to a",
                "&7maximum of &a530%&7.",
                "",
                "&8∙ &cClass Passive: Indomitable",
                "&7Gain &c7% &7of your Strength as Defense.",
                "",
                "&8∙ &cClass Passive: Weapon Master",
                "&7Your melee weapons hit up to &c5 &7enemies within",
                "&7your swing range in a frontal cone. Increases",
                "&7your maximum swing range by &c3.2 &7blocks."), passives.lore());
        ClassDetails.Item orb = ClassDetails.orbAbilities(DungeonClass.BERSERK, 20);
        assertEquals("&aDungeon Orb Abilities", orb.name());
        assertEquals("&8∙ &6Ability: Throwing Axe  &e&lRIGHT CLICK", orb.lore().get(0));
        assertEquals("&8Cooldown: &a60s", orb.lore().get(orb.lore().size() - 1));
        assertEquals("&aGhost Abilities", ClassDetails.ghostAbilities(DungeonClass.BERSERK).name());
    }

    @Test
    void levelNumbersOfTheOtherClassesAsRecorded() {
        List<String> healer = ClassDetails.passives(DungeonClass.HEALER, 15, 0).lore();
        assertTrue(healer.contains("&7Grants &a1.6x  Mending&7, which increases your"));
        assertTrue(healer.contains("&7block radius for &a1.3% &7HP per second."));
        assertTrue(healer.contains("&7Vanishes for &e85 &7seconds after reviving a player."));
        assertTrue(healer.contains("&7the healing into an absorption shield up to &a26% &7of"));

        List<String> mage = ClassDetails.passives(DungeonClass.MAGE, 15, 930).lore();
        assertTrue(mage.contains("&7Those attacks deal &a113.7% &7of their melee damage"));
        assertTrue(mage.contains("&7All abilities have a &a32%&7 shorter cooldown."));

        List<String> archer = ClassDetails.passives(DungeonClass.ARCHER, 16, 0).lore();
        assertTrue(archer.contains("&a66% &7chance to shoot a second arrow."));
        assertTrue(archer.contains("&a32% &7chance for your arrows to bounce to an"));
        List<String> archerOrb = ClassDetails.orbAbilities(DungeonClass.ARCHER, 16).lore();
        assertTrue(archerOrb.contains("&8Cooldown: &a34s"));
        assertTrue(archerOrb.contains("&7Shoots &a5&7 Arrows per second for &a5"));

        assertTrue(ClassDetails.passives(DungeonClass.TANK, 14, 0).lore().contains("&7Grants &a1.3x  Defense&7."));
    }
}
