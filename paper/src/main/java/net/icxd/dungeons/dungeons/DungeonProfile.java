package net.icxd.dungeons.dungeons;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.profile.Profiles;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;

/**
 * A player's dungeon progress, in the {@code dungeons} part of the profile they play on, and their
 * auto ready up setting, which is the account's (in {@code settings}). Main thread.
 */
public final class DungeonProfile {
    private DungeonProfile() {
    }

    private static Document dungeons(User user) {
        return part(user.profile(), "dungeons");
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

    public static int classLevel(User user, DungeonClass dungeonClass) {
        Document exp = dungeons(user).get("classExp", Document.class);
        Object xp = exp == null ? null : exp.get(dungeonClass.name());
        return xp instanceof Number n ? DungeonLevels.level(n.doubleValue()) : 0;
    }

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
