package net.icxd.dungeons.anticheat.check;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.anticheat.ACUser;
import net.icxd.dungeons.anticheat.check.combat.CombatCheck;
import org.bukkit.Bukkit;
import org.reflections.Reflections;

import java.util.ArrayList;

public class CheckHandler implements Runnable {
    public static final ArrayList<Check> checks = new ArrayList<>();

    public CheckHandler() {
        Bukkit.getScheduler().runTaskTimer(Dungeons.getInstance(), this, 0, 1);
        new Reflections().getSubTypesOf(CombatCheck.class).forEach(check -> {
            try {
                checks.add(check.newInstance());
            } catch (InstantiationException | IllegalAccessException e) { e.printStackTrace(); }
        });
    }

    public static void handleCheck(ACUser user, Check check) {
        if (check instanceof CombatCheck combatCheck) {
            if (!combatCheck.isEnabled()) return;
            if (combatCheck.checkTick(user.getPlayer()) != null && !combatCheck.checkTick(user.getPlayer()).isPassed())
                user.addViolation(combatCheck.checkTick(user.getPlayer()));
        }
    }

    @Override
    public void run() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            ACUser user = ACUser.getUser(player);
            if (user == null) return;
            checks.forEach(check -> handleCheck(user, check));
        });
    }
}
