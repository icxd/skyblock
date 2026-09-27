package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.instance.puzzle.WeirdosRiddle.Weirdo;

/** The statements against the patterns the solver mods use on Hypixel's chat. */
class WeirdosRiddleTest {
    /** Skyblocker's ThreeWeirdos.PATTERN: the line of the one whose chest has the reward. */
    private static final Pattern SKYBLOCKER = Pattern.compile("^\\[NPC] ([A-Z][a-z]+): (?:The reward is(?: not in my chest!|n't in any of our chests\\.)|My chest (?:doesn't have the reward\\. We are all telling the truth\\.|has the reward and I'm telling the truth!)|At least one of them is lying, and the reward is not in [A-Z][a-z]+'s chest!|Both of them are telling the truth\\. Also, [A-Z][a-z]+ has the reward in their chest!)$");
    /** Odin's WeirdosSolver: the right one's statements and the wrong ones'. */
    private static final List<Pattern> ODIN_RIGHT = patterns("The reward is not in my chest!",
            "At least one of them is lying, and the reward is not in .+'s chest.?", "My chest doesn't have the reward. We are all telling the truth.?",
            "My chest has the reward and I'm telling the truth!", "The reward isn't in any of our chests.?",
            "Both of them are telling the truth. Also, .+ has the reward in their chest.?");
    private static final List<Pattern> ODIN_WRONG = patterns("One of us is telling the truth!",
            "They are both telling the truth. The reward isn't in .+'s chest.", "We are all telling the truth!",
            ".+ is telling the truth and the reward is in his chest.",
            "My chest doesn't have the reward. At least one of the others is telling the truth!", "One of the others is lying.",
            "They are both telling the truth, the reward is in .+'s chest.", "They are both lying, the reward is in my chest!",
            "The reward is in my chest.", "The reward is not in my chest. They are both lying.", ".+ is telling the truth.",
            "My chest has the reward.");

    private static List<Pattern> patterns(String... regexes) {
        return java.util.Arrays.stream(regexes).map(Pattern::compile).toList();
    }

    private static boolean any(List<Pattern> patterns, String text) {
        return patterns.stream().anyMatch(p -> p.matcher(text).matches());
    }

    @Test
    void everyCaseAsTheModsReadIt() {
        List<String> names = List.of("Baxter", "Hope", "Hugo");
        for (String[] lines : WeirdosRiddle.CASES) {
            for (long seed = 0; seed < 6; seed++) {
                List<Weirdo> weirdos = WeirdosRiddle.of(lines, names, new Random(seed));
                assertEquals(3, weirdos.size());
                assertEquals(1, weirdos.stream().filter(Weirdo::reward).count());
                assertEquals(Set.copyOf(names), new HashSet<>(weirdos.stream().map(Weirdo::name).toList()));
                for (Weirdo w : weirdos) {
                    String chat = "[NPC] " + w.name() + ": " + w.statement();
                    assertFalse(w.statement().contains("{"), w.statement());
                    assertEquals(w.reward(), SKYBLOCKER.matcher(chat).matches(), chat);
                    assertEquals(w.reward(), any(ODIN_RIGHT, w.statement()), w.statement());
                    assertEquals(!w.reward(), any(ODIN_WRONG, w.statement()), w.statement());
                }
            }
        }
    }

    /** Names in statements are the others' (a statement about "{R}" names the one with the reward). */
    @Test
    void namesAreFilledIn() {
        List<Weirdo> weirdos = WeirdosRiddle.of(WeirdosRiddle.CASES[0], List.of("Baxter", "Hope", "Hugo"), new Random(3));
        Weirdo third = weirdos.stream().filter(w -> w.name().equals("Hugo")).findFirst().orElseThrow();
        assertEquals("They are both telling the truth. The reward isn't in Baxter's chest.", third.statement());
        assertTrue(weirdos.stream().filter(Weirdo::reward).allMatch(w -> w.name().equals("Baxter")));
    }

    @Test
    void rollsThreeDifferentNames() {
        for (long seed = 0; seed < 50; seed++) {
            List<Weirdo> weirdos = WeirdosRiddle.roll(new Random(seed));
            assertEquals(3, new HashSet<>(weirdos.stream().map(Weirdo::name).toList()).size());
            assertTrue(weirdos.stream().allMatch(w -> WeirdosRiddle.NAMES.contains(w.name())));
        }
        assertTrue(WeirdosRiddle.NAMES.containsAll(List.of("Baxter", "Hope")));
    }

    @Test
    void chatLine() {
        assertEquals("&e[NPC] &cHope&f: The reward isn't in any of our chests.",
                WeirdosRiddle.line(new Weirdo("Hope", "The reward isn't in any of our chests.", true)));
    }
}
