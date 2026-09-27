package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.dungeons.classes.ClassDetails;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.utils.Utils;

/**
 * Members who died in a run: instead of dying they become ghosts (research critic.md 3.1, MCW
 * Ghosts). A ghost flies without going through blocks, is invisible to the others but for a head
 * that follows it, can't be hurt or targeted, and can't touch the world (see {@link GhostEvents})
 * except to kill a Fairy. It comes back by itself after {@link DeathRules#autoReviveSeconds} (15
 * seconds on the Entrance), when a teammate uses a Revive Stone on it, or when it or a teammate kills
 * a Fairy; a Revive Stone in their inventory when they die brings them straight back instead.
 *
 * <p>Main thread; {@link DungeonRun} ticks it.
 */
final class Ghosts {
    /** How far over a ghost's feet the (small) armor stand carrying its head stands, so the head is about where theirs is. */
    private static final double HEAD_ABOVE = 1.0;

    private static final class Ghost {
        /** Run tick they became one. */
        int since;
        GameMode mode;
        ArmorStand head;
    }

    private final DungeonRun run;
    private final Plugin plugin;
    private final int autoReviveTicks;
    private final Map<UUID, Ghost> ghosts = new LinkedHashMap<>();
    /** Ghosts a teammate's Revive Stone is already bringing back (see {@link ReviveStones}). */
    private final Set<UUID> reviving = new HashSet<>();
    private int ticks;

    Ghosts(DungeonRun run, Plugin plugin) {
        this.run = run;
        this.plugin = plugin;
        int seconds = DeathRules.autoReviveSeconds(run.floor);
        this.autoReviveTicks = seconds < 0 ? -1 : seconds * 20;
    }

    boolean isGhost(UUID id) {
        return ghosts.containsKey(id);
    }

    /** Ghosts, first to die first. */
    List<UUID> all() {
        return List.copyOf(ghosts.keySet());
    }

    /** Whether a Revive Stone is already on its way to this ghost. */
    boolean beingRevived(UUID id) {
        return reviving.contains(id);
    }

    /** A Revive Stone was used on this ghost; false if it isn't one, or one already was. */
    boolean startRevive(UUID id) {
        return isGhost(id) && reviving.add(id);
    }

    /** That Revive Stone's 5 seconds are up. */
    void endRevive(UUID id) {
        reviving.remove(id);
    }

    /** Seconds until this ghost comes back by itself; -1 if it won't. */
    int secondsLeft(UUID id) {
        Ghost ghost = ghosts.get(id);
        if (ghost == null || autoReviveTicks < 0) return -1;
        return Math.max(0, (ghost.since + autoReviveTicks - ticks + 19) / 20);
    }

    /**
     * A member's health ran out: the death lines, and they're a ghost, unless a Revive Stone they carry
     * brings them straight back (it's used up; they stay where they are). Either way it's a death.
     */
    void died(Player player, DeathText.Reason reason, String killer) {
        DungeonRun.Member member = run.member(player.getUniqueId());
        if (member == null) {
            player.setInvulnerable(false);
            return;
        }
        if (isGhost(player.getUniqueId())) {
            // Something got through to a ghost (the void, /kill): it stays one.
            PlayerHealth.refill(player);
            return;
        }
        run.died(player.getUniqueId());
        announceDeath(member, reason, killer);
        if (ReviveStones.take(player)) {
            // Whether this says anything else, and whether it's a death for the score: UNKNOWN.
            back(player, player.getLocation());
            run.tell(DeathText.revived(member.rankColor + member.name));
            return;
        }
        haunt(player);
    }

    /** "... disconnected from the Dungeon and became a ghost.", to the others; they're a ghost when they're back. */
    void disconnected(Player player) {
        DungeonRun.Member member = run.member(player.getUniqueId());
        if (member == null || isGhost(player.getUniqueId())) return;
        run.died(player.getUniqueId());
        String line = DeathText.death(DeathText.Reason.DISCONNECTED, null, false, member.rankColor + member.name);
        for (Player other : run.players()) if (!other.equals(player)) other.sendMessage(Utils.color(line));
        Ghost ghost = new Ghost();
        ghost.since = ticks;
        ghost.mode = modeToRestore(player);
        // Made one when they're back (see arrived).
        ghosts.put(player.getUniqueId(), ghost);
    }

