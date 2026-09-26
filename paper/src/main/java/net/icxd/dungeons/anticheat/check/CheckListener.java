package net.icxd.dungeons.anticheat.check;

import net.icxd.dungeons.anticheat.ACUser;
import net.icxd.dungeons.anticheat.check.combat.CombatCheck;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CheckListener implements Listener {

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        ACUser user = ACUser.getUser((Player) event.getDamager());
        if (user == null) return;
        CheckHandler.checks.forEach(check -> {
            if (check instanceof CombatCheck combatCheck) {
                if (!combatCheck.isEnabled()) return;
                if (combatCheck.check(event) != null && !combatCheck.check(event).isPassed())
                    user.addViolation(combatCheck.check(event));
            }
        });
    }

}
