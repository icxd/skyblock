package net.icxd.dungeons.common;

import java.util.List;

/**
 * What it takes to lead a party into The Catacombs: Combat XV (the Catacombs Gate lists "Combat
 * Skill 15." as the Entrance's requirement, and Mort turns away whoever leads without it; research
 * critic.md 3.6). The proxy checks it when a party enters.
 */
public final class DungeonEntry {
    public static final int COMBAT_LEVEL = 15;
    /**
     * Combat experience for level XV from nothing, on the standard skill table (the Paper plugin's
     * {@code Skill}; a test there checks that they agree).
     */
    public static final double COMBAT_XP = 67_425;
    /** Mort, to whoever tries to lead a dungeon below Combat XV (MCW Mort, "Below Combat XV"; the missing colon is his). */
    public static final List<String> MORT_REFUSAL = List.of(
            "§e[NPC] §bMort§f Wanderer!",
            "§e[NPC] §bMort§f: Save your own skin, seek challenge elsewhere first.",
            "§e[NPC] §bMort§f: §7§oYou need Combat Level XV before leading a dungeon.");

    private DungeonEntry() {
    }

    /** Whether a player with this much Combat experience may lead a party into a dungeon. */
    public static boolean mayLead(double combatXp) {
        return combatXp >= COMBAT_XP;
    }
}
