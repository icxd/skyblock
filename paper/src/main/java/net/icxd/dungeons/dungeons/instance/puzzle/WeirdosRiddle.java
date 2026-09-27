package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * What the Three Weirdos say. One of the six sets of statements the wiki lists (the Catacombs puzzle
 * rooms page and the forum's "3 associates" guide, which agree), worded as it is in game where the
 * solver mods match it (Skyblocker's ThreeWeirdos pattern, Odin's WeirdosSolver lists); three names
 * and who stands where are random.
 */
public final class WeirdosRiddle {
    /**
     * The weirdos' names. Only Baxter and Hope are confirmed (Skyblocker's tests use lines from them);
     * the rest are the list of the RustClear recreation (github.com/Big-Dungeons/RustClear), which fits
     * both: UNVERIFIED.
     */
    static final List<String> NAMES = List.of("Ardis", "Baxter", "Benson", "Carver", "Elmo", "Eveleth", "Hope", "Hugo",
            "Lino", "Luverne", "Madelia", "Marshall", "Melrose", "Montgomery", "Morris", "Ramsey", "Rose", "Victoria",
            "Virginia", "Willmar", "Winona");

    /**
     * The six cases: what the one with the reward (R) says, then the other two (A, B). {R}, {A} and {B}
     * are their names.
     */
    static final String[][] CASES = {
            {"The reward is not in my chest!",
                    "One of us is telling the truth!",
                    "They are both telling the truth. The reward isn't in {R}'s chest."},
            {"At least one of them is lying, and the reward is not in {B}'s chest!",
                    "We are all telling the truth!",
                    "{A} is telling the truth and the reward is in his chest."},
            {"My chest doesn't have the reward. We are all telling the truth.",
                    "My chest doesn't have the reward. At least one of the others is telling the truth!",
                    "One of the others is lying."},
            {"My chest has the reward and I'm telling the truth!",
                    "They are both telling the truth, the reward is in {B}'s chest.",
                    "They are both lying, the reward is in my chest!"},
            {"The reward isn't in any of our chests.",
                    "The reward is in my chest.",
                    "The reward is not in my chest. They are both lying."},
            {"Both of them are telling the truth. Also, {A} has the reward in their chest!",
                    "{B} is telling the truth.",
                    "My chest has the reward."},
    };

    /** One of them: their name, what they say, and whether their chest has the reward. */
    public record Weirdo(String name, String statement, boolean reward) {
    }

    private WeirdosRiddle() {
    }

    /** Three weirdos, in the order of the places they stand in. */
    public static List<Weirdo> roll(Random random) {
        List<String> names = new ArrayList<>(NAMES);
        Collections.shuffle(names, random);
        return of(CASES[random.nextInt(CASES.length)], names.subList(0, 3), random);
    }

    /** A case said by these three (R, A, B), put in the places at random. */
    static List<Weirdo> of(String[] lines, List<String> names, Random random) {
        List<Weirdo> weirdos = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String line = lines[i].replace("{R}", names.get(0)).replace("{A}", names.get(1)).replace("{B}", names.get(2));
            weirdos.add(new Weirdo(names.get(i), line, i == 0));
        }
        Collections.shuffle(weirdos, random);
        return weirdos;
    }

    /** The chat line when someone clicks them. */
    public static String line(Weirdo weirdo) {
        return "&e[NPC] &c" + weirdo.name() + "&f: " + weirdo.statement();
    }
}
