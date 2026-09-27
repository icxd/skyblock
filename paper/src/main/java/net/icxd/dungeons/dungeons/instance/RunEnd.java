package net.icxd.dungeons.dungeons.instance;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.DungeonRecords;

/**
 * The end of a floor that was finished, as recorded on Hypixel (research score_rewards.md 2): each
 * member's summary in chat with their Bits and experience, then (half a second later) the Blood
 * Room counts and the map turns into the score card, which is why the card's score is higher than
 * the chat's. Experience follows the chat's.
 *
 * <p>Timings after the summary, in ticks (R1 and R2): the card at +0.5 s, "Your Score Summary" at
 * +0.6 s, the re-queue link at +2.0 s, the warning at +10.1 s and off to the Dungeon Hub at +20.2 s.
 */
final class RunEnd {
    static final long CARD = 10;
    static final long CARD_ITEM = 12;
    static final long REQUEUE_MESSAGE = 40;
    static final long CLOSE_WARNING = 202;
    static final long CLOSE = 404;

    /** The clickable line (see {@link DungeonRun#end}). */
    static final String EXTRA_STATS = RunText.centered("&6> &e&lEXTRA STATS &6<");
    /**
     * After a record Team Score or time (the mods' patterns have it). Its colour is UNKNOWN: Odin's own
     * imitation of these lines uses light purple bold.
     */
    static final String NEW_RECORD = " &d&l(NEW RECORD!)";
    /**
     * Whose day "the first five runs of each day" counts in: UNKNOWN, taken as the zone the sidebar's
     * date is in.
     */
    static final ZoneId DAY_ZONE = ZoneId.of("America/New_York");

    private RunEnd() {
    }

    /** What one member got, and the records they set. */
    record Outcome(RunRewards.Reward reward, DungeonRecords.Completion completion) {
    }

    /**
     * Counts the completion on the profile they play on and gives them its experience and Bits. The
     * caller saves.
     *
     * @param score         the chat's Team Score
     * @param millis        how long the run took
     * @param secretPercent the team's share of the floor's secrets
     */
    static Outcome award(Document profile, DungeonFloor floor, Score score, long millis, double secretPercent,
                         DungeonClass own, Collection<DungeonClass> teammates, LocalDate day) {
        DungeonRecords.Completion completion = DungeonRecords.complete(profile, floor, score.total(), score.grade(), millis, day);
        RunRewards.Reward reward = RunRewards.reward(floor, score.total(), completion.completionsBefore(), completion.todayBefore(),
                secretPercent, own, teammates);
        DungeonProfile.addCatacombsXp(profile, reward.catacombs());
        for (Map.Entry<DungeonClass, Double> e : reward.classes().entrySet()) DungeonProfile.addClassXp(profile, e.getKey(), e.getValue());
        int bits = profile.get("bits") instanceof Number n ? n.intValue() : 0;
        profile.put("bits", bits + reward.bits());
        return new Outcome(reward, completion);
    }

    /**
     * One member's summary, between the bold green rules: the floor, the Team Score and grade, the
     * boss and the time, EXTRA STATS, then their Bits and experience (R1 04:28.4, spaces as
     * recorded). Their own class comes first; the lines for the others' classes (their team bonus)
     * are UNKNOWN on Hypixel and look like it here. With no outcome (their data isn't here), no rewards.
     */
    static List<String> summary(DungeonFloor floor, Score score, long millis, Outcome outcome) {
        boolean bestScore = outcome != null && outcome.completion().bestScore();
        boolean fastest = outcome != null && outcome.completion().fastest();
        List<String> lines = new ArrayList<>();
        lines.add(RunText.RULE);
        lines.add(RunText.centered("&c" + floor.getDungeonName() + " &8- &e" + floor.getTierName()));
        lines.add("");
        lines.add(RunText.centered("Team Score: &a" + score.total() + " &f(" + Score.gradeColor(score.grade()) + score.grade() + "&f)"
                + (bestScore ? NEW_RECORD : "")));
        lines.add(RunText.centered("&c☠ &eDefeated &c" + floor.getBossName() + " &ein &a" + RunText.elapsed(millis)
                + (fastest ? NEW_RECORD : "")));
        lines.add(EXTRA_STATS);
        if (outcome != null) {
            RunRewards.Reward reward = outcome.reward();
            lines.add(RunText.centered("&8+&b" + reward.bits() + " Bits"));
            lines.add(RunText.centered("&8+&3" + RunRewards.format(reward.catacombs()) + " Catacombs Experience"));
            for (Map.Entry<DungeonClass, Double> e : reward.classes().entrySet()) {
                lines.add(RunText.centered("&8+&3" + RunRewards.format(e.getValue()) + " " + e.getKey().getDisplayName() + " Experience"));
            }
        }
        lines.add(RunText.RULE);
        return lines;
    }

    static LocalDate today() {
        return LocalDate.now(DAY_ZONE);
    }
}
