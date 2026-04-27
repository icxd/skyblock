package net.icxd.dungeons.item.requirement.hotm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.item.requirement.Requirement;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public class HeartOfTheMountainRequirement extends Requirement {
    private final int level;

    @Override
    public Predicate<Player> requirement() {
        return null;
    }
}
