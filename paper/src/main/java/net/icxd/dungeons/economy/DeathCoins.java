package net.icxd.dungeons.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import net.icxd.dungeons.combat.LastHit;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Coins lost on death: "The player usually loses half of the Coins in their Purse" (the wiki's Death), exactly
 * half, "splitting coins into decimals rather than rounding" (the wiki's Coins), and the chat line says how much
 * was lost, rounded down ("If the player dies with 5 in the purse, it will say that the player lost 2"). Not in the
 * Catacombs, where a death makes a ghost instead; not on a Sandbox profile (the owner's rule); and not for a fall
 * into the void with no mob behind it ("a player who walks off their Private Island will not lose any coins ...
 * but a player knocked off the island by a Zombie will lose coins", the wiki's Coins).
 *
 * <p>What saves coins (the Bank enchantment, a Piggy Bank) adds a share through {@link #addSaver}: shares add up,
 * as "Wearing two pieces with Bank V saves all of the player's coins" (the wiki's Death), to at most all of them.
 */
public final class DeathCoins implements Listener {
    /** How long a mob's hit keeps a void death one that costs coins. UNKNOWN: the wiki's "less than ten seconds or so". */
    static final long KNOCKED_OFF_MILLIS = 10_000;

    /** The share of the loss something saves them (0 to 1), asked at each death that costs coins. */
    public interface Saver {
        double saved(Player player, double losing);
    }

    private static final List<Saver> SAVERS = new ArrayList<>();

    public static void addSaver(Saver saver) {
        SAVERS.add(saver);
    }

    /** A saver that doesn't need to know the amount. */
    public static void addSaver(ToDoubleFunction<Player> saver) {
        SAVERS.add((player, losing) -> saver.applyAsDouble(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (InventorySyncListener.frozen(player) || RunManager.inRun(player)) return;
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || user.mode() == ProfileMode.SANDBOX) return;
        EntityDamageEvent cause = player.getLastDamageCause();
        boolean intoTheVoid = cause != null && cause.getCause() == EntityDamageEvent.DamageCause.VOID;
        if (intoTheVoid && LastHit.within(player, KNOCKED_OFF_MILLIS) == null) return;

        double coins = Purse.coins(user);
        double losing = coins / 2;
        double saved = 0;
        for (Saver saver : SAVERS) saved += saver.saved(player, losing);
        double lost = lost(coins, saved);
        if (lost <= 0) return;
        Purse.take(user, lost);
        // UNKNOWN: the plain line; the recorded one with a Piggy Bank is "§cYou died, lost 50,000 coins and your
        // piggy bank broke!" (SkyHanni's GFSPiggyBank), so this is that without the piggy bank.
        player.sendMessage(Text.line("&cYou died and lost " + wholeCoins(lost) + " coins!"));
    }

    /** What a death takes from a purse of {@code coins} when {@code saved} (0 to 1, or more) of the half is saved. */
    static double lost(double coins, double saved) {
        if (coins <= 0) return 0;
        return coins / 2 * (1 - Math.max(0, Math.min(1, saved)));
    }

    /** The loss as the chat line says it: rounded down, with commas ("2" for a loss of 2.5). */
    static String wholeCoins(double lost) {
        return Coins.format(Math.floor(lost));
    }
}
