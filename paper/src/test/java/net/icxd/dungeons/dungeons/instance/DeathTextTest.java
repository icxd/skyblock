package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** The death and revive lines against the patterns the mods match Hypixel's with. */
class DeathTextTest {
    /** Skytils' death pattern, with & for §. */
    private static final Pattern SKYTILS_DEATH =
            Pattern.compile("&c ☠ &7(?:You were |(?:&.)+(?<username>\\w+)&r)(?<reason>.*) and became a ghost&7\\.");
    /** SkyHanni's, on the text without colours. */
    private static final Pattern SKYHANNI_DEATH =
            Pattern.compile(" ☠ .+(?:(?:crush|di(?:sconnect)?|kill|suffocat)ed|fell|burnt).+became a ghost\\.");

    private static String plain(String legacy) {
        return legacy.replaceAll("&.", "");
    }

    @Test
    void yourOwnDeath() {
        String line = DeathText.death(DeathText.Reason.KILLED_BY, "Zombie Grunt", true, "&bAlice");
        assertEquals("&c ☠ &7You were killed by Zombie Grunt and became a ghost&7.", line);
        assertTrue(SKYTILS_DEATH.matcher(line).matches());
        assertTrue(SKYHANNI_DEATH.matcher(plain(line)).matches());
    }

    @Test
    void someoneElsesDeath() {
        // The name in its rank's colour, without the rank: the mods' patterns take colour codes, then the name.
        String line = DeathText.death(DeathText.Reason.TRAP, null, false, "&bAlice");
        assertEquals("&c ☠ &7&bAlice&r&7 died to a trap and became a ghost&7.", line);
        var match = SKYTILS_DEATH.matcher(line);
        assertTrue(match.matches());
        assertEquals("Alice", match.group("username"));
        assertTrue(SKYHANNI_DEATH.matcher(plain(line)).matches());
    }

    @Test
    void everyReasonReadsAsSkyHanniExpects() {
        for (DeathText.Reason reason : DeathText.Reason.values()) {
            assertTrue(SKYHANNI_DEATH.matcher(plain(DeathText.death(reason, "Tank Zombie", false, "&7Bob"))).matches(), reason::name);
        }
        assertEquals(" ☠ Bob fell to their death with help from Tank Zombie and became a ghost.",
                plain(DeathText.death(DeathText.Reason.FELL_WITH_HELP, "Tank Zombie", false, "&7Bob")));
        assertEquals(" ☠ Bob disconnected from the Dungeon and became a ghost.",
                plain(DeathText.death(DeathText.Reason.DISCONNECTED, null, false, "&7Bob")));
    }

    @Test
    void reasons() {
        assertEquals(DeathText.Reason.KILLED_BY, DeathText.reason("generic", true, false, true));
        assertEquals(DeathText.Reason.TRAP, DeathText.reason("generic", true, true, true));
        assertEquals(DeathText.Reason.OTHER, DeathText.reason("generic", false, false, false));
        assertEquals(DeathText.Reason.FELL_WITH_HELP, DeathText.reason("fall", false, false, true));
        assertEquals(DeathText.Reason.OTHER, DeathText.reason("fall", false, false, false));
        assertEquals(DeathText.Reason.DEEP_HOLE, DeathText.reason("out_of_world", false, false, false));
        assertEquals(DeathText.Reason.BURNT, DeathText.reason("lava", false, false, false));
        assertEquals(DeathText.Reason.SUFFOCATED, DeathText.reason("in_wall", false, false, false));
        assertEquals(DeathText.Reason.CRUSHED, DeathText.reason("falling_anvil", false, false, false));
    }

    @Test
    void aKillWithNobodyToNameIsAMobs() {
        assertEquals(" ☠ You died to a mob and became a ghost.", plain(DeathText.death(DeathText.Reason.KILLED_BY, null, true, "&7Bob")));
        assertEquals(" ☠ Bob died and became a ghost.", plain(DeathText.death(DeathText.Reason.FELL_WITH_HELP, null, false, "&7Bob")));
    }

    @Test
    void outsideADungeon() {
        // SkyHanni's PlayerDeathManager, "§c ☠ §r§7§r§.(?<name>.+)§r§7 (?<reason>.+)", here without its resets.
        Pattern seen = Pattern.compile("&c ☠ &7&.(?<name>.+)&r&7 (?<reason>.+)");
        String other = DeathText.outsideDeath(DeathText.Reason.KILLED_BY, "Ashfang", false, "&bZeroHazel");
        assertEquals("&c ☠ &7&bZeroHazel&r&7 was killed by &cAshfang&r&7&7.", other);
        assertTrue(seen.matcher(other).matches());
        assertEquals(" ☠ ZeroHazel was killed by Ashfang.", plain(other));
        assertEquals(" ☠ You were killed by Graveyard Zombie.",
                plain(DeathText.outsideDeath(DeathText.Reason.KILLED_BY, "Graveyard Zombie", true, null)));
        assertEquals("&c ☠ &7You fell into the void&7.", DeathText.outsideDeath(DeathText.Reason.VOID, null, true, null));
        assertEquals(" ☠ Bob fell into the void.", plain(DeathText.outsideDeath(DeathText.Reason.VOID, null, false, "&7Bob")));
        assertEquals(" ☠ You died to a mob.", plain(DeathText.outsideDeath(DeathText.Reason.KILLED_BY, null, true, null)));
        // Out of the world is the void here, a deep hole in a dungeon; a hit is a kill either way, and there are no traps.
        assertEquals(DeathText.Reason.VOID, DeathText.outside("out_of_world", false, true));
        assertEquals(DeathText.Reason.KILLED_BY, DeathText.outside("generic", true, false));
        assertEquals(DeathText.Reason.BURNT, DeathText.outside("lava", false, false));
        assertEquals(DeathText.Reason.FELL_WITH_HELP, DeathText.outside("fall", false, true));
    }

    @Test
    void reviveAndReconnect() {
        // Skytils' "^§r§a ❣ §r§7(?:§.)+(?<username>\w+)§r§a was revived" and BetterMap's "&r&a ❣ &r${info} was revived${*}!&r".
        assertTrue(Pattern.compile("&a ❣ &7(?:&.)+(?<username>\\w+)&r&a was revived.*!").matcher(DeathText.revived("&bAlice")).matches());
        // Skytils' "§r§c ☠ §r§7(?:§.)+(?<username>\w+) §r§7reconnected§r§7.§r".
        assertTrue(Pattern.compile("&c ☠ &7(?:&.)+(?<username>\\w+) &7reconnected&7\\.").matcher(DeathText.reconnected("&bAlice")).matches());
    }

    @Test
    void fairies() {
        assertEquals("&dMari the Fairy&f: You killed me! Take this &6Revive Stone &fso that my death is not in vain!",
                DeathText.fairyGivesStone("Mari"));
        assertEquals("&dZana the Fairy&f: You killed me! I'll revive your friend &r&bAlice &fso that my death is not in vain!",
                DeathText.fairyRevivesFriend("Zana", "&bAlice"));
    }
}
