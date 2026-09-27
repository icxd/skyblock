package net.icxd.dungeons.dungeons;

import java.time.LocalDate;

import org.bson.Document;

import net.icxd.dungeons.common.DungeonFloor;

/**
 * A profile's finished floors, for the end of a run: how many times each floor was completed (the
 * first five get more experience), the best score and fastest times (Hypixel's API has
 * {@code tier_completions}, {@code best_score}, {@code fastest_time}, {@code fastest_time_s} and
 * {@code fastest_time_s_plus}), the runs completed today (the first five get more experience), and
 * the highest floor completed, which item requirements read. Failed runs aren't completions and
 * don't count for today. Functions on the profile's document; main thread for a player's.
 *
 * <pre>
 * dungeons.floors: { highest, masterHighest,
 *                    "&lt;FLOOR&gt;": { completions, bestScore, fastest, fastestS, fastestSPlus } }   (times in ms)
 * dungeons.daily:  { day: "2026-09-27", completions }
 * </pre>
 */
public final class DungeonRecords {
    private DungeonRecords() {
    }

    /** What a completion changed: the counts before it (for its experience) and the records it set. */
    public record Completion(int completionsBefore, int todayBefore, boolean bestScore, boolean fastest) {
    }

    /** Times this floor was completed on the profile. */
    public static int completions(Document profile, DungeonFloor floor) {
        return number(floor(profile, floor), "completions").intValue();
    }

    /** The best Team Score on this floor; 0 for none. */
    public static int bestScore(Document profile, DungeonFloor floor) {
        return number(floor(profile, floor), "bestScore").intValue();
    }

    /** The fastest completion of this floor in milliseconds; 0 for none. */
    public static long fastest(Document profile, DungeonFloor floor) {
        return number(floor(profile, floor), "fastest").longValue();
    }

    /** Runs completed on {@code day} (any floor). */
    public static int completionsOn(Document profile, LocalDate day) {
        Document dungeons = profile == null ? null : profile.get("dungeons", Document.class);
        Document daily = dungeons == null ? null : dungeons.get("daily", Document.class);
        if (daily == null || !day.toString().equals(daily.getString("day"))) return 0;
        return number(daily, "completions").intValue();
    }

    /**
     * Counts a completion of the floor: one more for the floor and for the day, the best score and
     * fastest times if they're beaten (the first completion sets them), and the highest floor.
     *
     * @param score  the Team Score (the chat's)
     * @param grade  its grade, for the fastest S and S+ times
     * @param millis how long the run took
     */
    public static Completion complete(Document profile, DungeonFloor floor, int score, String grade, long millis, LocalDate day) {
        int before = completions(profile, floor);
        int today = completionsOn(profile, day);
        Document dungeons = DungeonProfile.dungeons(profile);
        Document floors = part(dungeons, "floors");
        Document mine = part(floors, floor.name());
        mine.put("completions", before + 1);
        boolean bestScore = before == 0 || score > bestScore(profile, floor);
        if (bestScore) mine.put("bestScore", score);
        boolean fastest = faster(mine, "fastest", millis);
        if (grade.equals("S") || grade.equals("S+")) faster(mine, "fastestS", millis);
        if (grade.equals("S+")) faster(mine, "fastestSPlus", millis);
        dungeons.put("daily", new Document("day", day.toString()).append("completions", today + 1));
        String highest = floor.isMasterMode() ? "masterHighest" : "highest";
        // An int: DungeonTierRequirement reads it as an Integer.
        floors.put(highest, Math.max(number(floors, highest).intValue(), floor.getNumber()));
        return new Completion(before, today, bestScore, fastest);
    }

    /** Sets the time if there's none or it's faster; whether it did. */
    private static boolean faster(Document floor, String key, long millis) {
        long old = number(floor, key).longValue();
        if (old > 0 && old <= millis) return false;
        floor.put(key, millis);
        return true;
    }

    private static Document floor(Document profile, DungeonFloor floor) {
        Document dungeons = profile == null ? null : profile.get("dungeons", Document.class);
        Document floors = dungeons == null ? null : dungeons.get("floors", Document.class);
        return floors == null ? null : floors.get(floor.name(), Document.class);
    }

    private static Document part(Document parent, String key) {
        Document doc = parent.get(key, Document.class);
        if (doc == null) {
            doc = new Document();
            parent.put(key, doc);
        }
        return doc;
    }

    private static Number number(Document doc, String key) {
        return doc != null && doc.get(key) instanceof Number n ? n : 0;
    }
}
