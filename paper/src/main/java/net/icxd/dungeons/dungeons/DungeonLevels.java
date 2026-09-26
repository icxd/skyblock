package net.icxd.dungeons.dungeons;

/**
 * Catacombs and class levels from experience. The table is Hypixel's, as the mods have it (Odin's
 * {@code calculateDungeonLevel}): the experience each level takes, then a level per 200 million.
 */
public final class DungeonLevels {
    private static final long[] XP = {
            50, 75, 110, 160, 230, 330, 470, 670, 950, 1340,
            1890, 2665, 3760, 5260, 7380, 10300, 14400, 20000,
            27600, 38000, 52500, 71500, 97000, 132000, 180000,
            243000, 328000, 445000, 600000, 800000, 1065000,
            1410000, 1900000, 2500000, 3300000, 4300000, 5600000,
            7200000, 9200000, 12000000, 15000000, 19000000,
            24000000, 30000000, 38000000, 48000000, 60000000,
            75000000, 93000000, 116250000, 200000000};
    private static final long PAST_TABLE = 200_000_000L;
    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private DungeonLevels() {
    }

    /** Whole levels reached with this much experience. */
    public static int level(double xp) {
        double total = 0;
        for (int level = 0; level < XP.length; level++) {
            if (xp < total + XP[level]) return level;
            total += XP[level];
        }
        return XP.length + (int) ((xp - total) / PAST_TABLE);
    }

    /** "XX" for 20, as in the tab list's "(Berserk XX)"; 0 stays "0". */
    public static String roman(int number) {
        if (number <= 0) return String.valueOf(number);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (number >= ROMAN_VALUES[i]) {
                out.append(ROMAN_SYMBOLS[i]);
                number -= ROMAN_VALUES[i];
            }
        }
        return out.toString();
    }
}
