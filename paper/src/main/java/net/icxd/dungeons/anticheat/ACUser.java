package net.icxd.dungeons.anticheat;

import lombok.Getter;
import net.icxd.dungeons.anticheat.check.Check;
import net.icxd.dungeons.anticheat.check.CheckResult;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A player's anticheat record: how often they've failed each check (kept while they're online). */
@Getter
public class ACUser {
    private static final Map<UUID, ACUser> users = new HashMap<>();

    private final Player player;
    private final Map<Check, Integer> violations = new HashMap<>();

    private ACUser(Player player) {
        this.player = player;
    }

    /** Tells staff, with how many times they've failed that check. */
    public void addViolation(CheckResult result) {
        Check check = result.getCheck();
        int count = violations.merge(check, 1, Integer::sum);
        Rank rank = User.rankOf(player.getUniqueId());
        String msg = Utils.color("&8[&dAC&8] &d" + rank.getPrefix() + player.getName() + " &7failed &d" + check.getName() + " &7check (" + result.getMessage() + ") &8(" + count + ")");
        Utils.getAllUsersOfRankOrHigher(Rank.STAFF).forEach(p -> p.sendMessage(msg));
    }

    public static ACUser getUser(Player player) {
        return users.computeIfAbsent(player.getUniqueId(), id -> new ACUser(player));
    }

    public static void forget(UUID player) {
        users.remove(player);
    }

    public int getViolations(Check check) {
        return violations.getOrDefault(check, 0);
    }
}
