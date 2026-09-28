package net.icxd.dungeons.leveling;

import org.bson.Document;

import net.icxd.dungeons.leveling.LevelingData.Emblem;
import net.icxd.dungeons.leveling.LevelingData.EmblemCategory;

/**
 * Prefix emblems: which a profile has unlocked, of those whose unlock the plugin can tell (a SkyBlock level, a skill
 * level, Catacombs, a class, the class average, a floor's completion; the rest, Slayer's and the like, stay locked),
 * and the one it has chosen to show, {@code leveling.emblem} in the profile (per profile, as its levels are; UNKNOWN
 * on Hypixel). A chosen emblem that's locked again shows nothing. No server needed.
 */
public final class Emblems {
    /** The profile's part for SkyBlock Leveling: the chosen emblem, and the last level they were told they reached. */
    static final String PART = "leveling";
    static final String CHOSEN = "emblem";

    private Emblems() {
    }

    public static boolean unlocked(Emblem emblem, Document profile, int level, LevelingSources sources) {
        return SkyBlockXp.done(emblem.unlock(), profile, level, sources);
    }

    /** The emblem the profile shows: its chosen one while it's unlocked; null for none. */
    public static Emblem shown(LevelingData data, Document profile, int level, LevelingSources sources) {
        Emblem emblem = data.emblem(chosen(profile));
        return emblem != null && unlocked(emblem, profile, level, sources) ? emblem : null;
    }

    /** The id of the emblem the profile has chosen, unlocked or not; null for none. */
    static String chosen(Document profile) {
        Document part = profile == null ? null : profile.get(PART) instanceof Document d ? d : null;
        return part == null ? null : part.getString(CHOSEN);
    }

    /** Chooses an emblem for the profile (null to show none). */
    static void choose(Document profile, String id) {
        Document part = part(profile);
        if (id == null) part.remove(CHOSEN);
        else part.put(CHOSEN, id);
    }

    /** How many of a category's emblems the profile has unlocked. */
    static int unlocked(EmblemCategory category, Document profile, int level, LevelingSources sources) {
        int n = 0;
        for (Emblem emblem : category.emblems()) if (unlocked(emblem, profile, level, sources)) n++;
        return n;
    }

    /** The profile's leveling part, made if it has none. */
    static Document part(Document profile) {
        if (profile.get(PART) instanceof Document d) return d;
        Document part = new Document();
        profile.put(PART, part);
        return part;
    }
}
