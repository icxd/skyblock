package net.icxd.dungeons.item.ability.utility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.PlayerAttributes;
import net.icxd.dungeons.stats.Stats;

/**
 * Stats for a while, from an ability ("gain +30❁ Strength ... for 20 seconds"), on {@link PlayerSession#buff}:
 * the same ability again starts them again rather than adding to them ("Effect doesn't stack."), and walk
 * speed follows at once and again when they run out.
 */
final class Buffs {
    private Buffs() {
    }

    /** Gives them {@code stats} for {@code millis}, in place of what {@code source} gave them before. */
    static void give(Player player, String source, Stats stats, long millis) {
        PlayerSession session = PlayerSession.of(player);
        session.buff(source, stats, millis);
        PlayerAttributes.apply(player, session.stats());
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            if (player.isOnline()) PlayerAttributes.apply(player, PlayerSession.of(player).stats());
        }, millis / 50 + 1);
    }

    /**
     * Who "you and 4 nearby players" are: the caster, then the others in the same world within {@code radius}
     * (nearest first), {@code others} of them at most (all of them for a negative number). Dungeon ghosts
     * aren't anyone's nearby players.
     */
    static List<Player> youAndNearby(Player caster, double radius, int others) {
        Location at = caster.getLocation();
        List<Player> nearby = new ArrayList<>();
        for (Player other : caster.getWorld().getPlayers()) {
            if (other.equals(caster) || other.isDead() || other.getLocation().distanceSquared(at) > radius * radius) continue;
            DungeonRun run = RunManager.of(other);
            if (run != null && run.isGhost(other.getUniqueId())) continue;
            nearby.add(other);
        }
        nearby.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(at)));
        List<Player> out = new ArrayList<>();
        out.add(caster);
        out.addAll(others < 0 ? nearby : nearby.subList(0, Math.min(others, nearby.size())));
        return out;
    }
}
