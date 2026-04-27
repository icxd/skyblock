package net.icxd.dungeons.anticheat.check.combat;

import net.icxd.dungeons.anticheat.check.Check;
import net.icxd.dungeons.anticheat.check.CheckResult;
import net.icxd.dungeons.anticheat.check.CheckType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public abstract class CombatCheck extends Check {
    public CombatCheck(String name, boolean enabled) {
        super(name, CheckType.COMBAT, enabled);
    }

    public abstract CheckResult check(EntityDamageByEntityEvent event);
    public abstract CheckResult checkTick(Player player);
}
