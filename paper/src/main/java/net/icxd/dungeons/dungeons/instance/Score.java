package net.icxd.dungeons.dungeons.instance;

import net.icxd.dungeons.common.DungeonFloor;

/**
 * A run's score: skill, exploration, speed and bonus, out of 300 plus bonus. The formulas are the
 * ones the mods use to predict Hypixel's (Skytils' {@code ScoreCalculation}); on the Entrance every
 * part counts 70%. Hypixel's own numbers don't always agree with them, so expect this to change.
 *
 * @param skill   20 plus 80 for clearing every room, minus 2 per death (1 for the first if it had a
 *                Spirit pet) and 10 per puzzle failed or not done
 * @param explore 60 for clearing every room plus 40 for the secrets the floor asks for
 * @param speed   100 until the floor's time limit, then less and less
 * @param bonus   a point per crypt (up to 5), 2 for the mimic, 10 with Paul's EZPZ perk
 */
public record Score(int skill, int explore, int speed, int bonus) {
    /** What the score is worked out from. */
    public record Inputs(int completedRooms, int totalRooms, int secretsFound, int totalSecrets, int deaths,
                         boolean firstDeathHadSpirit, int puzzlesNotDone, int crypts, boolean mimicKilled,
                         boolean paul, double seconds) {
    }

    public int total() {
        return skill + explore + speed + bonus;
    }

    /** D, C, B, A, S or S+. */
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
        boolean entrance = floor.getNumber() == 0;
        double scale = entrance ? 0.7 : 1;
        double cleared = in.totalRooms() > 0 ? Math.min(1, in.completedRooms() / (double) in.totalRooms()) : 0;

        int deathPenalty = 2 * in.deaths() - (in.deaths() > 0 && in.firstDeathHadSpirit() ? 1 : 0);
        int skill = (int) ((20 + cleared * 80 - deathPenalty - 10 * in.puzzlesNotDone()) * scale);
        skill = Math.clamp(skill, (int) (20 * scale), (int) (100 * scale));

        double roomScore = 60 * cleared;
        double secretScore = 0;
        if (in.totalSecrets() > 0) {
            int needed = (int) Math.ceil(in.totalSecrets() * secretsNeeded(floor));
            secretScore = Math.clamp(40.0 * in.secretsFound() / needed, 0, 40);
        }
        int explore = (int) (roomScore * scale) + (int) (secretScore * scale);

        int speed = (int) (speed(in.seconds() + 480 - timeLimit(floor)) * scale);

        int bonus = Math.min(in.crypts(), 5) + (in.mimicKilled() ? 2 : 0) + (in.paul() ? 10 : 0);
        if (entrance) bonus = (int) Math.ceil(bonus * 0.7);
        return new Score(skill, explore, speed, bonus);
    }

    /** Past the time limit, Hypixel takes points off slowly and then faster (the mods' curve). */
    private static double speed(double t) {
        if (t < 492) return 100;
        if (t < 600) return 140 - t / 12;
        if (t < 840) return 115 - t / 24;
        if (t < 1140) return 108 - t / 30;
        if (t < 3570) return 98.5 - t / 40;
        return 0;
    }

    /** The share of a floor's secrets that counts as all of them. */
    static double secretsNeeded(DungeonFloor floor) {
        if (floor.isMasterMode()) return 1;
        return switch (floor.getNumber()) {
            case 0, 1 -> 0.3;
            case 2 -> 0.4;
            case 3 -> 0.5;
            case 4 -> 0.6;
            case 5 -> 0.7;
            case 6 -> 0.85;
            default -> 1;
        };
    }

    /** Seconds before the speed score starts dropping. */
    static int timeLimit(DungeonFloor floor) {
        if (floor.isMasterMode()) return floor.getNumber() == 7 ? 15 * 60 : 8 * 60;
        return switch (floor.getNumber()) {
            case 0 -> 20 * 60;
            case 4, 6 -> 12 * 60;
            case 7 -> 14 * 60;
            default -> 10 * 60;
        };
    }
}
