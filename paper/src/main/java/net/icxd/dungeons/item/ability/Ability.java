package net.icxd.dungeons.item.ability;

import lombok.Getter;
import net.icxd.dungeons.utils.Text;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.icxd.dungeons.item.SkyBlockItem;
import org.bukkit.entity.Player;

import java.util.List;

@Getter
@RequiredArgsConstructor
public abstract class Ability {
    private final String name;
    private final AbilityType type;
    private final AbilityActivation activation;
    /** Hypixel's text: "\n" where Hypixel breaks the line, and anything longer is wrapped like Hypixel's. */
    private final String description;
    private final int cooldown;
    private final int manaCost;
    private final int soulflowCost;

    @Setter
    private boolean showManaCost = true;

    public abstract void activate(Player player, SkyBlockItem item);

    /** The description as lore lines. */
    public List<String> descriptionLines() {
        return description == null || description.isEmpty() ? List.of() : Text.wrap(description, Text.LORE_WIDTH);
    }
}
