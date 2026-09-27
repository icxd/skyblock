package net.icxd.dungeons.dungeons.instance;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonClass;

/**
 * What finishing a floor gives each member: Catacombs experience, class experience and Bits
 * (research score_rewards.md 4). The structure is sourced: a floor's base at a 300 score, scaled
 * linearly by the chat score, 300 more score for the first five completions of a floor, 40% more
 * for the first five runs of a day. The multipliers' constants come from community
 * reverse-engineering (the forum thread "How is Dungeoneering XP actually calculated?", 2025) and
 * are UNVERIFIED: the recorded runs fit the structure, but the player's own bonuses are unknown.
 * No mayors, Hecatomb, rings, essence perks or global boosts here, so those are all 1.
 */
final class RunRewards {
    /** 40% more for the first five completed runs of each day (official changelog, 2021-12-17). */
    static final double DAILY_BONUS = 1.4;
    static final int DAILY_RUNS = 5;
    /** 300 more score for the first five completions of a floor (changelog 2021-12-17; forum thread). */
    static final int FIRST_COMPLETIONS = 5;
    static final int FIRST_COMPLETION_SCORE = 300;
    /** UNVERIFIED (the forum thread's constant): class experience before anything else multiplies it. */
    static final double CLASS_BASE = 1.1;
    /**
     * UNVERIFIED: each member also gets a quarter of their class experience in each other class in
     * the party, as a team bonus (a community calculator's assumption; Hypixel gives no number).
     */
    static final double TEAM_SHARE = 0.25;

    private RunRewards() {
    }

    /** Catacombs experience, each class's (the member's own first) and Bits. */
    record Reward(double catacombs, Map<DungeonClass, Double> classes, int bits) {
    }

    /**
     * One member's reward.
     *
     * @param score             the chat's Team Score (experience follows it, not the card's: research 4.3)
     * @param completionsBefore their completions of this floor before this one
     * @param runsToday         runs they completed today before this one (any floor)
     * @param secretPercent     the team's share of the floor's secrets
     * @param own               their class
     * @param teammates         the others' classes
     */
    static Reward reward(DungeonFloor floor, int score, int completionsBefore, int runsToday, double secretPercent,
                         DungeonClass own, Collection<DungeonClass> teammates) {
        double daily = runsToday < DAILY_RUNS ? DAILY_BONUS : 1;
        double catacombs = round(catacombsXp(floor, score, completionsBefore, secretPercent) * daily);
        double classXp = classXp(floor, score, completionsBefore) * daily;
        Map<DungeonClass, Double> classes = new LinkedHashMap<>();
        classes.put(own, round(classXp));
        Set<DungeonClass> others = teammates.isEmpty() ? EnumSet.noneOf(DungeonClass.class) : EnumSet.copyOf(teammates);
        others.remove(own);
        for (DungeonClass other : others) classes.put(other, round(classXp * TEAM_SHARE));
        return new Reward(catacombs, classes, bits(floor));
    }

    /**
     * Catacombs experience before the daily bonus: the floor's base, times the repeat-completion
     * ("frequent flier") factor, the score multiplier and, up to Floor VI, 0.5% for every percent of
     * secrets over the floor's requirement (changelog 2021-12-17). The frequent flier factor is the
     * forum thread's, UNVERIFIED: {@code 1.1 + 0.02(runs-1) + (runs-1)/1000 - 0.05}, where runs are the
     * completions before this one (so the factor lags one run, as the thread says), capped at 26 on
     * Floor VII, 51 on Floor VI and 76 below.
     */
    static double catacombsXp(DungeonFloor floor, int score, int completionsBefore, double secretPercent) {
        int runs = Math.min(Math.max(0, completionsBefore), maxRuns(floor));
        double frequentFlier = 1.1 + 0.02 * (runs - 1) + (runs - 1) / 1000.0 - 0.05;
        double overExploration = 1;
        if (!floor.isMasterMode() && floor.getNumber() <= 6) {
            overExploration += 0.005 * Math.max(0, secretPercent - Score.secretsNeeded(floor));
        }
        return baseXp(floor) * frequentFlier * scoreMultiplier(score, completionsBefore) * overExploration;
    }

    /** Class experience before the daily bonus; repeat completions don't change it (forum thread). */
    static double classXp(DungeonFloor floor, int score, int completionsBefore) {
        return baseXp(floor) * CLASS_BASE * scoreMultiplier(score, completionsBefore);
    }

    /**
     * Experience scales linearly with the score, out of 300, and the first five completions of a floor
     * count 300 more (recorded: it fits both the Catacombs and the Berserk experience of R1, research 4.3).
     */
    static double scoreMultiplier(int score, int completionsBefore) {
        int counted = Math.max(0, score) + (completionsBefore < FIRST_COMPLETIONS ? FIRST_COMPLETION_SCORE : 0);
        return counted / 300.0;
    }

    /**
     * A floor's experience at a 300 score, from the archived wiki calculator (research score_rewards.md
     * 4.2: the Entrance 55; the wiki's infobox numbers don't fit the forum's data).
     */
    static double baseXp(DungeonFloor floor) {
        if (floor.isMasterMode()) {
            return switch (floor.getNumber()) {
                case 1 -> 15_000;
                case 2 -> 20_000;
                case 3 -> 35_000;
                case 4 -> 55_000;
                case 5 -> 70_000;
                case 6 -> 100_000;
                default -> 300_000;
            };
        }
        return switch (floor.getNumber()) {
            case 0 -> 55;
            case 1 -> 110;
            case 2 -> 220;
            case 3 -> 560;
            case 4 -> 1_420;
            case 5 -> 2_400;
            case 6 -> 4_880;
            default -> 28_000;
        };
    }

    private static int maxRuns(DungeonFloor floor) {
        return switch (floor.getNumber()) {
            case 7 -> 26;
            case 6 -> 51;
            default -> 76;
        };
    }

    /**
     * Bits for finishing a floor (FW Bits "Earning"). Hypixel multiplies them by the player's Bits
     * multiplier and takes them from a Booster Cookie's pool; there are neither here.
     */
    static int bits(DungeonFloor floor) {
        int n = floor.getNumber();
        if (floor.isMasterMode()) return new int[]{0, 10, 14, 18, 22, 26, 36, 100}[n];
        return new int[]{3, 5, 7, 9, 12, 15, 20, 25}[n];
    }

    /** To a tenth, as the chat shows it (and as it's saved). */
    static double round(double xp) {
        return Math.round(xp * 10) / 10.0;
    }

    /** "131.1", "33", "1,001,246.4": a decimal unless it's .0, thousands separated (research 2.1). */
    static String format(double xp) {
        String text = String.format(Locale.ROOT, "%,.1f", round(xp));
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }
}
