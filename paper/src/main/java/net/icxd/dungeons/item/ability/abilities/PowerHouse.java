package net.icxd.dungeons.item.ability.abilities;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityType;
import org.bukkit.entity.Player;

public class PowerHouse extends Ability {
    public PowerHouse() {
        super("Power House", AbilityType.PIECE_BONUS, AbilityActivation.PASSIVE, "&7", 0, 0, 0);
    }

    @Override
    public void activate(Player player, SkyBlockItem item) {

    }
}
