package net.icxd.dungeons.economy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.HitKind;
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
    static final String EMERALD_BLADE = "EMERALD_BLADE";

    /** The share of the loss something saves them (0 to 1), asked at each death that costs coins. */
    public interface Saver {
        double saved(Player player, double losing);

        /**
         * Told once the coins are taken, when it saved a share: what it does to itself then (a Piggy Bank cracks), and
         * the chat line to say instead of the usual one (null for the usual).
         */
        default String saving(PlayerDeathEvent event, double lost) {
            return null;
        }
    }

    private static final List<Saver> SAVERS = new ArrayList<>();
    /**
     * Who has the Emerald Blade's Curse of Greed: "Receive Curse of Greed when striking, CANCELLING any effect
     * modifying your coins loss on death!" (its text). UNKNOWN: how long it lasts; here until they die or leave (the
     * wiki's Milk Buckets that remove it aren't here).
     */
    private static final Set<UUID> CURSED = new HashSet<>();

    static {
        SAVERS.add(new PiggyBanks());
    }

    public DeathCoins() {
        Combat.addHitListener((player, landing, target, damage, killed) -> {
            if (landing.kind() == HitKind.MELEE && landing.weapon() != null && EMERALD_BLADE.equalsIgnoreCase(landing.weapon().getString("id"))) {
                CURSED.add(player.getUniqueId());
            }
        });
    }

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

        boolean cursed = CURSED.remove(player.getUniqueId());
        double coins = Purse.coins(user);
        double losing = coins / 2;
        double saved = 0;
        List<Saver> saving = new ArrayList<>();
        for (Saver saver : cursed ? List.<Saver>of() : SAVERS) {
            double share = saver.saved(player, losing);
            if (share <= 0) continue;
            saved += share;
            saving.add(saver);
        }
        double lost = lost(coins, saved);
        if (lost > 0) Purse.take(user, lost);
        String line = null;
        for (Saver saver : saving) {
            String own = saver.saving(event, lost);
            if (line == null) line = own;
        }
        // UNKNOWN: the plain line; the recorded one with a Piggy Bank is "§cYou died, lost 50,000 coins and your
        // piggy bank broke!" (SkyHanni's GFSPiggyBank), so this is that without the piggy bank.
        if (line == null && lost > 0) line = "&cYou died and lost " + wholeCoins(lost) + " coins!";
        if (line != null) player.sendMessage(Text.line(line));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        CURSED.remove(event.getPlayer().getUniqueId());
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
