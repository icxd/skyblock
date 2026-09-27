package net.icxd.dungeons.item.requirement.skill;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public class SkillRequirement extends Requirement {
    private final Skill skill;
    private final int level;

    /** Their level in the skill on the profile they play on (none while their data isn't loaded). */
    @Override
    public Predicate<Player> requirement() {
        return player -> Skills.level(player, skill) >= level;
    }

    @Override
    public List<String> lore() {
        return List.of("&4❣ &cRequires &a" + skill.getName() + " Skill " + level + "&c.");
    }
}
