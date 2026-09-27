package net.icxd.dungeons.dungeons.instance;

import net.icxd.dungeons.common.DungeonFloor;

/**
 * A run's score: skill, exploration, speed and bonus, out of 300 plus bonus. Hypixel's formulas
 * (research score_rewards.md 1.2-1.4, which reproduce all four recorded Entrance numbers: chat 182
 * and 109, cards 189 and 116). "Rooms" are map cells, and the Entrance room isn't one of them. On
 * the Entrance each part is rounded after taking 70% of it, so its best is 214 (B).
 *
 * @param skill   20 plus 80 for clearing every room, minus 2 per death (1 for the first if it had a
 *                Spirit pet) and 10 per puzzle failed or never found
 * @param explore 60 for clearing every room plus 40 for the share of secrets the floor asks for
 * @param speed   100 until the floor's time limit, then less and less
 * @param bonus   a point per crypt (up to 5), 2 for the mimic, 10 with Paul's EZPZ perk
 */
public record Score(int skill, int explore, int speed, int bonus) {
    /**
     * What the score is worked out from. Rooms are cells, not counting the Entrance room's (both
     * recorded Entrances: 16 cells, 15 counted).
     */
    public record Inputs(int completedRooms, int totalRooms, int secretsFound, int totalSecrets, int deaths,
                         boolean firstDeathHadSpirit, int puzzlesNotDone, int crypts, boolean mimicKilled,
                         boolean paul, double seconds) {
        /** The team's share of the floor's secrets, in percent (the tab's "Secrets Found: 19%"). */
        public double secretPercent() {
            return totalSecrets > 0 ? 100.0 * secretsFound / totalSecrets : 0;
        }
    }

    public int total() {
        return skill + explore + speed + bonus;
    }

    /** D, C, B, A, S or S+ (the mods' whole-number thresholds; the wiki's "269.5" wording is unverified). */
    public String grade() {
        int total = total();
        if (total < 100) return "D";
        if (total < 160) return "C";
        if (total < 230) return "B";
        if (total < 270) return "A";
        if (total < 300) return "S";
        return "S+";
    }

    public static Score of(DungeonFloor floor, Inputs in) {
        int skill = skill(in);
        int explore = rooms(60, in) + secrets(floor, in);
        int speed = speed(floor, in.seconds());
        int bonus = bonus(in);
        if (floor.getNumber() != 0) return new Score(skill, explore, speed, bonus);
        return new Score(entrance(skill), entrance(explore), entrance(speed), entrance(bonus));
    }

    /**
     * The number in brackets after "Cleared:" on the sidebar while the run goes on: not the final
     * score and not reduced on the Entrance, but skill without its 20, exploration, the cleared
     * share in place of speed, the bonus, and something for the Watcher's undead killed. Fitted to
     * 17 of the 21 recorded values exactly (research score_rewards.md 1.6); the Watcher's part is
     * fitted too (see {@link #watcherPart}).
     */
    public static int indicator(DungeonFloor floor, Inputs in, int watcherKills) {
        int skill = Math.max(0, rooms(80, in) - 10 * in.puzzlesNotDone() - 2 * in.deaths());
        return skill + rooms(60, in) + secrets(floor, in) + cleared(in.completedRooms(), in.totalRooms()) + bonus(in)
                + watcherPart(watcherKills);
    }

    /**
     * The indicator's Watcher part. UNKNOWN on Hypixel: 2.2 per undead killed, rounded down, fits the
     * four recorded values (3, 4 and 8 killed gave 6, 8 and 17; all 9 gave 19). Any rate from 2.125
     * up to 2.22 fits them as well.
     */
    static int watcherPart(int kills) {
        return kills * 11 / 5;
    }

    /** Share of the floor's rooms done, in percent, to the nearest (the sidebar's 13/15 is 87%). */
    public static int cleared(int completed, int total) {
        if (total <= 0) return 0;
        return (int) ((200L * Math.min(completed, total) + total) / (2L * total));
    }

