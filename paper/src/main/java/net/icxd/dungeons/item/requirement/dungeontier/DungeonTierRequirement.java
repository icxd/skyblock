package net.icxd.dungeons.item.requirement.dungeontier;

import net.icxd.dungeons.utils.Utils;
import net.icxd.dungeons.utils.Text;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public class DungeonTierRequirement extends Requirement {
    private final DungeonType dungeonType;
    private final int tier;

    @Override
    public Predicate<Player> requirement() {
        // The highest floor they've completed, of The Catacombs or of Master Mode.
        return (player) -> {
            User user = User.ifLoaded(player.getUniqueId());
            Integer highest = user == null ? null
                    : user.get(dungeonType == DungeonType.MASTER_CATACOMBS ? "dungeons.floors.masterHighest" : "dungeons.floors.highest", Integer.class);
            return highest != null && highest >= tier;
        };
    }

    /** "The Catacombs Floor VII"; Master Mode's is long enough to wrap onto a second line. */
    @Override
    public List<String> lore() {
        String floor = (dungeonType == DungeonType.MASTER_CATACOMBS ? "Master Mode " : "") + "The Catacombs Floor " + Utils.getRomanNumeral(tier);
        return Text.wrap("&4❣ &cRequires " + floor + " Completion.", Text.REQUIREMENT_WIDTH);
    }
}