    private void announceDeath(DungeonRun.Member member, DeathText.Reason reason, String killer) {
        String name = member.rankColor + member.name;
        for (Player other : run.players()) {
            boolean self = other.getUniqueId().equals(member.id);
            other.sendMessage(Utils.color(DeathText.death(reason, killer, self, name)));
        }
    }

    /** Makes them a ghost where they are. */
    private void haunt(Player player) {
        Ghost ghost = new Ghost();
        ghost.since = ticks;
        ghost.mode = modeToRestore(player);
        ghosts.put(player.getUniqueId(), ghost);
        showAsGhost(player);
    }

    /** The game mode they go back to: theirs now, but survival for adventure (a ghost's). */
    private static GameMode modeToRestore(Player player) {
        return player.getGameMode() == GameMode.ADVENTURE ? GameMode.SURVIVAL : player.getGameMode();
    }

    /**
     * A member arriving (or back after leaving): a ghost's state again, with "reconnected" to the others,
     * or everything a ghost had undone (a run world keeps what was set on them).
     */
    void arrived(Player player) {
        Ghost ghost = ghosts.get(player.getUniqueId());
        hideGhostsFrom(player);
        if (ghost == null) {
            restore(player, null);
            return;
        }
        // Their timer starts again from here (UNKNOWN on Hypixel).
        ghost.since = ticks;
        showAsGhost(player);
        DungeonRun.Member member = run.member(player.getUniqueId());
        if (member != null) {
            String line = DeathText.reconnected(member.rankColor + member.name);
            for (Player other : run.players()) if (!other.equals(player)) other.sendMessage(Utils.color(line));
        }
    }

    private void showAsGhost(Player player) {
        // A ghost's stats have no class in them.
        PlayerSession.of(player).invalidateStats();
        PlayerHealth.refill(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.setFireTicks(0);
        player.setFallDistance(0);
        Ghost ghost = ghosts.get(player.getUniqueId());
        if (ghost != null && (ghost.head == null || !ghost.head.isValid())) ghost.head = head(player);
        hideFromOthers(player);
        PlayerHealth.sync(player);
    }

    /**
     * The Ghost Head the others see. Its skin is UNKNOWN: the head on Class Details' Ghost Abilities item
     * (the same for every class) stands in for it.
     */
    private ArmorStand head(Player player) {
        Location at = headSpot(player);
        ArmorStand stand = at.getWorld().spawn(at, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setMarker(true);
            s.setPersistent(false);
            s.setSmall(true);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            Utils.skull(skull, Utils.texture(ClassDetails.GHOST_TEXTURE));
            s.getEquipment().setHelmet(skull);
        });
        // Not in their own face.
        player.hideEntity(plugin, stand);
        return stand;
    }

    private static Location headSpot(Player player) {
        Location at = player.getLocation().clone().add(0, HEAD_ABOVE, 0);
        at.setPitch(0);
        return at;
    }

    private void hideFromOthers(Player ghost) {
        for (Player other : ghost.getWorld().getPlayers()) {
            if (!other.equals(ghost) && other.canSee(ghost)) other.hidePlayer(plugin, ghost);
        }
    }

