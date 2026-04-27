package net.icxd.dungeons.anticheat.check.combat;

import net.icxd.dungeons.anticheat.check.CheckResult;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ReachCheck extends CombatCheck {
    public ReachCheck() {
        super("Reach", true);
    }

    @Override
    public CheckResult check(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return new CheckResult(this, false);
        Entity damaged = event.getEntity();

        double distance = player.getEyeLocation().distance(damaged.getLocation());
        if (distance > 4.0)
            return new CheckResult(this, false, "Distance: " + Utils.round(distance, 2));

        return new CheckResult(this, true);
    }

    @Override
    public CheckResult checkTick(Player player) {
        return null;
    }
}
