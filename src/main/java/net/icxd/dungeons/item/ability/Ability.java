package net.icxd.dungeons.item.ability;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.icxd.dungeons.item.SkyBlockItem;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor
public abstract class Ability {
    private final String name;
    private final AbilityType type;
    private final AbilityActivation activation;
    private final String description;
    private final int cooldown;
    private final int manaCost;
    private final int soulflowCost;

    @Setter
    private boolean showManaCost = true;

    public abstract void activate(Player player, SkyBlockItem item);
}
