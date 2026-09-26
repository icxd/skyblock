package net.icxd.dungeons.item.ability.abilities.scrolls;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityType;
import org.bukkit.entity.Player;

public class WitherShield extends Ability {
    public WitherShield() {
        super("Wither Shield", AbilityType.ABILITY, AbilityActivation.RIGHT_CLICK,
                "&7Reduces damage taken by &c10% &7for &e5\n&7seconds. Also grants an absorption\n&7shield that scales based on your\n&7Catacombs Level.", 10, 150, 0);
    }

    @Override
    public void activate(Player player, SkyBlockItem item) {

    }
}
