package net.icxd.dungeons.item.ability.abilities.scrolls;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityType;
import org.bukkit.entity.Player;

public class Implosion extends Ability {
    public Implosion() {
        super("Implosion", AbilityType.ABILITY, AbilityActivation.RIGHT_CLICK, "&7Deals &c13,961.2 &7damage to nearby enemies.", 10, 300, 0);
    }

    @Override
    public void activate(Player player, SkyBlockItem item) {

    }
}
