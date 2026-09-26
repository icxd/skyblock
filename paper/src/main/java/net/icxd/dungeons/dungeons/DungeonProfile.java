package net.icxd.dungeons.dungeons;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;

/** A player's dungeon settings and progress, in the {@code dungeons} part of their user document. Main thread. */
public final class DungeonProfile {
    private DungeonProfile() {
    }

    private static Document dungeons(User user) {
        Document doc = user.getDocument().get("dungeons", Document.class);
        if (doc == null) {
            doc = new Document();
            user.getDocument().put("dungeons", doc);
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
        return Boolean.TRUE.equals(dungeons(user).getBoolean("autoReadyUp"));
    }

    /** {@code /togglereadyup}, or the toggle in the Ready Up menu. */
    public static void toggleAutoReadyUp(Player player, User user) {
        boolean on = !autoReadyUp(user);
        dungeons(user).put("autoReadyUp", on);
        user.save();
        player.sendMessage(Utils.color(on ? "&aYou will now auto ready up when joining The Catacombs!"
                : "&cYou will no longer auto ready up when joining &aThe Catacombs&c!"));
        player.sendMessage(Utils.color("&8Use '/togglereadyup' when the instance has started or the toggle in the queue menu to change this option whenever."));
    }
}
