package net.icxd.dungeons.anticheat.check;

import net.icxd.dungeons.anticheat.ACUser;
import net.icxd.dungeons.anticheat.check.combat.CombatCheck;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CheckListener implements Listener {

    /** Hits that went through (cancelled ones, like sweeps, aren't checked). */
    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ACUser user = ACUser.getUser(player);
        for (Check check : CheckHandler.checks) {
            if (!(check instanceof CombatCheck combatCheck) || !combatCheck.isEnabled()) continue;
            CheckResult result = combatCheck.check(event);
            if (result != null && !result.isPassed()) user.addViolation(result);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        ACUser.forget(event.getPlayer().getUniqueId());
    }
}
