package net.icxd.dungeons.item.requirement.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public class SkillRequirement extends Requirement {
    private final Skill skill;
    private final int level;

    @Override
    public Predicate<Player> requirement() {
        return (player) -> {
            User user = User.ifLoaded(player.getUniqueId());
            Integer xp = user == null ? null : user.get("skills." + skill.name().toLowerCase(), Integer.class);
            return xp != null && Skill.getLevelFromXP(xp) >= level;
        };
    }
}
