package net.icxd.dungeons.pet.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** What the gathering pets' stats from other stats come to, by the wiki's rules for each. */
class GatheringAbilitiesTest {
    /** The stats once this pet's ability, with these numbers, has worked them out from the rest. */
    private static Stats derived(String pet, String name, Stats stats, double... numbers) {
        PetAbility ability = PetAbilities.all().stream().filter(a -> a.pet().equals(pet) && a.name().equals(name)).findFirst().orElseThrow();
        ability.derivedStats(null, new PetAbility.Active(ability, null, 100, numbers), stats);
        return stats;
    }

    @Test
    void snailTakesTheSpeedItConverts() {
        // Every 2 Speed above 100 as 1 Block Fortune, past the speed cap too.
        Stats stats = derived("SNAIL", "Slow and Steady", new Stats().set(Stat.SPEED, 505), 2, 100, 1);
        assertEquals(202, stats.get(Stat.BLOCK_FORTUNE), 1e-9);
        assertEquals(101, stats.get(Stat.SPEED), 1e-9, "what's left over");
    }

    @Test
    void mantisKeepsTheSpeedUpToTheCap() {
        Stats stats = derived("ORCHID_MANTIS", "Swift Sickles", new Stats().set(Stat.SPEED, 500), 3, 100, 1);
        assertEquals(100, stats.get(Stat.FARMING_FORTUNE), 1e-9, "(400 - 100) / 3");
        assertEquals(500, stats.get(Stat.SPEED), 1e-9);
        assertEquals(0, derived("ORCHID_MANTIS", "Swift Sickles", new Stats().set(Stat.SPEED, 102), 3, 100, 1).get(Stat.FARMING_FORTUNE), 1e-9);
    }

    @Test
    void mooshroomRoundsDownOnce() {
        // The wiki's 1,780.7 Strength: ⌊1780.7 / 20 × 0.7⌋ = 62, not 0.7 for each whole 20 (62.3).
        Stats stats = derived("MOOSHROOM_COW", "Farming Strength", new Stats().set(Stat.STRENGTH, 1780.7), 0.7, 20);
        assertEquals(62, stats.get(Stat.FARMING_FORTUNE), 1e-9);
    }

    @Test
    void elephantForEachWhole() {
        Stats stats = derived("ELEPHANT", "Stomp", new Stats().set(Stat.SPEED, 450), 20, 100);
        assertEquals(80, stats.get(Stat.DEFENSE), 1e-9);
    }
}
