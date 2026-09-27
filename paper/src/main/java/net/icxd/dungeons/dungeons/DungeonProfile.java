package net.icxd.dungeons.dungeons;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.profile.Profiles;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;

/**
 * A player's dungeon progress, in the {@code dungeons} part of the profile they play on, and their
 * auto ready up setting, which is the account's (in {@code settings}). Catacombs experience is
 * {@code dungeons.catacombsExp} and each class's {@code dungeons.classExp.<CLASS>}, as doubles (older
 * whole numbers read the same); both levels come from {@link DungeonLevels}. Main thread.
 */
public final class DungeonProfile {
    private DungeonProfile() {
    }

    private static Document dungeons(User user) {
        return dungeons(user.profile());
    }

    /** A profile's {@code dungeons} part, made if it has none. */
    static Document dungeons(Document profile) {
        return part(profile, "dungeons");
    }

    private static Document settings(User user) {
        return part(user.getDocument(), Profiles.SETTINGS);
    }

    private static Document part(Document parent, String key) {
        Document doc = parent.get(key, Document.class);
        if (doc == null) {
            doc = new Document();
            parent.put(key, doc);
        }
        return doc;
    }

    public static DungeonClass selectedClass(User user) {
        return DungeonClass.parse(dungeons(user).getString("selectedClass"));
    }

    public static void selectClass(User user, DungeonClass dungeonClass) {
        dungeons(user).put("selectedClass", dungeonClass.name());
        user.save();
    }

    // Experience

    /** The profile's Catacombs experience (0 for none). */
    public static double catacombsXp(Document profile) {
        Document dungeons = profile == null ? null : profile.get("dungeons", Document.class);
        return dungeons != null && dungeons.get("catacombsExp") instanceof Number n ? n.doubleValue() : 0;
    }

    /** The profile's experience in a class (0 for none). */
    public static double classXp(Document profile, DungeonClass dungeonClass) {
        Document dungeons = profile == null ? null : profile.get("dungeons", Document.class);
        Document exp = dungeons == null ? null : dungeons.get("classExp", Document.class);
        return exp != null && exp.get(dungeonClass.name()) instanceof Number n ? n.doubleValue() : 0;
    }

    /** Adds Catacombs experience to the profile (nothing for none or less). */
    public static void addCatacombsXp(Document profile, double xp) {
        if (xp > 0) dungeons(profile).put("catacombsExp", catacombsXp(profile) + xp);
    }

    /** Adds experience in a class to the profile (nothing for none or less). */
    public static void addClassXp(Document profile, DungeonClass dungeonClass, double xp) {
        if (!(xp > 0)) return;
        double before = classXp(profile, dungeonClass);
        part(dungeons(profile), "classExp").put(dungeonClass.name(), before + xp);
    }

    /** At the end of a run: Catacombs experience on the profile they play on, saved. */
    public static void addCatacombsXp(User user, double xp) {
        addCatacombsXp(user.profile(), xp);
        user.save();
    }

    /** At the end of a run: experience in a class on the profile they play on, saved. */
    public static void addClassXp(User user, DungeonClass dungeonClass, double xp) {
        addClassXp(user.profile(), dungeonClass, xp);
        user.save();
    }

    /** Their Catacombs level, cosmetic levels past 50 included (for what's shown). */
    public static int catacombsLevel(User user) {
        return DungeonLevels.level(catacombsXp(user.profile()));
    }

    /** Their Catacombs level for stats (the dungeon item boost): at most 50. */
    public static int catacombsStatLevel(User user) {
        return Math.min(catacombsLevel(user), DungeonLevels.STAT_CAP);
    }

    public static int classLevel(User user, DungeonClass dungeonClass) {
        return DungeonLevels.level(classXp(user.profile(), dungeonClass));
    }

    // Settings

    /** Ready up by yourself when you arrive in a dungeon. */
    public static boolean autoReadyUp(User user) {
        return Boolean.TRUE.equals(settings(user).getBoolean("autoReadyUp"));
    }

    /** {@code /togglereadyup}, or the toggle in the Ready Up menu. */
    public static void toggleAutoReadyUp(Player player, User user) {
        boolean on = !autoReadyUp(user);
        settings(user).put("autoReadyUp", on);
        user.save();
        player.sendMessage(Utils.color(on ? "&aYou will now auto ready up when joining The Catacombs!"
                : "&cYou will no longer auto ready up when joining &aThe Catacombs&c!"));
        player.sendMessage(Utils.color("&8Use '/togglereadyup' when the instance has started or the toggle in the queue menu to change this option whenever."));
    }
}
