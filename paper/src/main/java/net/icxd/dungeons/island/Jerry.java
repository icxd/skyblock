package net.icxd.dungeons.island;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.npc.Hologram;
import net.icxd.dungeons.npc.Npc;
import net.icxd.dungeons.quest.QuestProgress;
import net.icxd.dungeons.quest.Quests;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Jerry, on every island, where the starter island has him (9.5 100 33.5): a plains farmer villager
 * under "NEW UPDATE / Jerry / CLICK", with the portal's "Travel to: / Hub Island" over it, as recorded.
 * Talking to him the first time (or while Getting Started is at "Talk to Jerry") he says his three
 * lines, 1.5 s and then 2 s apart as recorded, and the objective is done with the last; after that
 * he opens his menu ({@link JerryMenu}).
 */
public final class Jerry implements IslandFeature {
    public static final String ID = "JERRY";
    static final List<String> LINES = List.of(
            "&e[NPC] Jerry&f: &rYour SkyBlock island is part of a much larger universe.",
            "&e[NPC] Jerry&f: &rThe SkyBlock universe is full of islands to explore and resources to discover!",
            "&e[NPC] Jerry&f: &rUse the &5Portal &fto warp to the first of those islands - the SkyBlock Hub!");
    /** Ticks before each line (recorded at 01:47.5, 01:48.9 and 01:50.9). */
    static final long[] LINE_TICKS = {0, 29, 69};
    static final List<String> TAGS = List.of("&6&lNEW UPDATE", "Jerry", "&e&lCLICK");
    static final double[] TAG_HEIGHTS = {0.35, 0.10, -0.15};

    private final Plugin plugin;
    /** What each loaded island has of this: Jerry and the portal's sign, by world. */
    private final Map<String, Spawned> spawned = new HashMap<>();
    private final Set<UUID> talking = new HashSet<>();

    private record Spawned(Npc jerry, Hologram sign) {
    }

    public Jerry(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void loaded(World world) {
        double[] at = StarterIsland.JERRY;
        Npc jerry = Npc.villager(ID, new Location(world, at[0], at[1], at[2], StarterIsland.JERRY_YAW, 0), Villager.Type.PLAINS,
                Villager.Profession.FARMER, TAGS, TAG_HEIGHTS, this::talk);
        double[] sign = StarterIsland.PORTAL_SIGN;
        Hologram portal = Hologram.spawn(new Location(world, sign[0], sign[1], sign[2]), List.of("&bTravel to:", "&aHub Island"));
        spawned.put(world.getName(), new Spawned(jerry, portal));
    }

    @Override
    public void unloading(World world) {
        Spawned gone = spawned.remove(world.getName());
        if (gone == null) return;
        gone.jerry().remove();
        gone.sign().remove();
    }

    private void talk(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || talking.contains(player.getUniqueId())) return;
        boolean objective = Quests.current(user.profile(), Quests.GETTING_STARTED) == Quests.TALK_TO_JERRY;
        if (!Npc.firstTalk(user.profile(), ID) && !objective) {
            JerryMenu.show(player);
            return;
        }
        talking.add(player.getUniqueId());
        for (int i = 0; i < LINES.size(); i++) {
            String line = LINES.get(i);
            boolean last = i == LINES.size() - 1;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (last) talking.remove(player.getUniqueId());
                if (!player.isOnline()) return;
                player.sendMessage(Text.line(line));
                if (last) QuestProgress.complete(player, Quests.GETTING_STARTED, Quests.TALK_TO_JERRY);
            }, LINE_TICKS[i]);
        }
    }
}
