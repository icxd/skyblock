package net.icxd.dungeons.quest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.hub.Hub;
import net.icxd.dungeons.island.Islands;
import net.icxd.dungeons.quest.Place.Island;
import net.icxd.dungeons.quest.Quest.Objective;
import net.icxd.dungeons.region.RegionType;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * The objective a player follows, shown as Hypixel shows it: a yellow boss bar "Objective: Break a
 * log" (full, no notches; it came 0.4 s after arriving on the island) and two sidebar lines. Which
 * quest is followed is {@link Quests#follow}'s: picked again on arriving somewhere (joining, another
 * world, another area), kept while it's going otherwise. When a step of the quest being followed
 * changes the objective, NEW OBJECTIVE says the new one in chat two seconds later (recorded after
 * every step); arriving somewhere only changes the bar. Main thread.
 */
public final class QuestBar {
    /** NEW OBJECTIVE comes this long after the step (recorded: 1.9 to 2.0 s after the bar changed). */
    static final long ANNOUNCE_DELAY_TICKS = 40;

    private static final Map<UUID, Following> FOLLOWING = new HashMap<>();

    /** What a player follows here: the quest (null for none), the objective shown, the bar showing it. */
    private static final class Following {
        Quest quest;
        int objective = -1;
        BossBar bar;
        BukkitTask announcing;
    }

    private QuestBar() {
    }

    /** Where they are, for quests. */
    public static Place place(Player player) {
        ServerType type = Dungeons.getSkyBlockServer().getServerType();
        if (Islands.isIsland(player.getWorld())) {
            return new Place(Islands.isOwnIsland(player, player.getWorld()) ? Island.PRIVATE_ISLAND : Island.OTHER, null);
        }
        if (Hub.is(player.getWorld())) {
            RegionType region = PlayerSession.of(player).getRegion();
            return new Place(Island.HUB, region == null || region == RegionType.NONE ? null : region.getName());
        }
        if (type == ServerType.DUNGEON_HUB) return new Place(Island.DUNGEON_HUB, "Dungeon Hub");
        return new Place(Island.OTHER, null);
    }

    /** They've arrived somewhere (joined, another world, another area): the quest most at home here, without a word. */
    public static void moved(Player player) {
        update(player, true, false);
    }

    /** A step of some quest was done: the followed quest stays while it's going, and a new objective of it is said. */
    static void progressed(Player player) {
        update(player, false, true);
    }

    private static void update(Player player, boolean moved, boolean announce) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null) return;
        Document profile = user.profile();
        Following following = FOLLOWING.computeIfAbsent(player.getUniqueId(), id -> new Following());
        Quest quest = shown(player) ? Quests.follow(profile, place(player), following.quest, moved) : null;
        int objective = quest == null ? -1 : Quests.current(profile, quest);
        boolean changed = quest != following.quest || objective != following.objective;
        following.quest = quest;
        following.objective = objective;
        show(player, following, quest == null ? null : quest.objective(objective));
        if (changed && announce && quest != null) announceLater(player, following, quest, objective);
    }

    /** Not in a dungeon run's world, where the run's own sidebar is. */
    private static boolean shown(Player player) {
        return Dungeons.getSkyBlockServer().getServerType() != ServerType.DUNGEONS;
    }

    private static void show(Player player, Following following, Objective objective) {
        if (objective == null) {
            if (following.bar != null) player.hideBossBar(following.bar);
            following.bar = null;
            return;
        }
        Component name = Text.line(QuestText.bossBar(objective));
        if (following.bar == null) {
            following.bar = BossBar.bossBar(name, 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
            player.showBossBar(following.bar);
        } else {
            following.bar.name(name);
        }
    }

    private static void announceLater(Player player, Following following, Quest quest, int objective) {
        if (following.announcing != null) following.announcing.cancel();
        following.announcing = Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            following.announcing = null;
            User user = User.ifLoaded(player.getUniqueId());
            if (!player.isOnline() || user == null || Quests.current(user.profile(), quest) != objective) return;
            for (Component line : newObjective(quest, objective, Quests.done(user.profile(), quest))) player.sendMessage(line);
        }, ANNOUNCE_DELAY_TICKS);
    }

    /** NEW OBJECTIVE's lines, the middle two linking to the Quest Log with the quest's progress as their hover when the objective is linked. */
    static List<Component> newObjective(Quest quest, int objective, int done) {
        Objective step = quest.objective(objective);
        List<String> lines = QuestText.newObjective(step);
        Component hover = Text.line(String.join("\n", QuestText.hover(quest, done)));
        return lines.stream().map(line -> {
            if (line.isEmpty() || !step.linked()) return Text.line(line);
            // The two spaces in front aren't part of the link, as recorded.
            return Component.text("  ").append(Text.line(line.substring(2))
                    .clickEvent(ClickEvent.runCommand("/questlog")).hoverEvent(HoverEvent.showText(hover)));
        }).toList();
    }

    /** The sidebar's lines for what they follow ("Objective", "Break a log"); none when they follow nothing. */
    public static List<String> sidebar(Player player) {
        Following following = FOLLOWING.get(player.getUniqueId());
        if (following == null || following.quest == null || following.objective < 0) return List.of();
        return QuestText.sidebar(following.quest.objective(following.objective));
    }

    /** Gone, or on another profile: they follow nothing till they arrive somewhere again. */
    public static void forget(Player player) {
        Following following = FOLLOWING.remove(player.getUniqueId());
        if (following == null) return;
        if (following.announcing != null) following.announcing.cancel();
        if (following.bar != null) player.hideBossBar(following.bar);
    }
}
