package net.icxd.dungeons.leveling;

import org.bson.Document;

/**
 * What SkyBlock XP counts that other parts of the plugin keep: each is none until the code that keeps it is wired
 * in with {@link SkyBlockLevels#sources(LevelingSources)} (overriding the method here), as ScoreCounts does for a
 * dungeon run's score. Read on the main thread, from the profile's document.
 */
public interface LevelingSources {
    /** Collection tiers (milestones) reached on the profile, all its collections' together: the Collections task. */
    default int collectionTiers(Document profile) {
        return 0;
    }

    /** The tier reached in one collection, by its id in Hypixel's API ("CARROT_ITEM"): the guide's Collections tasks. */
    default int collectionTier(Document profile, String collection) {
        return 0;
    }
}
