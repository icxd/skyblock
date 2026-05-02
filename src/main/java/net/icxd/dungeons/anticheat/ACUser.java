package net.icxd.dungeons.anticheat;

import lombok.Getter;
import net.icxd.dungeons.anticheat.check.Check;
import net.icxd.dungeons.anticheat.check.CheckResult;
import net.icxd.dungeons.user.Rank;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class ACUser {
    public static final HashMap<UUID, ACUser> users = new HashMap<>();

    private final Player player;
    private final HashMap<ACUser, HashMap<Check, Integer>> violations = new HashMap<>();

    public ACUser(Player player) {
        this.player = player;
        this.violations.put(this, new HashMap<>());
    }

    public void addViolation(CheckResult result) {
        Check check = result.getCheck();
        violations.get(this).put(check, violations.get(this).getOrDefault(check, 0) + 1);
        User user = User.getUser(player.getUniqueId());
        Rank rank = Rank.valueOf(user.getDocument().getString("rank"));
        String msg = Utils.color("&8[&dAC&8] &d" + rank.getPrefix() + player.getName() + " &7failed &d" + check.getName() + " &7check (" + result.getMessage() + ") &8(" + violations.get(this).get(check) + ")");
        Utils.getAllUsersOfRankOrHigher(Rank.STAFF).forEach(u -> u.getPlayer().sendMessage(msg));
    }

    public static ACUser getUser(Player player) {
        if (users.containsKey(player.getUniqueId())) return users.get(player.getUniqueId());
        return new ACUser(player);
    }

    public int getViolations(Check check) {
        return violations.get(this).getOrDefault(check, 0);
    }
}
