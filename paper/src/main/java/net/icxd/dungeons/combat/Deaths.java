package net.icxd.dungeons.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.instance.DeathText;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.economy.DeathCoins;
import net.icxd.dungeons.hub.DungeonHubMap;
import net.icxd.dungeons.hub.Hub;
import net.icxd.dungeons.island.Islands;
import net.icxd.dungeons.listeners.HealthListener;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Dying outside a dungeon run, as on Hypixel: no death screen and no respawn. The death is called off, as a run's is
 * (see GhostEvents), and on the next tick they're at the island's spawn ("Upon dying, the Player is returned to the
 * island's spawn", the wiki's Death), with their items ("the player does not lose items on death"), full health and
 * half their mana (HealthListener). Usually half their purse goes too (DeathCoins). Chat says why, to them and to the
 * others in the world (Hypixel's lobby, whose deaths SkyHanni can hide when far off): see DeathText#outsideDeath.
 * A run's members die as the run has it. Main thread.
 */
public final class Deaths implements Listener {
    /** A SkyBlock hit this recent took the last of their health; one this recent helped a fall (as in a run). */
    private static final long HIT_NOW = 500;
    private static final long HIT_LATELY = 5_000;

    private final Plugin plugin;
    /**
     * Who's on their way back, and whether they were invulnerable before: they are until they're there, and another
     * death meanwhile (the void hurts the invulnerable too) is called off without a word.
     */
    private final Map<UUID, Boolean> returning = new HashMap<>();

    public Deaths(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (RunManager.inRun(player)) return;
        event.setCancelled(true);
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        event.setReviveHealth(max == null ? 20 : Math.max(1, max.getValue()));
        event.setShouldPlayDeathSound(false);
        HealthListener.revived(player);
        UUID id = player.getUniqueId();
        if (returning.containsKey(id)) return;
        returning.put(id, player.isInvulnerable());
        player.setInvulnerable(true);

        LastHit.Hit now = LastHit.within(player, HIT_NOW);
        LastHit.Hit lately = LastHit.within(player, HIT_LATELY);
        String type = event.getDamageSource().getDamageType().getKey().getKey();
        DeathText.Reason reason = DeathText.outside(type, now != null, lately != null && lately.by() != null);
        Entity by = now != null ? now.by() : lately != null ? lately.by() : event.getDamageSource().getCausingEntity();
        String killer = DeathText.killer(by);
        player.sendMessage(Text.line(DeathText.outsideDeath(reason, killer, true, null)));
        String seen = DeathText.outsideDeath(reason, killer, false, name(player));
        for (Player other : player.getWorld().getPlayers()) if (!other.equals(player)) other.sendMessage(Text.line(seen));
        DeathCoins.died(event);
        LastHit.forget(id);

        Location spawn = spawn(player.getWorld());
        Bukkit.getScheduler().runTask(plugin, () -> {
            Boolean wasInvulnerable = returning.remove(id);
            if (wasInvulnerable == null || !player.isOnline()) return;
            player.setInvulnerable(wasInvulnerable);
            // As a death would have it: their menu closes (one holding an item gives it back), and nothing on them goes on.
            player.closeInventory();
            player.teleport(spawn);
            player.setVelocity(new Vector());
            player.setFallDistance(0);
            player.setFireTicks(0);
            player.setFreezeTicks(0);
            player.setRemainingAir(player.getMaximumAir());
            HealthListener.revived(player);
        });
    }

    /** Leaving on the way back: not invulnerable when they're next here. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Boolean wasInvulnerable = returning.remove(event.getPlayer().getUniqueId());
        if (wasInvulnerable != null) event.getPlayer().setInvulnerable(wasInvulnerable);
    }

    /** Their name in their rank's colour, as others see it in the line. */
    private static String name(Player player) {
        User user = User.cached(player.getUniqueId());
        return (user != null && user.isLoaded() ? user.getRank().getColor() : "&7") + player.getName();
    }

    /** Where a death in this world brings them back: where players arrive on the Hub, the Dungeon Hub, an island. */
    static Location spawn(World world) {
        if (Islands.isIsland(world)) return Islands.spawn(world);
        if (Hub.is(world)) return Hub.spawn();
        if (DungeonHubMap.here() && world.equals(Dungeons.getSkyBlockServer().getMainWorld())) return DungeonHubMap.spawn(world);
        // A world's own spawn is a block: its middle.
        return world.getSpawnLocation().add(0.5, 0, 0.5);
    }
}