    /** Every tick: heads follow, ghosts stay hidden and flying, and the timer brings them back. */
    void tick() {
        ticks++;
        for (Map.Entry<UUID, Ghost> entry : List.copyOf(ghosts.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            Ghost ghost = entry.getValue();
            if (player == null || !player.getWorld().equals(run.world)) {
                if (ghost.head != null) ghost.head.remove();
                ghost.head = null;
                continue;
            }
            if (ghost.head != null && ghost.head.isValid()) ghost.head.teleport(headSpot(player));
            if (ticks % 20 == 0) {
                hideFromOthers(player);
                if (!player.getAllowFlight()) player.setAllowFlight(true);
            }
            if (autoReviveTicks >= 0 && ticks - ghost.since >= autoReviveTicks) revive(player, null);
        }
    }

    /**
     * Brings a ghost back: at {@code at} (on the ground under it), or where the ghost is. Full health and
     * mana ("Reviving will now replenish your mana fully", MCW Dungeons 0.9.4).
     */
    boolean revive(Player player, Location at) {
        if (!ghosts.containsKey(player.getUniqueId())) return false;
        back(player, at == null ? player.getLocation() : at);
        DungeonRun.Member member = run.member(player.getUniqueId());
        if (member != null) run.tell(DeathText.revived(member.rankColor + member.name));
        return true;
    }

    /** Alive again at {@code at}, or on the ground under it. */
    private void back(Player player, Location at) {
        Ghost ghost = ghosts.remove(player.getUniqueId());
        restore(player, ghost != null && ghost.mode != null ? ghost.mode : null);
        if (ghost != null && ghost.head != null) ghost.head.remove();
        player.teleport(ground(at));
        player.setFallDistance(0);
        // Their class counts again: stats already worked out this tick were a ghost's.
        PlayerSession session = PlayerSession.of(player);
        session.invalidateStats();
        PlayerHealth.refill(player);
        PlayerHealth.sync(player);
        session.setMana(session.maxMana());
    }

    /** Undoes a ghost's state (the game mode, if one's given). */
    private void restore(Player player, GameMode mode) {
        if (mode != null) player.setGameMode(mode);
        else if (player.getGameMode() == GameMode.ADVENTURE) player.setGameMode(GameMode.SURVIVAL);
        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        player.setInvulnerable(false);
        for (Player other : player.getWorld().getPlayers()) if (!other.canSee(player)) other.showPlayer(plugin, player);
    }

    /** The first spot at or below {@code at} with ground under it (a ghost may be in mid-air). */
    static Location ground(Location at) {
        World world = at.getWorld();
        int x = at.getBlockX();
        int z = at.getBlockZ();
        for (int y = at.getBlockY(); y > world.getMinHeight(); y--) {
            if (world.getBlockAt(x, y - 1, z).getType().isSolid() && !world.getBlockAt(x, y, z).getType().isSolid()
                    && !world.getBlockAt(x, y + 1, z).getType().isSolid()) {
                Location spot = at.clone();
                spot.setY(y);
                return spot;
            }
        }
        return at;
    }

    /**
     * A member leaving the server: a ghost's state comes off them (their player file keeps what's set on
     * them, and another run may be next), and its head goes; they're still a ghost here if they're back.
     */
    void leaving(Player player) {
        Ghost ghost = ghosts.get(player.getUniqueId());
        if (ghost == null) return;
        if (ghost.head != null) ghost.head.remove();
        ghost.head = null;
        restore(player, ghost.mode);
    }

    /** A ghost a new arrival shouldn't see. */
    void hideGhostsFrom(Player viewer) {
        for (UUID id : ghosts.keySet()) {
            Player ghost = Bukkit.getPlayer(id);
            if (ghost != null && !ghost.equals(viewer) && viewer.canSee(ghost)) viewer.hidePlayer(plugin, ghost);
        }
    }

    /** The run is over: everyone's back to themselves. */
    void dispose() {
        for (Map.Entry<UUID, Ghost> entry : ghosts.entrySet()) {
            if (entry.getValue().head != null) entry.getValue().head.remove();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) restore(player, entry.getValue().mode);
        }
        ghosts.clear();
        reviving.clear();
    }

    /** "&a&lDowned: ..." and the two lines under it, for the tab list's Player Stats. */
    List<String> tabLines() {
        List<String> out = new ArrayList<>();
        if (ghosts.isEmpty()) {
            out.add("&a&lDowned: &7NONE");
            out.add(" Time: &eN/A");
            out.add(" Revive: &cN/A");
            return out;
        }
        // What these show while someone is dead is UNKNOWN: the first ghost, and its time until it's back.
        UUID first = ghosts.keySet().iterator().next();
        DungeonRun.Member member = run.member(first);
        out.add("&a&lDowned: " + (member == null ? "&7?" : member.rankColor + member.name));
        int left = secondsLeft(first);
        out.add(" Time: &e" + (left < 0 ? "N/A" : left + "s"));
        out.add(" Revive: &cN/A");
        return out;
    }
}
