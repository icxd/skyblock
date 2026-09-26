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

        // To the nearest point of the target's hitbox, as the game measures reach (not to its feet).
        var eye = player.getEyeLocation().toVector();
        var box = damaged.getBoundingBox();
        double dx = Math.max(box.getMinX() - eye.getX(), Math.max(0, eye.getX() - box.getMaxX()));
        double dy = Math.max(box.getMinY() - eye.getY(), Math.max(0, eye.getY() - box.getMaxY()));
        double dz = Math.max(box.getMinZ() - eye.getZ(), Math.max(0, eye.getZ() - box.getMaxZ()));
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > 4.0)
            return new CheckResult(this, false, "Distance: " + Utils.round(distance, 2));

        return new CheckResult(this, true);
    }

    @Override
    public CheckResult checkTick(Player player) {
        return null;
    }
}
