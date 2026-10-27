package net.icxd.dungeons.dungeons.chests;

/**
 * The reward chests a run can end with (MCW Dungeon Reward Chest): Wood, Gold and Diamond always, Emerald from a score
 * of 230 (A), Obsidian from 270 (S), Bedrock from 300 (S+, Floors V to VII only).
 */
public enum ChestType {
    WOODEN("Wood", 0),
    GOLD("Gold", 0),
    DIAMOND("Diamond", 0),
    EMERALD("Emerald", 230),
    OBSIDIAN("Obsidian", 270),
    BEDROCK("Bedrock", 300);

    private final String title;
    private final int minScore;

    ChestType(String title, int minScore) {
        this.title = title;
        this.minScore = minScore;
    }

    /** Its menu's title, "Wood" (as recorded for Wood, Gold and Diamond), and its name in the loot tables. */
    public String title() {
        return title;
    }

    /** The team score it needs to spawn at all. */
    public int minScore() {
        return minScore;
    }

    /**
     * "&f&lWOOD CHEST REWARDS" over what it gave (recorded for Wood; the other chests' colours are UNKNOWN, so they're
     * their blocks' colours here).
     */
    public String rewardsTitle() {
        String colour = switch (this) {
            case WOODEN -> "&f";
            case GOLD -> "&6";
            case DIAMOND -> "&b";
            case EMERALD -> "&2";
            case OBSIDIAN -> "&5";
            case BEDROCK -> "&8";
        };
        return colour + "&l" + title.toUpperCase(java.util.Locale.ROOT) + " CHEST REWARDS";
    }
}
