package net.icxd.dungeons.item.requirement;

import lombok.Getter;
import net.icxd.dungeons.crimsonisle.kuudra.KuudraTier;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Predicate;

/** Having completed a Kuudra tier (the highest one completed is stored). */
@Getter
public class KuudraTierRequirement extends Requirement {
    private final KuudraTier tier;

    public KuudraTierRequirement(KuudraTier tier) {
        this.tier = tier;
    }

    @Override
    public Predicate<Player> requirement() {
        return player -> {
            User user = User.ifLoaded(player.getUniqueId());
            Integer highest = user == null ? null : user.get("crimsonIsle.kuudra.highest", Integer.class);
            return highest != null && highest >= tier.getTier();
        };
    }

    @Override
    public List<String> lore() {
        return List.of("&4❣ &cRequires Kuudra " + Utils.title(tier.name()) + " Tier Completion.");
    }
}
