package net.icxd.dungeons.item.requirement.hotm;

import java.util.List;
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

    @Override
    public List<String> lore() {
        return List.of("&4❣ &cRequires &5Heart of the Mountain Tier " + level + "&c.");
    }
}