    /**
     * The colour of the sidebar's cleared share: red up to 33%, gold from 40% to 67% and green at 87%
     * and 93% as recorded. Where it turns gold (34-40%) and green (68-87%) is UNKNOWN; 40 and 80 are guesses.
     */
    public static String clearedColor(int percent) {
        if (percent < 40) return "&c";
        if (percent < 80) return "&6";
        return "&a";
    }

    /** B yellow, C gold and D red as in Hypixel's end-of-run messages; A, S and S+ are UNKNOWN (guesses). */
    public static String gradeColor(String grade) {
        return switch (grade) {
            case "S+" -> "&6";
            case "S" -> "&e";
            case "A" -> "&5";
            case "B" -> "&e";
            case "C" -> "&6";
            default -> "&c";
        };
    }

    /** 70% of a part, to the nearest (Hypixel rounds each part on the Entrance; half up, no recorded .5 case). */
    static int entrance(int part) {
        return (7 * Math.max(0, part) + 5) / 10;
    }

    private static int skill(Inputs in) {
        int deathPenalty = 2 * in.deaths() - (in.deaths() > 0 && in.firstDeathHadSpirit() ? 1 : 0);
        return 20 + Math.clamp(rooms(80, in) - 10 * in.puzzlesNotDone() - deathPenalty, 0, 80);
    }

    /** {@code points} for all the rooms, rounded down. */
    private static int rooms(int points, Inputs in) {
        if (in.totalRooms() <= 0) return 0;
        return (int) ((long) points * Math.min(in.completedRooms(), in.totalRooms()) / in.totalRooms());
    }

    /**
     * 40 for the share of secrets the floor asks for, by the percentage found (Hypixel's; the mods'
     * "found out of the secrets needed" gives 180 for the recorded 182).
     */
    private static int secrets(DungeonFloor floor, Inputs in) {
        if (in.totalSecrets() <= 0) return 0;
        long points = 4000L * Math.min(in.secretsFound(), in.totalSecrets()) / ((long) in.totalSecrets() * secretsNeeded(floor));
        return (int) Math.min(40, points);
    }

    private static int bonus(Inputs in) {
        return Math.min(Math.max(0, in.crypts()), 5) + (in.mimicKilled() ? 2 : 0) + (in.paul() ? 10 : 0);
    }

    /**
     * 100 until the floor's time limit, then the wiki's curve (research score_rewards.md 1.2). Which of
     * the wiki's and the mods' curves Hypixel uses is UNKNOWN; they differ by a point here and there.
     */
    static int speed(DungeonFloor floor, double seconds) {
        double t = seconds + 480 - timeLimit(floor);
        if (t < 480) return 100;
        if (t < 600) return (int) Math.ceil(140 - t / 12);
        if (t < 840) return (int) Math.ceil(115 - t / 24);
        if (t < 1140) return (int) Math.ceil(108 - t / 30);
        if (t < 3940) return (int) Math.ceil(98.5 - t / 40);
        return 0;
    }

    /** The share of a floor's secrets that counts as all of them, in percent (the Entrance's is the mods'). */
    static int secretsNeeded(DungeonFloor floor) {
        if (floor.isMasterMode()) return 100;
        return switch (floor.getNumber()) {
            case 0, 1 -> 30;
            case 2 -> 40;
            case 3 -> 50;
            case 4 -> 60;
            case 5 -> 70;
            case 6 -> 85;
            default -> 100;
        };
    }

    /**
     * Seconds before the speed score starts dropping (FW/MCW Dungeon Score). The Entrance's isn't in
     * any official source: 20 minutes as the mods have it (one forum screenshot suggests about 28).
     */
    static int timeLimit(DungeonFloor floor) {
        if (floor.isMasterMode()) {
            return switch (floor.getNumber()) {
                case 6 -> 10 * 60;
                case 7 -> 14 * 60;
                default -> 8 * 60;
            };
        }
        return switch (floor.getNumber()) {
            case 0 -> 20 * 60;
            case 4, 6 -> 12 * 60;
            case 7 -> 14 * 60;
            default -> 10 * 60;
        };
    }
}
