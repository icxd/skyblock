package net.icxd.dungeons.region;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.icxd.dungeons.quest.QuestProgress;
import net.icxd.dungeons.quest.Quests;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Text;
import net.kyori.adventure.title.Title;

/**
 * The area a player is in, and the areas their profile has found: the first time it gets to one,
 * Hypixel says so with a title ("Village" over "NEW AREA DISCOVERED!", 2 s in, 2 s up, 2 s out) and
 * a few lines in chat on what's there, all as recorded (2026-10-24). Finding the Village starts Time
 * to Strike (the boss bar went to "Talk to the Bartender" as it was found). Only areas whose lines
 * were recorded are announced. The profile keeps what it found under {@code areas}. Main thread.
 */
public final class Areas {
    static final String KEY = "areas";
    static final String DUNGEON_HUB = "Dungeon Hub";
    /** What each area's discovery says it has, as recorded. */
    static final Map<String, List<String>> FOUND = Map.of(
            "Village", List.of("Explore the area to learn more about &bSkyBlock&f.", "Visit the &6Auction House&f.",
                    "Manage your &6coins &fin the &6Bank&f."),
            "Community Center", List.of("Participate in the &aMayor Elections&f.", "Contribute to City Projects.",
                    "Unlock upgrades, buffs, and cosmetics in the &bCommunity Shop&f."),
            "Election Room", List.of("Vote in the &aMayor Election&f."),
            "Auction House", List.of("Auction off your special items.", "Bid on other players' items."),
            "Bazaar Alley", List.of("Buy and sell materials in bulk at the &6Bazaar&f."),
            "Bank", List.of("Talk to the Banker.", "Store your coins to keep them safe.", "Earn interest on your coins."),
            "Farm", List.of("Learn about the Farming Skill at &eFarmer Rigby&f.", "Gather &eWheat&f.",
                    "Learn about Minion Upgrades at &eArthur&f.", "Travel to &eThe Barn&f."),
            DUNGEON_HUB, List.of("Join Dungeons!", "Meet new friends and organize Dungeon parties!", "Talk with &cOphelia &fand &cMalik&f."));

    private Areas() {
    }

    /**
     * Where they are now ({@code at}): their session's area, the action bar's notice if it changed, and
     * its discovery if it's new to the profile. True if the area changed.
     */
    public static boolean update(Player player, Location at) {
        PlayerSession session = PlayerSession.of(player);
        RegionType now = RegionType.getRegionType(at);
        RegionType was = session.getRegion() != null ? session.getRegion() : RegionType.NONE;
        session.setRegion(now);
        if (was == now) return false;
        session.setDefenseReplacement(Replacement.forMillis("&7⏣ &7" + now.displayName(), 1000));
        if (now != RegionType.NONE) found(player, now.getName(), now.displayName());
        return true;
    }

    /** The area they're in, found by their profile if it's new to it. */
    public static void found(Player player, String area, String coloured) {
        User user = User.ifLoaded(player.getUniqueId());
        List<String> lines = FOUND.get(area);
        if (user == null || user.isReleased() || lines == null || !discover(user.profile(), area)) return;
        user.save();
        player.showTitle(Title.title(Text.line(coloured), Text.line("&6&lNEW AREA DISCOVERED!"),
                Title.Times.times(Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(2))));
        for (String line : discovery(coloured, lines)) player.sendMessage(Text.line(line));
        if (area.equals("Village")) QuestProgress.start(player, Quests.TIME_TO_STRIKE);
    }

    /** Marks an area found on the profile; false if it was already. */
    static boolean discover(Document profile, String area) {
        List<Object> found = profile.get(KEY) instanceof List<?> list ? new ArrayList<>(list) : new ArrayList<>();
        if (found.contains(area)) return false;
        found.add(area);
        profile.put(KEY, found);
        return true;
    }

    /**
     * The chat: a blank line, " NEW AREA DISCOVERED!", the area under it (Hypixel's area icon is its
     * resource pack's, so the classic ⏣ here, as in the sidebar), a blank, what's there, a blank.
     */
    static List<String> discovery(String coloured, List<String> things) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(" &6&lNEW AREA DISCOVERED!");
        lines.add("  &7⏣ " + coloured);
        lines.add("");
        for (String thing : things) lines.add("&7   ■ &f" + thing);
        lines.add("");
        return lines;
    }
}
