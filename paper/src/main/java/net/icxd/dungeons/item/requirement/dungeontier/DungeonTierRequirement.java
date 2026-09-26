package net.icxd.dungeons.item.requirement.dungeontier;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.user.User;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public class DungeonTierRequirement extends Requirement {
    private final DungeonType dungeonType;
    private final int tier;

    @Override
    public Predicate<Player> requirement() {
        return (player) -> Skill.getLevelFromXP(User.getUser(player.getUniqueId()).getDocument().get("dungeons", Document.class).get("floors", Document.class).getInteger("highest")) >= tier;
    }
}
