package net.icxd.dungeons.hub;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.npc.NpcTable;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.utils.Text;

/**
 * What the Hub's NPCs said when the recording's player talked to them (2026-10-24), as they said it:
 * each line in chat ("[NPC] Elizabeth: ...") at its recorded moment, in the NPC's voice. They say it
 * on every click for now (UNKNOWN: whether Hypixel's say it again; Elizabeth's Community Shop and the
 * Auction Master's Auction House, which they'd likely open, aren't here). The Hub's other NPCs weren't
 * talked to, so they say nothing yet.
 *
 * @param id       the NPC's ({@link NpcTable.Entry#id})
 * @param at       where it stands (x, y, z), for a name two NPCs share; null for any of that name
 * @param chatName its name in chat, which isn't always its tag's ("Nancy" for Minister Nancy)
 * @param ticks    when each line comes, from the click
 */
record HubTalk(String id, double[] at, String chatName, Voice voice, long[] ticks, List<String> lines) {
    static final List<HubTalk> ALL = List.of(
            // 03:37.9, 03:38.9, 03:39.9.
            new HubTalk("ELIZABETH", null, "&dElizabeth", Voice.villager(2), new long[]{0, 20, 40}, List.of(
                    "&rHello! Welcome to &bSkyBlock&r!",
                    "&rI have powerful items to offer, but only to experienced adventurers!",
                    "&rUntil then, I suggest leveling up to SkyBlock Level 3&r!")),
            // The Community Center's, not the Election Room's: 03:42.9, 03:43.7, 03:45.2.
            new HubTalk("CLERK_SERAPHINE", new double[]{-1.5, 79, 10.5}, "Clerk Seraphine", Voice.villager(2), new long[]{0, 16, 46}, List.of(
                    "&r&fWelcome to the &bCommunity Center&f!",
                    "&r&fContribute to community projects, upgrade your account, and more by talking to &dElizabeth&f!",
                    "&r&fYou can also vote in the &bmayor elections &fby heading through the warp to the left!")),
            // 04:13.7, 04:14.6.
            new HubTalk("MINISTER_NANCY", null, "Nancy", Voice.VILLAGER, new long[]{0, 18}, List.of(
                    "&rHi!",
                    "&rI'm just a placeholder because there is no Minister this election.")),
            // 04:41.4, 04:43.4, 04:45.4, 04:48.4.
            new HubTalk("AUCTION_MASTER", null, "&6Auction Master", Voice.VILLAGER, new long[]{0, 40, 80, 140}, List.of(
                    "&r&fHey there, I'm the Auction Master.",
                    "&r&fIn the &6Auction House&f, you can put your valuable items up for auction!",
                    "&r&fYou may also want to check back here to see what items other players are selling to see if you can get a good deal.",
                    "&r&fTalk to me or one of the &6Auction Agents &fif you would like to start your first auction or if you want to see the items currently being sold.")));

    /** Who's in the middle of a talk (a click then doesn't start another). */
    private static final Set<UUID> TALKING = new HashSet<>();

    /** What this NPC says, if its words were recorded. */
    static Optional<HubTalk> of(NpcTable.Entry npc) {
        for (HubTalk talk : ALL) {
            if (!talk.id.equals(npc.id())) continue;
            if (talk.at == null || Math.abs(talk.at[0] - npc.x()) <= 1 && Math.abs(talk.at[1] - npc.y()) <= 1 && Math.abs(talk.at[2] - npc.z()) <= 1) {
                return Optional.of(talk);
            }
        }
        return Optional.empty();
    }

    /** "[NPC] Elizabeth: Hello! Welcome to SkyBlock!" */
    String line(int i) {
        return "&e[NPC] " + chatName + "&f: " + lines.get(i);
    }

    void talk(Plugin plugin, Player player) {
        UUID id = player.getUniqueId();
        if (!TALKING.add(id)) return;
        for (int i = 0; i < lines.size(); i++) {
            int line = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (line == lines.size() - 1) TALKING.remove(id);
                if (!player.isOnline()) return;
                player.sendMessage(Text.line(line(line)));
                voice.speak(player);
            }, ticks[i]);
        }
    }
}
