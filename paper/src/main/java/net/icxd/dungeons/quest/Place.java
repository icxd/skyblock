package net.icxd.dungeons.quest;

/**
 * Where a player is, as far as quests go: which island (Hypixel's word for each part of SkyBlock) and
 * the area of it the sidebar names (null while it names none).
 */
public record Place(Island island, String area) {
    public enum Island {
        /** Their own island. */
        PRIVATE_ISLAND,
        HUB,
        DUNGEON_HUB,
        /** Anywhere else (the mines, the Crimson Isle, a dungeon). */
        OTHER
    }
}
