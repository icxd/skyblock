package net.icxd.dungeons.region;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
 * Hypixel says so with a title ("Village" over "NEW AREA DISCOVERED!", 2 s in, 2 s up, 2 s out), a
 * challenge's fanfare and a few lines in chat on what's there, all as recorded (2026-10-24 and 10-25).
 * Finding the Village starts the Hub's quests (the boss bar went to "Talk to the Bartender" as it was
 * found, and the Quest Log had them all started that minute). Only areas whose lines were recorded
 * are announced. The profile keeps what it found under {@code areas}. Main thread.
 */
public final class Areas {
    static final String KEY = "areas";
    static final String DUNGEON_HUB = "Dungeon Hub";
    /** The fanfare: a challenge toast's, quiet and high (0.3 and 94/63, as recorded with each area). */
    static final float FOUND_VOLUME = 0.3f;
    static final float FOUND_PITCH = 94 / 63f;
    /** What each area's discovery says it has, as recorded; a long one went on under itself, after a "\n". */
    static final Map<String, List<String>> FOUND = Map.ofEntries(
            Map.entry("Village", List.of("Explore the area to learn more about &bSkyBlock&f.", "Visit the &6Auction House&f.",
                    "Manage your &6coins &fin the &6Bank&f.")),
            Map.entry("Community Center", List.of("Participate in the &aMayor Elections&f.", "Contribute to City Projects.",
                    "Unlock upgrades, buffs, and cosmetics in the &bCommunity Shop&f.")),
            Map.entry("Election Room", List.of("Vote in the &aMayor Election&f.")),
            Map.entry("Auction House", List.of("Auction off your special items.", "Bid on other players' items.")),
            Map.entry("Bazaar Alley", List.of("Buy and sell materials in bulk at the &6Bazaar&f.")),
            Map.entry("Bank", List.of("Talk to the Banker.", "Store your coins to keep them safe.", "Earn interest on your coins.")),
            Map.entry("Farm", List.of("Learn about the Farming Skill at &eFarmer Rigby&f.", "Gather &eWheat&f.",
                    "Learn about Minion Upgrades at &eArthur&f.", "Travel to &eThe Barn&f.")),
            Map.entry("Flower House", List.of("Talk to Marco about &aSpray Cans&f.", "Gather flowers.")),
            Map.entry("Combat Settlement", List.of("Buy Combat Tools from the &cWeaponsmith &fand &cRosetta&f.",
                    "Talk to &cJax &fat the &3Archery Range &fto learn about bows and\n&farrows.",
                    "Learn about &3Enchanting &fat the &3Library&f.",
                    "Talk to &cMaxwell &fat the &6Thaumaturgist &fto learn about\n&6Accessory Power&f.")),
            Map.entry("Graveyard", List.of("Fight Zombies.", "Travel to the &cSpider's Den&f.", "Talk to &7Pat&f.", "Investigate the Catacombs.")),
            Map.entry(DUNGEON_HUB, List.of("Join Dungeons!", "Meet new friends and organize Dungeon parties!", "Talk with &cOphelia &fand &cMalik&f.")));

    private Areas() {
    }

    /**
     * Where they are now ({@code at}): their session's area, the action bar's notice if it changed, and
     * its discovery if it's new to the profile. True if the area changed (by its name: from one of the
     * Village's boxes to the other, it didn't).
     */
    public static boolean update(Player player, Location at) {
        PlayerSession session = PlayerSession.of(player);
        RegionType now = RegionType.getRegionType(at);
        RegionType was = session.getRegion() != null ? session.getRegion() : RegionType.NONE;
        session.setRegion(now);
        if (now.sameArea(was)) return false;
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
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, FOUND_VOLUME, FOUND_PITCH);
        for (String line : discovery(coloured, lines)) player.sendMessage(Text.line(line));
        if (area.equals("Village")) QuestProgress.startAll(player, Quests.HUB);
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
     * resource pack's, so the classic ⏣ here, as in the sidebar), a blank, what's there, a blank. A thing
     * that goes on to another line goes on under its text ("   &farrows.", as recorded).
     */
    static List<String> discovery(String coloured, List<String> things) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(" &6&lNEW AREA DISCOVERED!");
        lines.add("  &7⏣ " + coloured);
        lines.add("");
        for (String thing : things) {
            String[] parts = thing.split("\n");
            lines.add("&7   ■ &f" + parts[0]);
            for (int i = 1; i < parts.length; i++) lines.add("   " + parts[i]);
        }
        lines.add("");
        return lines;
    }
}
